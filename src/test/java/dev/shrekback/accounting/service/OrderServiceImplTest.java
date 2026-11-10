package dev.shrekback.accounting.service;

import dev.shrekback.accounting.dao.OrderRepository;
import dev.shrekback.accounting.dao.UserAccountRepository;
import dev.shrekback.accounting.dto.Cart;
import dev.shrekback.accounting.dto.OrderDto;
import dev.shrekback.accounting.model.*;
import dev.shrekback.post.dao.PostRepository;
import dev.shrekback.post.dto.SizeQuantityDto;
import dev.shrekback.post.model.Post;
import dev.shrekback.shipping.service.SippingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.modelmapper.ModelMapper;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class OrderServiceImplTest {
    OrderRepository orders = mock(OrderRepository.class);
    UserAccountRepository users = mock(UserAccountRepository.class);
    PostRepository products = mock(PostRepository.class);
    SippingService shipping = mock(SippingService.class);
    OrderServiceImpl service = new OrderServiceImpl(new ModelMapper(), orders, users, products, shipping);
    Post product;
    UserAccount user;
    OrderDto request;

    @BeforeEach
    void setup() {
        product = new Post("Gift", 12.34, "gifts", "", 100, "", "", "wood");
        product.setId("product-1");
        SizeQuantityDto stock = new SizeQuantityDto(); stock.setSize("One size"); stock.setQuantity(5);
        product.setSizeQuantities(List.of(stock));
        user = new UserAccount("buyer@example.com", "hashed", "Buyer", "One");
        // Deliberately tampered cart price: the current product document must win.
        Post stale = new Post(); stale.setId(product.getId()); stale.setPrice(0.01);
        user.setCart(new Cart("buyer@example.com", Set.of(new Item("cart-1", stale, "One size", 2)), 0.02));
        when(users.findById(user.getLogin())).thenReturn(Optional.of(user));
        when(products.findById(product.getId())).thenReturn(Optional.of(product));
        when(shipping.getShippingCost("Israel", 200)).thenReturn(10.0);
        when(orders.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
        ReflectionTestUtils.setField(service, "shippingExchangeRate", new BigDecimal("0.25"));
        request = new OrderDto();
        request.setShippingAddress(new Address("Buyer One", "Street 1", "City", "", "12345", "Israel", "123456789"));
    }

    @Test
    void checkoutIgnoresClientIdentityStatusPricesAndOrderIdAndKeepsCart() {
        request.setUserId("victim@example.com"); request.setStatus(OrderStatus.PAID); request.setOrderId("overwrite-me");
        request.setTotalAmount(new BigDecimal("0.01"));
        OrderDto result = service.checkOut(user.getLogin(), request);
        assertEquals(user.getLogin(), result.getUserId());
        assertEquals(OrderStatus.PENDING, result.getStatus());
        assertNotEquals("overwrite-me", result.getOrderId());
        assertEquals(new BigDecimal("27.18"), result.getTotalAmount());
        assertEquals(new BigDecimal("2.50"), result.getShippingCost());
        assertEquals(12.34, result.getOrderItems().get(0).getUnitPrice());
        assertEquals(1, user.getCart().getItems().size());
        verify(users, never()).save(any());
    }

    @Test
    void refusesOutOfStockOrUnknownSizeWithoutCreatingOrder() {
        product.getSizeQuantities().get(0).setQuantity(1);
        assertThrows(ResponseStatusException.class, () -> service.checkOut(user.getLogin(), request));
        verify(orders, never()).save(any());
    }

    @Test
    void unconfiguredCurrencyConversionFailsClosed() {
        ReflectionTestUtils.setField(service, "shippingExchangeRate", BigDecimal.ZERO);
        var failure = assertThrows(ResponseStatusException.class, () -> service.checkOut(user.getLogin(), request));
        assertEquals(503, failure.getStatusCode().value());
        verify(orders, never()).save(any());
    }

    @Test
    void privateOrderCannotBeReadByAnotherUser() {
        Order order = new Order(); order.setUserId("owner@example.com");
        when(orders.findById("private")).thenReturn(Optional.of(order));
        var failure = assertThrows(ResponseStatusException.class, () -> service.getOrdersById("private", "stranger", false));
        assertEquals(404, failure.getStatusCode().value());
    }
}
