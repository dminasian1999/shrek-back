package dev.shrekback.post.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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
    private String desc;
    private String color;
    private List<String> materials;
    private LocalDateTime dateCreated;


}
