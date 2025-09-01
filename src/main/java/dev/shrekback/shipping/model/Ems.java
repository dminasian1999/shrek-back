package dev.shrekback.shipping.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "shrek-shipping-ems")
public class Ems {
    @Id
    String groupId;
    private double priceUpTo500g;       // 0.5 kg
    private double priceUpTo1000g;      // 1 kg
    private double eachAdditional500g;
}
