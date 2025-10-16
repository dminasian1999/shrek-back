package dev.shrekback.accounting.model;

import dev.shrekback.post.dto.PostDto;
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
    PostDto product;
    int quantity;
    double unitPrice;
}
