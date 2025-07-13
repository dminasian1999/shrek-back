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
@Document(collection = "receipts")
public class Receipt {
    @Id
    private String id;
    private String name;
    private String imageUrl;
    private int quantity;
    private double sell;
    private double buy;
    private double income;
    private String seller;
    private String category;
    private LocalDateTime dateCreated;

    public Receipt() {
        this.dateCreated = LocalDateTime.now();
    }

    public Receipt(String name, String imageUrl, int quantity, double sell, double buy, double income, String seller, String category) {
        this();
        this.name = name;
        this.imageUrl = imageUrl;
        this.quantity = quantity;
        this.sell = sell;
        this.buy = buy;
        this.income = income;
        this.seller = seller;
        this.category = category;
    }
}
