package dev.shrekback.accounting.controller;

import dev.shrekback.accounting.dto.OrderDto;
import dev.shrekback.accounting.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import org.springframework.security.core.Authentication;


@RestController
@RequiredArgsConstructor
@CrossOrigin
public class OrderController {

    final OrderService orderService;


    @PostMapping("/checkOut")
    public OrderDto checkOut(@RequestBody OrderDto orderDto, Authentication authentication) {
        return orderService.checkOut(authentication.getName(), orderDto);
    }

    @GetMapping("/orders-all")
    public List<OrderDto> getAllOrders() {
        return orderService.getAllOrders();
    }

    @GetMapping("/ordersByUser/{userId}")
    public List<OrderDto> getOrdersByUser(@PathVariable String userId) {
        return orderService.getOrdersByUser(userId);
    }

    @GetMapping("/order/{orderId}")
    public OrderDto getOrdersById(@PathVariable String orderId, Authentication authentication) {
        boolean administrator = authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMINISTRATOR".equals(authority.getAuthority()));
        return orderService.getOrdersById(orderId, authentication.getName(), administrator);
    }
}
