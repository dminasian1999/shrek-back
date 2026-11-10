package dev.shrekback.accounting.controller;

import dev.shrekback.accounting.dto.OrderDto;
import dev.shrekback.accounting.service.CheckoutPaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/paypal")
public class PayPalController {
    private final CheckoutPaymentService checkout;

    @PostMapping("/orders")
    public Map<String, String> createOrder(@RequestBody CreateOrderDto request, Authentication authentication) {
        return Map.of("orderId", checkout.createPayment(request.orderId(), authentication.getName()));
    }

    @PostMapping("/orders/{orderId}/capture")
    public OrderDto capture(@PathVariable String orderId, Authentication authentication) {
        return checkout.capturePayment(orderId, authentication.getName());
    }

    public record CreateOrderDto(String orderId) { }
}
