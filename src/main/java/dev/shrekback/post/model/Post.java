package dev.shrekback.post.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
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
public class Post {
    @Id
    private String id;
    private String name;
    private List<String> imageUrls;
    private int quantity;
    private double price;
    private String category;
    private String desc;
    private String color;
    private List<String> materials;
    private LocalDateTime dateCreated;

    public Post() {
        this.dateCreated = LocalDateTime.now();
        this.imageUrls = new ArrayList<>();
        this.materials = new ArrayList<>();
    }

    public Post(String name, int quantity, double price, String category, String desc, String color) {
        this();
        this.name = name;
        this.quantity = quantity;
        this.price = price;
        this.category = category;
        this.desc = desc;
        this.color = color;
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

    public boolean addMaterial(String m) {
        return materials.add(m);
    }


    public boolean removeMaterial(String m) {
        return materials.remove(m);
    }

}
