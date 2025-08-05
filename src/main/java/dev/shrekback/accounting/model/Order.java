package dev.shrekback.accounting.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@EqualsAndHashCode(of = "orderId")
@ToString
@Document(collection = "shrek-orders")
public class Order {
    @Id
    private String orderId;
    String userId;
    private OrderStatus status;
    @Singular
    private List<OrderItem> orderItems;
    private Address shippingAddress;
    private String paymentMethod;
    private LocalDateTime dateCreated;

    public Order() {
        this.orderItems = new ArrayList<>();
        this.dateCreated = LocalDateTime.now();
        this.status = OrderStatus.PENDING;
    }

    public Order( double totalAmount, Address shippingAddress, String paymentMethod) {
        this();
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
