package dev.shrekback.accounting.controller;

import dev.shrekback.accounting.service.ShippingService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/shipping")
@RequiredArgsConstructor
@CrossOrigin
public class ShippingController {

     final ShippingService shippingService;

    @PostMapping()
    public Mono<String> estimateShipping(@RequestBody ShippingRequest request) {
        return shippingService.estimateShipping(
                request.getCountryCode(),
                request.getWeightGrams(),
                request.getServiceCode()
        );
    }
}

@Data
class ShippingRequest {
    private String countryCode;
    private int weightGrams;
    private int serviceCode;
}
