package dev.shrekback.accounting.model;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(of = {"productId"})
public class OrderItem {
    String productId;
    int quantity;
    double unitPrice;
}
