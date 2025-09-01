package dev.shrekback.shipping.model;

import dev.shrekback.shipping.dto.EcoPostDto;
import dev.shrekback.shipping.dto.EmsDto;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@EqualsAndHashCode(of = "groupId")
@ToString
@Document(collection = "shippingIL")
public class ShippingGroup {
    @Id
    String groupId;

    Double weightFrom;
    Double weightTo;
    private List<String> countries=new ArrayList<>();
    private Boolean availableEcopost;
    private Boolean availableEMS;
    private Ems emsDto;
    private EcoPost ecoPostDtoMaxitem;

    public ShippingGroup( Double weightFrom, Double weightTo, List<String> countries, Boolean availableEcopost, Boolean availableEMS, Ems emsDto, EcoPostDto ecoPostDtoMaxitem) {
        this.weightFrom = weightFrom;
        this.weightTo = weightTo;
        this.countries = countries;
        this.availableEcopost = availableEcopost;
        this.availableEMS = availableEMS;
        this.emsDto = emsDto;
    }
}
