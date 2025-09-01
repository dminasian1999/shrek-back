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
@NoArgsConstructor

public class ShippingCountry {
    @Id
    @Setter
    String countryName;
    @Setter
    int groupIdESM;
    @Setter
    int groupIdEcoPost;


}
