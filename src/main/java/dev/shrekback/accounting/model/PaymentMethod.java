package dev.shrekback.accounting.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentMethod {
    private String cardname;
    private String cardtype;
    private String cardno;
    private String cvv;
    private String exdate;
}

