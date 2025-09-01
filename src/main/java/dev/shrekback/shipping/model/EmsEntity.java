
package dev.shrekback.shipping.model;

import lombok.*;
        import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Getter
@ToString
@NoArgsConstructor
@Document(collection = "shippingems")
@EqualsAndHashCode(of = "id")
public class EmsEntity {
    @Id
    String id;

    @Setter
    String monthlyShipmentsFrom;

    @Setter
    String monthlyShipmentsTo;

    @Setter
    String weightGramsFrom;

    @Setter
    String weightGramsTO;

    @Setter
    String additional500;

    @Setter
    String group1;
    @Setter
    String group2;
    @Setter
    String group3;
    @Setter
    String group4;
    @Setter
    String group5;
    @Setter
    String group6;
    @Setter
    String group7;
    @Setter
    String group8;
    @Setter
    String group9;
    @Setter
    String group10;
    @Setter
    String group11;
    @Setter
    String group12;
    @Setter
    String group13;

    public EmsEntity(
            String monthlyShipmentsFrom,
            String monthlyShipmentsTo,
            String weightGramsFrom,
            String weightGramsTO,
            String additional500,
            String group1,
            String group2,
            String group3,
            String group4,
            String group5,
            String group6,
            String group7,
            String group8,
            String group9,
            String group10,
            String group11,
            String group12,
            String group13
    ) {
        this.monthlyShipmentsFrom = monthlyShipmentsFrom;
        this.monthlyShipmentsTo = monthlyShipmentsTo;
        this.weightGramsFrom = weightGramsFrom;
        this.weightGramsTO = weightGramsTO;
        this.additional500 = additional500;
        this.group1 = group1;
        this.group2 = group2;
        this.group3 = group3;
        this.group4 = group4;
        this.group5 = group5;
        this.group6 = group6;
        this.group7 = group7;
        this.group8 = group8;
        this.group9 = group9;
        this.group10 = group10;
        this.group11 = group11;
        this.group12 = group12;
        this.group13 = group13;
    }
}
