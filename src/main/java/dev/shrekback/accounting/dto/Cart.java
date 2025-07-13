package dev.shrekback.accounting.dto;

import dev.shrekback.accounting.model.CartItem;
import dev.shrekback.post.dto.exceptions.PostNotFoundException;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashSet;
import java.util.Set;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Cart {
    String userId;
    Set<CartItem> items = new HashSet<>();
    Double totalPrice;


    public boolean addCartEntry(CartItem cartItem) {
        boolean res;
        res = items.add(cartItem);
        recalculateTotalPrice();
        return res;

    }

    public boolean removeCartEntry(CartItem cartItem) {
        boolean res;
        res = items.remove(cartItem);
        recalculateTotalPrice();
        return res;

    }

    public void recalculateTotalPrice() {
        this.totalPrice = items.stream()
                .mapToDouble(item -> item.getQuantity() * item.getProduct().getPrice())
                .sum();
    }

    public boolean updateCartList(String cartItemId, boolean isAdd) {
        boolean res;

        if (isAdd) {
            res = items.stream()
                    .filter(item -> item.getCartItemId().equals(cartItemId))
                    .findFirst()
                    .orElseThrow(PostNotFoundException::new).increment();
        } else {
            res = items.stream()
                    .filter(item -> item.getCartItemId().equals(cartItemId))
                    .findFirst()
                    .orElseThrow(PostNotFoundException::new).decrement();
        }
        recalculateTotalPrice();
        return res;
    }

}
