package dev.shrekback.post.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Getter
@Setter
@EqualsAndHashCode(of = "id")
@ToString
@Document(collection = "shrek-orders ")
public class Orderr {
    @Id
    private String id;
    private String orderId;           // PayPal order ID
    private String payerName;
    private String payerEmail;
    private String status;
    private String amount;
    private String currency;
    private LocalDateTime dateCreated;

    public Orderr() {
        this.dateCreated = LocalDateTime.now();
    }

    public Orderr(String orderId, String payerName, String payerEmail, String status, String amount, String currency) {
       this();
        this.orderId = orderId;
        this.payerName = payerName;
        this.payerEmail = payerEmail;
        this.status = status;
        this.amount = amount;
        this.currency = currency;
    }
}
