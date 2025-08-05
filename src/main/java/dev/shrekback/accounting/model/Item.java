package dev.shrekback.accounting.model;

import dev.shrekback.post.model.Post;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(of = {"cartItemId"})
public class Item {
     String cartItemId;
    private Post product;
    private int quantity;

    public boolean increment() {
        quantity++;
        return true;
    }

    public boolean decrement() {
        if (quantity > 1) {
            quantity--;
            return true;
        }
        return false;
    }
}
