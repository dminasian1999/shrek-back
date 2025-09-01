package dev.shrekback.shipping.model;

import dev.shrekback.shipping.dto.EcoPostDto;
import dev.shrekback.shipping.dto.EmsDto;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.List;

@Getter
@EqualsAndHashCode(of = "countryName")
@ToString
@Document(collection = "shrek-shipping-countries")
@AllArgsConstructor

public class Country {
    @Id
    @Setter
    String countryName;
    @Setter
    String groupIdESM;
    @Setter
    String groupIdEcoPost;
    @Setter
    boolean isEMSAvailable;
    @Setter
    boolean isEcoPostAvailable;

    @Setter
    List<Ems> emsPackages;
    @Setter
    List<EcoPost> ecoPostPackages;

    public Country() {
        this.ecoPostPackages = new ArrayList<>();
        this.emsPackages = new ArrayList<>();
    }

    public Country(
            String groupIdESM, String groupIdEcoPost,
            Boolean isEcoPostAvailable, Boolean isEMSAvailable, List<EmsDto> emsPackages, List<EcoPostDto> ecoPostPackages) {
        this();
        this.groupIdESM = groupIdESM;
        this.groupIdEcoPost = groupIdEcoPost;
        this.isEcoPostAvailable = isEcoPostAvailable;
        this.isEMSAvailable = isEMSAvailable;
    }
}
