package dev.shrekback.shipping.controller;

import dev.shrekback.shipping.service.SippingService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@CrossOrigin

public class SippingController {

    final SippingService sippingService;


    @PostMapping("/parser/{index}/{csv}")
    public boolean addHistoryWithFile(@PathVariable String index, @PathVariable String csv) {
        return sippingService.addCountries(index,csv);
    }
    @PostMapping("/parser-ems/{index}/{csv}")
    public boolean addEms(@PathVariable String index, @PathVariable String csv) {
        return sippingService.addEms(index,csv);
    }
    @PostMapping("/parser-eco/{index}/{csv}")
    public boolean addEco(@PathVariable String index, @PathVariable String csv) {
        return sippingService.addEco(index,csv);
    }
    @GetMapping("/shippingCost/{country}/{weight}")
    public Double getShippingCost(@PathVariable String country, @PathVariable double weight) {
        return sippingService.getShippingCost(country, weight);
    }
}
