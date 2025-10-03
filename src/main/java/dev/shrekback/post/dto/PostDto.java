package dev.shrekback.post.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PostDto {
    private String id;
    private String name;
    private List<String> imageUrls;
    private Integer quantity;
    private Double price;
    private String category;
    private String subCategory;
    private Double weight;
    private String size;
    private String desc;
    private String color;
    private String material;
    private LocalDateTime dateCreated;


}
