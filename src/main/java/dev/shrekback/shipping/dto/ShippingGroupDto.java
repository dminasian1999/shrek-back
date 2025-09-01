package dev.shrekback.shipping.dto;

import lombok.Getter;
import org.springframework.data.annotation.Id;

import java.util.List;
import java.util.Map;

@Getter
public class ShippingGroupDto {
    @Id
    String groupId;
    Double weightFrom ;
    Double weightTo ;

//    private List<String> countries;
    private Boolean availableEcopost;
    private Boolean availableEMS;
    private EmsDto emsDto;
    private EcoPostDto ecoPostDtoMaxitem;

}
