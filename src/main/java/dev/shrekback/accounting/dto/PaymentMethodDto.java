package dev.shrekback.accounting.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PaymentMethodDto {
    private String cardname;
    private String cardtype;
    private String cardno;
    private String cvv;
    private String exdate;
}
