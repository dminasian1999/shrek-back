package dev.shrekback.shipping.dto;

import lombok.Getter;

import java.util.List;

@Getter
public class CountryDto {
    String countryName;
    String groupId;
    Boolean isEcoPostAvailable;
    Boolean isEMSAvailable;
    List<EmsDto> emsPackages;
    List<EcoPostDto> ecoPostPackages;
}
