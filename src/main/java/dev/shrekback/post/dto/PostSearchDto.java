package dev.shrekback.post.dto;

import lombok.Data;

@Data
public class PostSearchDto {
    private String name;
    private String category;
    private String color;
    private String desc;
    private Double minPrice;
    private Double maxPrice;
    private String material;
    private String sortBy;
    private Boolean asc;
}
