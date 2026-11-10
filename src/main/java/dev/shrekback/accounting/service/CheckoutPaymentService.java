package dev.shrekback.accounting.service;

import com.mongodb.client.result.UpdateResult;
import com.paypal.orders.Capture;
import com.paypal.http.exceptions.HttpException;
import com.paypal.orders.PurchaseUnit;
import dev.shrekback.accounting.dao.OrderRepository;
import dev.shrekback.accounting.dto.OrderDto;
import dev.shrekback.accounting.model.Order;
import dev.shrekback.accounting.model.OrderItem;
import dev.shrekback.accounting.model.OrderStatus;
import dev.shrekback.accounting.model.UserAccount;
import dev.shrekback.post.model.Post;
import lombok.RequiredArgsConstructor;
import org.bson.Document;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CheckoutPaymentService {
    private final PayPalService paypal;
    private final OrderRepository orders;
    private final MongoTemplate mongo;
    private final TransactionTemplate transactions;
    private final ModelMapper mapper;

    @Value("${checkout.payments-enabled:false}")
    private boolean paymentsEnabled;

    public String createPayment(String localOrderId, String userId) {
        requireEnabled();
        Order order = ownOrder(orders.findById(requiredId(localOrderId)).orElseThrow(CheckoutPaymentService::notFound), userId);
        if (order.getStatus() != OrderStatus.PENDING || order.getTotalAmount() == null || order.getTotalAmount().signum() <= 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This order cannot be paid.");
        }
        if (order.getPaypalOrderId() != null) return order.getPaypalOrderId();
        if (order.getDateCreated() == null || order.getDateCreated().isBefore(LocalDateTime.now().minusMinutes(30))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This checkout expired. Please review your cart again.");
        }
        try {
            String paymentId = paypal.createOrder(key(order.getOrderId(), "create"), order.getCurrencyCode(),
                    order.getTotalAmount().toPlainString(), order.getOrderId());
            // A deterministic gateway key makes parallel creation/retry resolve to the same payment.
            mongo.updateFirst(Query.query(Criteria.where("_id").is(order.getOrderId())
                            .and("userId").is(userId).and("status").is(OrderStatus.PENDING)),
                    Update.update("paypalOrderId", paymentId), Order.class);
            return paymentId;
        } catch (IOException exception) {
            throw gatewayFailure();
        }
    }

    public OrderDto capturePayment(String paypalOrderId, String userId) {
        requireEnabled();
        Order order = ownOrder(orders.findByPaypalOrderId(requiredId(paypalOrderId))
                .orElseThrow(CheckoutPaymentService::notFound), userId);
        if (order.getStatus() == OrderStatus.PAID) return mapper.map(order, OrderDto.class);
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This order cannot be paid.");
        }
        // A short persisted lease prevents parallel captures while permitting recovery after a crashed worker.
        String lockToken = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();
        Query claim = Query.query(Criteria.where("_id").is(order.getOrderId()).and("status").is(OrderStatus.PENDING)
                .orOperator(Criteria.where("paymentLockUntil").exists(false), Criteria.where("paymentLockUntil").lt(now)));
        if (mongo.updateFirst(claim, new Update().set("paymentLockUntil", now.plusMinutes(2))
                .set("paymentLockToken", lockToken), Order.class).getModifiedCount() != 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Payment is already being processed. Please wait and retry.");
        }
        try {
            return captureLocked(order, paypalOrderId, userId);
        } finally {
            mongo.updateFirst(Query.query(Criteria.where("_id").is(order.getOrderId()).and("paymentLockToken").is(lockToken)),
                    new Update().unset("paymentLockUntil").unset("paymentLockToken"), Order.class);
        }
    }

    private OrderDto captureLocked(Order order, String paypalOrderId, String userId) {
        final com.paypal.orders.Order captured;
        try {
            com.paypal.orders.Order providerOrder = paypal.getOrder(paypalOrderId);
            if (providerOrder == null || (!"APPROVED".equals(providerOrder.status()) && !"COMPLETED".equals(providerOrder.status()))) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Approve your payment with PayPal before continuing.");
            }
            if (!order.isInventoryReserved() && !"COMPLETED".equals(providerOrder.status())
                    && order.getDateCreated().isBefore(LocalDateTime.now().minusMinutes(30))) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "This checkout expired. Please review your cart again.");
            }
            // Unapproved payments cannot reserve stock. All size decrements and the reservation marker commit together.
            reserveInventory(order);
            captured = "COMPLETED".equals(providerOrder.status()) ? providerOrder
                    : paypal.captureOrder(paypalOrderId, key(order.getOrderId(), "capture"));
        } catch (HttpException exception) {
            if (definiteDecline(exception)) releaseInventory(order);
            throw gatewayFailure();
        } catch (IOException exception) {
            // A timeout may happen after charging. Keep stock reserved and reconcile against GET on the next retry.
            throw gatewayFailure();
        }
        verifyCapture(order, captured);
        transactions.executeWithoutResult(status -> {
            Order current = ownOrder(orders.findById(order.getOrderId()).orElseThrow(CheckoutPaymentService::notFound), userId);
            if (current.getStatus() == OrderStatus.PAID) return;
            current.setStatus(OrderStatus.PAID);
            orders.save(current);
            for (OrderItem item : current.getOrderItems()) {
                // Preserve entries changed or added while the payment window was open.
                Document purchased = new Document("cartItemId", item.getCartItemId()).append("quantity", item.getQuantity())
                        .append("product._id", item.getProductId()).append("selectedSize", item.getSelectedSize());
                mongo.updateFirst(Query.query(Criteria.where("_id").is(userId)),
                        new Update().pull("cart.items", purchased).inc("cartVersion", 1), UserAccount.class);
            }
        });
        return mapper.map(orders.findById(order.getOrderId()).orElseThrow(CheckoutPaymentService::notFound), OrderDto.class);
    }

    private void reserveInventory(Order order) {
        transactions.executeWithoutResult(status -> {
            UpdateResult claim = mongo.updateFirst(Query.query(Criteria.where("_id").is(order.getOrderId())
                            .and("status").is(OrderStatus.PENDING).and("inventoryReserved").ne(true)),
                    Update.update("inventoryReserved", true), Order.class);
            if (claim.getModifiedCount() == 0) {
                Order current = orders.findById(order.getOrderId()).orElseThrow(CheckoutPaymentService::notFound);
                if (current.isInventoryReserved() || current.getStatus() == OrderStatus.PAID) return;
                throw new ResponseStatusException(HttpStatus.CONFLICT, "This payment cannot reserve inventory.");
            }
            for (OrderItem item : order.getOrderItems()) {
                Query available = Query.query(Criteria.where("_id").is(item.getProductId()).and("sizeQuantities")
                        .elemMatch(Criteria.where("size").is(item.getSelectedSize()).and("quantity").gte(item.getQuantity())));
                UpdateResult reserved = mongo.updateFirst(available,
                        new Update().inc("sizeQuantities.$.quantity", -item.getQuantity()), Post.class);
                if (reserved.getModifiedCount() != 1) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT,
                            "A selected size sold out. Your payment was not charged. Please review your cart.");
                }
            }
        });
    }

    private void releaseInventory(Order order) {
        transactions.executeWithoutResult(status -> {
            UpdateResult claimed = mongo.updateFirst(Query.query(Criteria.where("_id").is(order.getOrderId())
                            .and("status").is(OrderStatus.PENDING).and("inventoryReserved").is(true)),
                    Update.update("inventoryReserved", false), Order.class);
            if (claimed.getModifiedCount() != 1) return;
            for (OrderItem item : order.getOrderItems()) {
                mongo.updateFirst(Query.query(Criteria.where("_id").is(item.getProductId())
                                .and("sizeQuantities.size").is(item.getSelectedSize())),
                        new Update().inc("sizeQuantities.$.quantity", item.getQuantity()), Post.class);
            }
        });
    }

    private static boolean definiteDecline(HttpException error) {
        if (error.statusCode() != 422 || error.getMessage() == null) return false;
        return error.getMessage().contains("INSTRUMENT_DECLINED") || error.getMessage().contains("PAYER_CANNOT_PAY")
                || error.getMessage().contains("ORDER_NOT_APPROVED") || error.getMessage().contains("PAYMENT_DENIED");
    }

    static void verifyCapture(Order expected, com.paypal.orders.Order received) {
        if (received == null || !Objects.equals(expected.getPaypalOrderId(), received.id())
                || !"COMPLETED".equals(received.status()) || received.purchaseUnits() == null
                || received.purchaseUnits().size() != 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Payment is not completed. Please retry or contact the store.");
        }
        PurchaseUnit unit = received.purchaseUnits().get(0);
        if (!Objects.equals(expected.getOrderId(), unit.referenceId()) || unit.payments() == null
                || unit.payments().captures() == null || unit.payments().captures().isEmpty()) throw invalidCapture();
        BigDecimal total = BigDecimal.ZERO;
        try {
            for (Capture capture : unit.payments().captures()) {
                if (!"COMPLETED".equals(capture.status()) || capture.amount() == null
                        || !Objects.equals(expected.getCurrencyCode(), capture.amount().currencyCode())) throw invalidCapture();
                total = total.add(new BigDecimal(capture.amount().value()));
            }
        } catch (NumberFormatException | NullPointerException exception) {
            throw invalidCapture();
        }
        if (total.compareTo(expected.getTotalAmount()) != 0) throw invalidCapture();
    }

    private void requireEnabled() {
        if (!paymentsEnabled) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                "Online payments are not configured. Please contact the store.");
    }

    private static String requiredId(String id) {
        if (id == null || id.isBlank() || id.length() > 100) throw OrderServiceImpl.invalid("A valid order ID is required.");
        return id;
    }

    private static Order ownOrder(Order order, String userId) {
        if (!Objects.equals(order.getUserId(), userId)) throw notFound();
        return order;
    }

    private static String key(String id, String operation) {
        return UUID.nameUUIDFromBytes((id + ":" + operation).getBytes(StandardCharsets.UTF_8)).toString();
    }

    private static ResponseStatusException notFound() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found.");
    }

    private static ResponseStatusException invalidCapture() {
        return new ResponseStatusException(HttpStatus.CONFLICT, "Payment could not be verified. Please contact the store.");
    }

    private static ResponseStatusException gatewayFailure() {
        return new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                "The payment provider did not complete this request. Please retry this checkout or contact the store.");
    }
}
