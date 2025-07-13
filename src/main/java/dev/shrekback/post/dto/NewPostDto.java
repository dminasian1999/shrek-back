package dev.shrekback.post.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

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
    private String desc;
    private String color;
    private List<String> materials;
    private LocalDateTime dateCreated;


}
