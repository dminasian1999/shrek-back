package dev.shrekback.post.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReceiptDto {
    private String id;
    private String name;
    private String imageUrl;
    private Integer quantity;
    private Double sell;
    private Double buy;
    private Double income;
    private String seller;
    private String category;
    private String type;
    private String desc;
    private LocalDateTime dateCreated;


}
