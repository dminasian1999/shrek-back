package dev.shrekback.integration;

import com.paypal.orders.Capture;
import com.paypal.orders.Money;
import com.paypal.orders.PaymentCollection;
import com.paypal.orders.PurchaseUnit;
import dev.shrekback.accounting.dao.OrderRepository;
import dev.shrekback.accounting.dao.UserAccountRepository;
import dev.shrekback.accounting.dto.Cart;
import dev.shrekback.accounting.dto.OrderDto;
import dev.shrekback.accounting.model.Address;
import dev.shrekback.accounting.model.Item;
import dev.shrekback.accounting.model.Order;
import dev.shrekback.accounting.model.OrderItem;
import dev.shrekback.accounting.model.OrderStatus;
import dev.shrekback.accounting.model.UserAccount;
import dev.shrekback.accounting.service.CheckoutPaymentService;
import dev.shrekback.accounting.service.OrderService;
import dev.shrekback.accounting.service.PayPalService;
import dev.shrekback.post.dto.SizeQuantityDto;
import dev.shrekback.post.model.Post;
import dev.shrekback.shipping.model.EcoPost;
import dev.shrekback.shipping.model.ShippingCountry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Opt-in tests against a disposable Mongo replica set. Only the payment gateway is
 * mocked; shipping lookups, checkout snapshots, stock reservations, rollback, and
 * cart cleanup use the real application services and database transactions.
 * Set AUDIT_INTEGRATION_MONGODB_URI to the isolated integration database to run.
 * Cleanup deletes only this test's uniquely identified fixtures, never a database.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@EnabledIfEnvironmentVariable(named = "AUDIT_INTEGRATION_MONGODB_URI", matches = ".+")
class CheckoutMongoIntegrationTest {
    @Autowired MongoTemplate mongo;
    @Autowired OrderRepository orders;
    @Autowired UserAccountRepository users;
    @Autowired OrderService checkout;
    @Autowired CheckoutPaymentService payments;
    @MockitoBean PayPalService gateway;

    private final List<String> productIds = new ArrayList<>();
    private String userId;
    private String countryName;
    private String rateGroup;

