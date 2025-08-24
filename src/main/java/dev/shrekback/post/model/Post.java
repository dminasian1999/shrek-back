package dev.shrekback.post.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Getter
@Setter
@EqualsAndHashCode(of = "id")
@ToString
@Document(collection = "shrek-products")
@AllArgsConstructor

public class Post {
    @Id
    private String id;
    private String name;
    private List<String> imageUrls;
    private int quantity;
    private double price;
    private String category;
    private double weight;
    private String size;
    private String desc;
    private String color;
    private String material;
    private LocalDateTime dateCreated;

    public Post() {
        this.dateCreated = LocalDateTime.now();
        imageUrls = new ArrayList<>();
    }

    public Post(String name, int quantity, double price, String category,double weight, String size, String desc, String color,String material) {
        this();
        this.name = name;
        this.quantity = quantity;
        this.price = price;
        this.category = category;
        this.weight = weight;
        this.size = size;
        this.desc = desc;
        this.color = color;
        this.material = material;
    }

    public void adjust(Adjustment adjustment) {
        if (adjustment.isAdd()) {
            this.quantity += adjustment.getQuantity();
        } else {
            this.quantity -= adjustment.getQuantity();
        }
    }
    public boolean addPic(String pic) {
        return imageUrls.add(pic);
    }


    public boolean removePic(String pic) {
        return imageUrls.remove(pic);
    }

}
