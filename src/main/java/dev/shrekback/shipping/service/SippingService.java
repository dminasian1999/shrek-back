package dev.shrekback.shipping.service;

import java.util.List;

public interface SippingService {
    boolean addCountries(String index, String csv);
    boolean addEms(String index, String csv);
    boolean addEco(String index, String csv);
    List<String> getValidCountries();
    Double getShippingCost(String countryName, double weight);
}
