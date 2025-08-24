package dev.shrekback.post.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class NewPostDto {
    private String name;
    private List<String> imageUrls;
    private Integer quantity;
    private Double price;
    private String category;
    private Double weight;
    private String size;
    private String desc;
    private String color;
    private String material;


}
