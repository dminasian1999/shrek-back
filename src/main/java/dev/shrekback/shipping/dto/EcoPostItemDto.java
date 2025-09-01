package dev.shrekback.shipping.dto;

import lombok.Getter;

@Getter
public class EcoPostItemDto {
    Integer itemsFrom;
    Integer itemsTo;
    Double price;
}
