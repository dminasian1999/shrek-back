package dev.shrekback.accounting.dto;

import dev.shrekback.accounting.model.Address;
import dev.shrekback.accounting.model.OrderItem;
import dev.shrekback.accounting.model.OrderStatus;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
public class OrderDto {
    private String orderId;           // PayPal order ID
    private String userId;
    private OrderStatus status;
    private List<OrderItem> orderItems;
    private double totalAmount;
    private Address shippingAddress;
    private String paymentMethod;
}
