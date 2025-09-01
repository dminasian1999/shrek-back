package dev.shrekback.shipping.dto;

import lombok.Getter;

import java.util.List;

@Getter
public class EcoPostDto {
    Integer weightFrom;
    Integer weightTo;
    Double price;
    List<EcoPostItemDto> ecoPostsItems;
}
