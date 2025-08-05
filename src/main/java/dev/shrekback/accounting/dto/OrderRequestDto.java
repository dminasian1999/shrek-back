package dev.shrekback.accounting.dto;

import dev.shrekback.accounting.model.Address;
import dev.shrekback.accounting.model.OrderItem;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class OrderRequestDto {
    private String userId;
    private String paymentMethod;
    private Address shippingAddress;
    private List<OrderItem> orderItems;
    private double totalAmount;
}
