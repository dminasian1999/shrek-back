package dev.shrekback.post.dto;

import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

@Getter
public class QueryDto {
    String sortField;
    Boolean asc;
    String id;
    String name;
    String category;
    String subCategory;

    String color;
    private String material;
    String desc;
    Double minPrice;
    Double maxPrice;
    LocalDate dateCreated;
    LocalDate dateFrom;
    LocalDate dateTo;
}



