package dev.shrekback.post.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OrderItem {
    private String productId;
    private String productName;
    private int quantity; // Changed to int for numerical operations
    private double unitPrice;
}
