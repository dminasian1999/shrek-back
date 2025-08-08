package dev.shrekback.accounting.service;

import reactor.core.publisher.Mono;

public interface ShippingService {
    Mono<String> estimateShipping(String countryCode, int weightGrams, int serviceCode);
}
