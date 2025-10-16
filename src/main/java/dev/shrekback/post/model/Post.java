package dev.shrekback.post.model;

import dev.shrekback.post.dto.SizeQuantityDto;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.*;

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
    private double price;
    private String category;
    private String subCategory;

    private double weight;
    private String desc;
    private String color;
    private String material;
    private LocalDateTime dateCreated;
    private List<SizeQuantityDto> sizeQuantities;

    public Post() {
        this.dateCreated = LocalDateTime.now();
        imageUrls = new ArrayList<>();
        sizeQuantities = new ArrayList<>();
    }

    public Post(String name,  double price, String category, String subCategory,double weight,  String desc, String color,String material) {
        this();
        this.name = name;
        this.price = price;
        this.category = category;
        this.subCategory = subCategory;
        this.weight = weight;
        this.desc = desc;
        this.color = color;
        this.material = material;
    }
//
//    public void adjust(Adjustment adjustment) {
//        if (adjustment.isAdd()) {
//            this.quantity += adjustment.getQuantity();
//        } else {
//            this.quantity -= adjustment.getQuantity();
//        }
//    }
    public boolean addPic(String pic) {
        return imageUrls.add(pic);
    }


    public boolean removePic(String pic) {
        return imageUrls.remove(pic);
    }


}
