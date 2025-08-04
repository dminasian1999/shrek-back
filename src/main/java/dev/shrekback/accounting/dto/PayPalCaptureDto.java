package dev.shrekback.accounting.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PayPalCaptureDto {
    private String orderId;
}
