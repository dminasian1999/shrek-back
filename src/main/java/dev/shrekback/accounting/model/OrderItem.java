package dev.shrekback.accounting.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderItem {
    String productId;
    String productName;
    String quantity;
    double unitPrice;
}
