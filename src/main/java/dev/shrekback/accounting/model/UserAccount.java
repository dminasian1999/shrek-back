package dev.shrekback.accounting.model;

import dev.shrekback.accounting.dto.Cart;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Getter
@Document(collection = "shrek-users")
public class UserAccount {
    @Id
    String login;

    @Setter
    String firstName;
    @Setter
    String lastName;
    @Setter
    String password;

    Set<Role> roles;
    @Setter
    Address address;
    @Setter

    Cart cart;

    @Setter
    PaymentMethod paymentMethod; // ✅ Single method

    Set<String> wishList;
    @Getter
    List<Order> orders;

    public UserAccount() {
        roles = new HashSet<>();
        wishList = new HashSet<>();
        orders = new ArrayList<>();
        cart = new Cart();
        addRole("USER");
    }

    public UserAccount(String login, String password, String firstName, String lastName) {
        this();
        this.login = login;
        this.password = password;
        this.firstName = firstName;
        this.lastName = lastName;
    }

    public boolean addRole(String role) {
        return roles.add(Role.valueOf(role.toUpperCase()));
    }

    public boolean removeRole(String role) {
        return roles.remove(Role.valueOf(role.toUpperCase()));
    }

    public boolean addWishList(String productId) {
        return wishList.add(productId);
    }

    public boolean removeWishList(String productId) {
        return wishList.remove(productId);
    }

    public boolean addCartEntry(Item item) {
        return cart.addCartEntry(item);
    }

    public boolean removeCartEntry(Item item) {
        return cart.removeCartEntry(item);
    }

    public void addOrder(Order order) {
        orders.add(order);
    }

    public boolean removeOrder(Order order) {
        return orders.remove(order);
    }
}
