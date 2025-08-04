//package dev.shrekback.post.controller;
//
//import com.stripe.Stripe;
//import com.stripe.model.checkout.Session;
//import com.stripe.param.checkout.SessionCreateParams;
//import dev.shrekback.post.dto.OrderDto;
//import dev.shrekback.post.service.OrderService;
//import lombok.RequiredArgsConstructor;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.*;
//
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//
//@RestController
//@RequestMapping("/orders")
//@RequiredArgsConstructor
//@CrossOrigin
//public class OrderController {
//
//    private final OrderService orderService;
//
//    @Value("${stripe.api.key}")
//    private String stripeApiKey;
//
//    @PostMapping("/save/{login}")
//    public ResponseEntity<String> saveOrder(@PathVariable String login, @RequestBody OrderDto orderDto) {
//        String orderId = orderService.saveOrder(orderDto);
//        return ResponseEntity.ok(orderId);
//    }
//
//    @GetMapping("/receipts")
//    public ResponseEntity<List<OrderDto>> getAllReceipts() {
//        List<OrderDto> receipts = orderService.getAllReceipts();
//        return ResponseEntity.ok(receipts);
//    }
//
//    @PostMapping("/create-checkout-session")
//    public ResponseEntity<Map<String, String>> createCheckoutSession(@RequestBody Map<String, Object> request) {
//        try {
//            Stripe.apiKey = stripeApiKey;
//
//            String YOUR_DOMAIN = "http://localhost:3000";
//            String priceId = (String) request.get("priceId");
//
//            if (priceId == null) {
//                return ResponseEntity.badRequest().body(Map.of("error", "Price ID is required"));
//            }
//
//            SessionCreateParams params = SessionCreateParams.builder()
//                    .setMode(SessionCreateParams.Mode.PAYMENT)
//                    .setSuccessUrl(YOUR_DOMAIN + "/success")
//                    .setCancelUrl(YOUR_DOMAIN + "/cancel")
//                    .addLineItem(
//                            SessionCreateParams.LineItem.builder()
//                                    .setQuantity(1L)
//                                    .setPrice(priceId)
//                                    .build()
//                    )
//                    .build();
//
//            Session session = Session.create(params);
//
//            Map<String, String> responseData = new HashMap<>();
//            responseData.put("url", session.getUrl());
//
//            return ResponseEntity.ok(responseData);
//        } catch (Exception e) {
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
//                    .body(Map.of("error", "Failed to create checkout session: " + e.getMessage()));
//        }
//    }
//}
