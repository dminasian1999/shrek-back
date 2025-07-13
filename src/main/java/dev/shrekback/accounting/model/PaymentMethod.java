package dev.shrekback.accounting.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentMethod {
    private String type; // e.g., "CREDIT_CARD", "PAYPAL"
    private String provider; // e.g., "Visa", "Mastercard", "PayPal"
    private String accountNumberMasked; // e.g., **** **** **** 1234
    private String expiryDate; // MM/YY
}
