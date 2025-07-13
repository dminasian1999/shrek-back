package dev.shrekback.post.dto;

import lombok.Getter;

import java.time.LocalDate;

@Getter
public class QueryDto {
    String query;
    Boolean asc;
    String id;
    String name;
    String category;
    String type;
    String desc;
    Double minPrice;
    Double maxPrice;
    LocalDate dateCreated;
    LocalDate dateFrom;
    LocalDate dateTo;
}


