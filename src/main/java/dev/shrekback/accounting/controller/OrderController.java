package dev.shrekback.accounting.controller;

import dev.shrekback.accounting.dto.OrderDto;
import dev.shrekback.accounting.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequiredArgsConstructor
@CrossOrigin
public class OrderController {

    final OrderService orderService;


    @PostMapping("/checkOut")
    public OrderDto checkOut(@RequestBody OrderDto orderDto) {
        return orderService.checkOut(orderDto);
    }

    @GetMapping("/ordersByUser/{userId}")
    public List<OrderDto> getOrdersByUser(@PathVariable String userId) {
        return orderService.getOrdersByUser(userId);
    }

    @GetMapping("/order/{orderId}")
    public OrderDto getOrdersById(@PathVariable String orderId) {
        return orderService.getOrdersById(orderId);
    }
}
