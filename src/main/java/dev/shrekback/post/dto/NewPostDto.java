package dev.shrekback.post.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class NewPostDto {
    private String name;
    private List<String> imageUrls;
    private Double price;
    private String category;
    private String subCategory;

    private Double weight;
    private String desc;
    private String color;
    private String material;
    private List<SizeQuantityDto> sizeQuantities;


}
