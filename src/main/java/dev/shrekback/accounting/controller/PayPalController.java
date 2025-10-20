package dev.shrekback.accounting.controller;// src/main/java/com/example/paypal/PayPalController.java

import dev.shrekback.accounting.service.PayPalService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import java.util.UUID;
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/paypal")
@CrossOrigin // narrow this in production
public class PayPalController {

     final PayPalService service;


    // Client calls this to create a server-trusted order
    @PostMapping("/orders")
    public ResponseEntity<Map<String, String>> createOrder(@RequestBody CreateOrderDto dto) throws Exception {
        String key = UUID.randomUUID().toString();
        String orderId = service.createOrder(
                key,
                dto.currencyCode() == null ? "USD" : dto.currencyCode(),
                dto.value(),
                dto.referenceId() == null ? "ref-" + System.currentTimeMillis() : dto.referenceId(),
                dto.returnUrl(),
                dto.cancelUrl()
        );
        return ResponseEntity.ok(Map.of("orderId", orderId));
    }

    // Client calls this after onApprove with orderId to perform the capture
    @PostMapping("/orders/{orderId}/capture")
    public ResponseEntity<?> capture(@PathVariable String orderId) throws Exception {
        var order = service.captureOrder(orderId, UUID.randomUUID().toString());
        // TODO: persist order/captures, mark your order paid if status COMPLETE
        return ResponseEntity.ok(order);
    }

    public record CreateOrderDto(String value, String currencyCode, String referenceId,
                                 String returnUrl, String cancelUrl) {}
}
