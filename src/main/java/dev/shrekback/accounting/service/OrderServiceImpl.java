package dev.shrekback.accounting.service;

import dev.shrekback.accounting.dao.OrderRepository;
import dev.shrekback.accounting.dao.UserAccountRepository;
import dev.shrekback.accounting.dto.OrderDto;
import dev.shrekback.accounting.dto.exceptions.UserNotFoundException;
import dev.shrekback.accounting.model.*;
import dev.shrekback.post.dao.PostRepository;
import dev.shrekback.post.dto.PostDto;
import dev.shrekback.post.model.Post;
import dev.shrekback.shipping.service.SippingService;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.StreamSupport;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {
    private final ModelMapper modelMapper;
    private final OrderRepository orderRepository;
    private final UserAccountRepository userAccountRepository;
    private final PostRepository postRepository;
    private final SippingService shippingService;

    @Value("${checkout.currency:USD}")
    private String currencyCode = "USD";
    // Shipping tables are in ILS. A deployment must explicitly configure its conversion rate.
    @Value("${checkout.shipping-exchange-rate:0}")
    private BigDecimal shippingExchangeRate = BigDecimal.ZERO;

    @Override
    public List<OrderDto> getAllOrders() {
        return StreamSupport.stream(orderRepository.findAll().spliterator(), false)
                .map(order -> modelMapper.map(order, OrderDto.class)).toList();
    }

    @Override
    public OrderDto checkOut(String userId, OrderDto request) {
        if (request == null) throw invalid("Checkout details are required.");
        validateAddress(request.getShippingAddress());
        UserAccount user = userAccountRepository.findById(userId).orElseThrow(UserNotFoundException::new);
        if (user.getCart() == null || user.getCart().getItems() == null || user.getCart().getItems().isEmpty()) {
            throw invalid("Your cart is empty.");
        }
        if (user.getCart().getItems().size() > 100) throw invalid("Your cart contains too many items.");

        List<OrderItem> items = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;
        double weight = 0;
        for (Item item : user.getCart().getItems()) {
            if (item == null || item.getProduct() == null || item.getProduct().getId() == null
                    || item.getQuantity() < 1 || item.getQuantity() > 99) {
                throw invalid("Your cart contains an invalid item. Please update your cart.");
            }
            Post product = postRepository.findById(item.getProduct().getId())
                    .orElseThrow(() -> invalid("A product is no longer available. Please update your cart."));
            validateProduct(product, item.getSelectedSize(), item.getQuantity());
            PostDto snapshot = modelMapper.map(product, PostDto.class);
            items.add(new OrderItem(product.getId(), snapshot, item.getQuantity(), product.getPrice(),
                    item.getSelectedSize(), item.getCartItemId()));
            subtotal = subtotal.add(BigDecimal.valueOf(product.getPrice()).multiply(BigDecimal.valueOf(item.getQuantity())));
            weight += product.getWeight() * item.getQuantity();
        }
        if (!Double.isFinite(weight) || weight <= 0) throw invalid("Shipping weight is unavailable for this cart.");
        double shippingIls = shippingService.getShippingCost(request.getShippingAddress().getCountry(), weight);
        BigDecimal rate = "ILS".equals(currencyCode) ? BigDecimal.ONE : shippingExchangeRate;
        if (rate.signum() <= 0) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Checkout shipping currency is not configured. Please contact the store.");
        }
        BigDecimal shipping = BigDecimal.valueOf(shippingIls).multiply(rate).setScale(2, RoundingMode.HALF_UP);
        Order order = new Order();
        order.setOrderId(UUID.randomUUID().toString());
        order.setUserId(userId);
        order.setOrderItems(items);
        order.setShippingAddress(request.getShippingAddress());
        order.setPaymentMethod("PAYPAL");
        order.setStatus(OrderStatus.PENDING);
        order.setDateCreated(LocalDateTime.now());
        order.setCurrencyCode(currencyCode);
        order.setShippingCost(shipping);
        order.setTotalAmount(subtotal.add(shipping).setScale(2, RoundingMode.HALF_UP));
        return modelMapper.map(orderRepository.save(order), OrderDto.class);
    }

    static void validateProduct(Post product, String size, int quantity) {
        if (!Double.isFinite(product.getPrice()) || product.getPrice() <= 0
                || !Double.isFinite(product.getWeight()) || product.getWeight() <= 0) {
            throw invalid("A product has invalid price or shipping information. Please contact the store.");
        }
        if (product.getSizeQuantities() == null || product.getSizeQuantities().isEmpty()) {
            throw invalid("A product has no available inventory.");
        }
        boolean available = product.getSizeQuantities().stream().anyMatch(stock ->
                stock != null && java.util.Objects.equals(stock.getSize(), size)
                        && stock.getQuantity() != null && stock.getQuantity() >= quantity);
        if (!available) throw new ResponseStatusException(HttpStatus.CONFLICT,
                "A selected size or quantity is no longer available. Please update your cart.");
    }

    private static void validateAddress(Address address) {
        if (address == null || !valid(address.getFullName(), 200) || !valid(address.getStreet(), 300)
                || !valid(address.getCity(), 150) || !valid(address.getCountry(), 100)
                || !valid(address.getZipCode(), 30) || !valid(address.getPhone(), 50)) {
            throw invalid("Enter a complete shipping address, postal code, and phone number.");
        }
    }

    private static boolean valid(String value, int max) {
        return value != null && !value.isBlank() && value.length() <= max;
    }

    static ResponseStatusException invalid(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }

    @Override
    public List<OrderDto> getOrdersByUser(String userId) {
        return orderRepository.findByUserId(userId).stream().map(order -> modelMapper.map(order, OrderDto.class)).toList();
    }

    @Override
    public OrderDto getOrdersById(String orderId, String userId, boolean administrator) {
        Order order = orderRepository.findById(orderId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found."));
        if (!administrator && !userId.equals(order.getUserId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found.");
        }
        return modelMapper.map(order, OrderDto.class);
    }
}
