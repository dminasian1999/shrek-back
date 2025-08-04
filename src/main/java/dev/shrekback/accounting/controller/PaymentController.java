//package dev.shrekback.accounting.controller;
//
//import dev.shrekback.accounting.dto.PayPalCaptureDto;
//import dev.shrekback.accounting.service.PayPalService;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.*;
//
//@RestController
//@RequestMapping("/api/payment")
//@RequiredArgsConstructor
//@Slf4j
////@CrossOrigin(origins = "https://sevan-front.vercel.app")
//@CrossOrigin
//public class PaymentController {
//
//    private final PayPalService payPalService;
//
//    @PostMapping(value = "/capture", consumes = "application/json")
//    public ResponseEntity<String> capturePayment(@RequestBody PayPalCaptureDto dto) {
//        log.info("Attempting to capture PayPal order: {}", dto.getOrderId());
//
//        if (dto.getOrderId() == null || dto.getOrderId().isBlank()) {
//            return ResponseEntity.badRequest().body("Missing orderId");
//        }
//
//        boolean success = payPalService.captureOrder(dto.getOrderId());
//
//        return success
//                ? ResponseEntity.ok("Payment captured successfully")
//                : ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Failed to capture payment");
//    }
//}
