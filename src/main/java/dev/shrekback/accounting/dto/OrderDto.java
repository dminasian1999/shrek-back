package dev.shrekback.accounting.dto;

import dev.shrekback.accounting.model.Address;
import dev.shrekback.accounting.model.OrderItem;
import dev.shrekback.accounting.model.OrderStatus;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class OrderDto {
    String orderId;
    String userId;
    private OrderStatus status;
    private List<OrderItem> orderItems;
    private Address shippingAddress;
    private String paymentMethod;
    private LocalDateTime dateCreated;
}

