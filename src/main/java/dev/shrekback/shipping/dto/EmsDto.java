package dev.shrekback.shipping.dto;

import lombok.Getter;

@Getter
public class EmsDto {
    Integer weightFrom;
    Integer weightTo;
    Double price;
    Double additional500;
}
