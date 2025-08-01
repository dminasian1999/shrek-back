package dev.shrekback.accounting.model;

import dev.shrekback.post.model.Post;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(of = {"orderItemId"})
public class OrderItem {
    String orderItemId;
    private Post product;
    String quantity;
    double unitPrice;
}
