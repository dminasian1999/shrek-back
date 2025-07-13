package dev.shrekback.accounting.dto;

import lombok.Getter;

@Getter

public class PaymentMethodDto {
     String type; // e.g., "CREDIT_CARD", "PAYPAL"
    private String provider; // e.g., "Visa", "Mastercard", "PayPal"
    private String accountNumberMasked; // e.g., **** **** **** 1234
    private String expiryDate; // MM/YY
}
