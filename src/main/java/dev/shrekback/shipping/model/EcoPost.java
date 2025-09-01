package dev.shrekback.shipping.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "shrek-shipping-eco")

public class EcoPost {

    @Id
    String groupId;
    private double priceUpTo100g;       // 0.5 kg
    private double priceUpTo250g;      // 1 kg
    private double priceUpTo500g;      // 1 kg
    private double priceUpTo750g;      // 1 kg
    private double priceUpTo1000g;      // 1 kg
    private double priceUpTo1500g;
    private double priceUpTo2000g;      // 1 kg
}
