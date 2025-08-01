package dev.shrekback.accounting.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.data.annotation.Id;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@EqualsAndHashCode(of = "orderId")
@ToString
public class Order {
    @Id
    private String orderId;
    String userId;
    private OrderStatus status;
    private List<OrderItem> orderItems;
    private double totalAmount;
    private Address shippingAddress;
    private String paymentMethod;
    private LocalDateTime dateCreated;

    public Order() {
        this.orderItems = new ArrayList<>();
        this.dateCreated = LocalDateTime.now();
    }

    public Order(OrderStatus status, double totalAmount, Address shippingAddress, String paymentMethod) {
        this();
        this.status = status;
        this.totalAmount = totalAmount;
        this.shippingAddress = shippingAddress;
        this.paymentMethod = paymentMethod;
    }
// Getters and Setters (or use Lombok @Getter @Setter)

    // Constructors (no-args and all-args if needed)

    // Enums

    public enum PaymentMethod {
        CARD, PAYPAL
    }

    public enum PaymentStatus {
        PAID, FAILED
    }
}