    @DynamicPropertySource
    static void integrationProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", () -> System.getenv("AUDIT_INTEGRATION_MONGODB_URI"));
        registry.add("checkout.payments-enabled", () -> "true");
        registry.add("checkout.currency", () -> "USD");
        registry.add("checkout.shipping-exchange-rate", () -> "0.25");
        registry.add("paypal.mode", () -> "sandbox");
        registry.add("paypal.client-id", () -> "integration-gateway-is-mocked");
        registry.add("paypal.client-secret", () -> "integration-gateway-is-mocked");
    }

    @BeforeEach
    void createShippingFixtures() {
        String fixture = "audit-" + UUID.randomUUID();
        userId = fixture + "@example.test";
        countryName = fixture + "-destination";
        int group;
        do {
            group = ThreadLocalRandom.current().nextInt(1_000_000, Integer.MAX_VALUE);
            rateGroup = Integer.toString(group);
        } while (mongo.exists(Query.query(Criteria.where("_id").is(rateGroup)), EcoPost.class));
        mongo.save(new ShippingCountry(countryName, 0, group));
        mongo.save(new EcoPost(rateGroup, 12, 12, 12, 12, 12, 12, 12));
    }

    @AfterEach
    void removeOnlyOwnedFixtures() {
        if (userId != null) {
            mongo.remove(Query.query(Criteria.where("userId").is(userId)), Order.class);
            mongo.remove(Query.query(Criteria.where("_id").is(userId)), UserAccount.class);
        }
        if (!productIds.isEmpty()) mongo.remove(Query.query(Criteria.where("_id").in(productIds)), Post.class);
        if (countryName != null) mongo.remove(Query.query(Criteria.where("_id").is(countryName)), ShippingCountry.class);
        if (rateGroup != null) mongo.remove(Query.query(Criteria.where("_id").is(rateGroup)), EcoPost.class);
    }

    @Test
    void snapshotsCanonicalPricesThenCapturesOnceAndPreservesLaterCartAdditions() throws Exception {
        Post product = product(19.95, 5);
        // Cart documents can be stale or tampered with; only the catalog is authoritative.
        Post staleProduct = new Post();
        staleProduct.setId(product.getId());
        staleProduct.setPrice(0.01);
        Item purchased = new Item(UUID.randomUUID().toString(), staleProduct, "One size", 2);
        saveUser(purchased);

        OrderDto request = request();
        request.setOrderId("client-cannot-overwrite-an-order");
        request.setUserId("another-person@example.test");
        request.setStatus(OrderStatus.PAID);
        request.setTotalAmount(new BigDecimal("0.01"));
        request.setShippingCost(new BigDecimal("-100.00"));
        request.setCurrencyCode("ILS");
        OrderDto snapshot = checkout.checkOut(userId, request);

        assertNotEquals(request.getOrderId(), snapshot.getOrderId());
        assertEquals(userId, snapshot.getUserId());
        assertEquals(OrderStatus.PENDING, snapshot.getStatus());
        assertEquals("USD", snapshot.getCurrencyCode());
        assertEquals(new BigDecimal("3.00"), snapshot.getShippingCost());
        assertEquals(new BigDecimal("42.90"), snapshot.getTotalAmount());
        assertEquals(19.95, snapshot.getOrderItems().get(0).getUnitPrice());
        assertEquals(19.95, snapshot.getOrderItems().get(0).getProduct().getPrice());
        assertEquals(5, stock(product.getId()));
        assertEquals(1, users.findById(userId).orElseThrow().getCart().getItems().size());

        String paypalId = "PAY-" + UUID.randomUUID();
        when(gateway.createOrder(anyString(), eq("USD"), eq("42.90"), eq(snapshot.getOrderId())))
                .thenReturn(paypalId);
        assertEquals(paypalId, payments.createPayment(snapshot.getOrderId(), userId));
        assertEquals(paypalId, payments.createPayment(snapshot.getOrderId(), userId));
        when(gateway.getOrder(paypalId)).thenReturn(new com.paypal.orders.Order().id(paypalId).status("APPROVED"));
        when(gateway.captureOrder(eq(paypalId), anyString())).thenAnswer(invocation -> {
            assertEquals(3, stock(product.getId()), "Stock must commit before the payment is charged");
            assertTrue(orders.findById(snapshot.getOrderId()).orElseThrow().isInventoryReserved());
            return completedPayment(paypalId, snapshot);
        });

        Post laterProduct = product(7.50, 8);
        Item laterAddition = new Item(UUID.randomUUID().toString(), laterProduct, "One size", 1);
        UserAccount currentUser = users.findById(userId).orElseThrow();
        currentUser.addCartEntry(laterAddition);
        users.save(currentUser);

        OrderDto captured = payments.capturePayment(paypalId, userId);
        assertEquals(OrderStatus.PAID, captured.getStatus());
        Order persisted = orders.findById(snapshot.getOrderId()).orElseThrow();
        assertEquals(OrderStatus.PAID, persisted.getStatus());
        assertEquals(new BigDecimal("42.90"), persisted.getTotalAmount());
        assertTrue(persisted.isInventoryReserved());
        assertEquals(3, stock(product.getId()));
        assertEquals(8, stock(laterProduct.getId()));
        assertEquals(Set.of(laterAddition.getCartItemId()), cartItemIds());
        assertEquals(7.50, users.findById(userId).orElseThrow().getCart().getTotalPrice(),
                "The persisted cart total must reflect only the unpurchased addition");

        assertEquals(OrderStatus.PAID, payments.capturePayment(paypalId, userId).getStatus());
        assertEquals(3, stock(product.getId()), "A retry must never reserve the same stock twice");
        assertEquals(Set.of(laterAddition.getCartItemId()), cartItemIds());
        assertEquals(7.50, users.findById(userId).orElseThrow().getCart().getTotalPrice(),
                "The persisted cart total must reflect only the unpurchased addition");
        verify(gateway, times(1)).createOrder(anyString(), eq("USD"), eq("42.90"), eq(snapshot.getOrderId()));
        verify(gateway, times(1)).getOrder(paypalId);
        verify(gateway, times(1)).captureOrder(eq(paypalId), anyString());
    }

    @Test
    void soldOutSecondItemRollsBackEveryStockDecrementAndDoesNotCharge() throws Exception {
        Post firstProduct = product(10, 5);
        Post secondProduct = product(15, 5);
        Item firstItem = new Item(UUID.randomUUID().toString(), firstProduct, "One size", 2);
        Item secondItem = new Item(UUID.randomUUID().toString(), secondProduct, "One size", 2);
        saveUser(firstItem, secondItem);
        OrderDto snapshot = checkout.checkOut(userId, request());
        assertEquals(2, snapshot.getOrderItems().size());

        // Use the persisted processing order: the first decrement succeeds and the
        // second fails, exercising rollback rather than merely refusing item one.
        OrderItem available = snapshot.getOrderItems().get(0);
        OrderItem soldOut = snapshot.getOrderItems().get(1);
        mongo.updateFirst(Query.query(Criteria.where("_id").is(soldOut.getProductId())),
                Update.update("sizeQuantities.0.quantity", 0), Post.class);
        String paypalId = "PAY-" + UUID.randomUUID();
        when(gateway.createOrder(anyString(), eq("USD"), eq(snapshot.getTotalAmount().toPlainString()), eq(snapshot.getOrderId())))
                .thenReturn(paypalId);
        assertEquals(paypalId, payments.createPayment(snapshot.getOrderId(), userId));
        when(gateway.getOrder(paypalId)).thenReturn(new com.paypal.orders.Order().id(paypalId).status("APPROVED"));

        ResponseStatusException failure = assertThrows(ResponseStatusException.class,
                () -> payments.capturePayment(paypalId, userId));
        assertEquals(409, failure.getStatusCode().value());
        assertEquals(5, stock(available.getProductId()), "An earlier stock decrement must be rolled back");
        assertEquals(0, stock(soldOut.getProductId()));
        Order persisted = orders.findById(snapshot.getOrderId()).orElseThrow();
        assertEquals(OrderStatus.PENDING, persisted.getStatus());
        assertFalse(persisted.isInventoryReserved(), "The reservation marker must roll back with the stock");
        assertEquals(Set.of(firstItem.getCartItemId(), secondItem.getCartItemId()), cartItemIds());
        verify(gateway, never()).captureOrder(anyString(), anyString());
    }

    private Post product(double price, int quantity) {
        Post product = new Post("Integration ceramic", price, "ceramics", "tiles", 100,
                "Local integration fixture", "blue", "ceramic");
        product.setId("audit-product-" + UUID.randomUUID());
        SizeQuantityDto stock = new SizeQuantityDto();
        stock.setSize("One size");
        stock.setQuantity(quantity);
        product.setSizeQuantities(List.of(stock));
        productIds.add(product.getId());
        return mongo.save(product);
    }

    private void saveUser(Item... items) {
        UserAccount user = new UserAccount(userId, "unused-integration-password", "Integration", "Buyer");
        Cart cart = new Cart(userId, new HashSet<>(List.of(items)), 0.0);
        cart.recalculateTotalPrice();
        user.setCart(cart);
        users.save(user);
    }

    private OrderDto request() {
        OrderDto request = new OrderDto();
        request.setShippingAddress(new Address("Integration Buyer", "Test Street 1", "Test City", "",
                "12345", countryName, "+15555550100"));
        return request;
    }

    private int stock(String productId) {
        return mongo.findById(productId, Post.class).getSizeQuantities().get(0).getQuantity();
    }

    private Set<String> cartItemIds() {
        Set<String> result = new HashSet<>();
        users.findById(userId).orElseThrow().getCart().getItems().forEach(item -> result.add(item.getCartItemId()));
        return result;
    }

    private com.paypal.orders.Order completedPayment(String paypalId, OrderDto order) {
        Capture capture = new Capture().id("CAPTURE-" + UUID.randomUUID()).status("COMPLETED")
                .amount(new Money().currencyCode(order.getCurrencyCode()).value(order.getTotalAmount().toPlainString()));
        return new com.paypal.orders.Order().id(paypalId).status("COMPLETED").purchaseUnits(List.of(
                new PurchaseUnit().referenceId(order.getOrderId()).payments(new PaymentCollection().captures(List.of(capture)))));
    }
}
