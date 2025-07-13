package dev.shrekback.accounting.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@EqualsAndHashCode(of = "orderId")
@ToString
public class Order {

    private String orderId;
    private OrderStatus status;
    private List<OrderItem> orderItems;
    private double totalAmount;
    private Address shippingAddress;
    private String paymentMethod;
    private String paymentStatus;
    private LocalDateTime dateCreated;

    public Order() {
        this.orderItems = new ArrayList<>();
        this.dateCreated = LocalDateTime.now();
    }

    public Order(OrderStatus status, double totalAmount, Address shippingAddress, String paymentMethod, String paymentStatus) {
        this();
        this.status = status;
        this.totalAmount = totalAmount;
        this.shippingAddress = shippingAddress;
        this.paymentMethod = paymentMethod;
        this.paymentStatus = paymentStatus;
    }
// Getters and Setters (or use Lombok @Getter @Setter)

    // Constructors (no-args and all-args if needed)

    // Enums
    public enum OrderStatus {
        PENDING, PAID, SHIPPED, DELIVERED, CANCELLED
    }

    public enum PaymentMethod {
        CARD, PAYPAL
    }

    public enum PaymentStatus {
        PAID, FAILED
    }
}
