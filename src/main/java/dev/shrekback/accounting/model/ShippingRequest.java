package dev.shrekback.accounting.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Getter
public class ShippingRequest {
     String countryCode;
     int weightGrams;
     int serviceCode;
}
