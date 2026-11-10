package dev.shrekback.accounting.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PaymentMethodDto {
    private String cardname;
    private String cardtype;
    private String cardno;
    @com.fasterxml.jackson.annotation.JsonIgnore
    private String cvv;
    private String exdate;
}
