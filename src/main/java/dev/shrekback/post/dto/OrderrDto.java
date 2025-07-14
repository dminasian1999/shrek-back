package dev.shrekback.post.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
public class OrderrDto {
    private String id;
    private String orderId;           // PayPal order ID
    private String payerName;
    private String payerEmail;
    private String status;
    private String amount;
    private String currency;
    private LocalDateTime dateCreated;


}
