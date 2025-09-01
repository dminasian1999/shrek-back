package dev.shrekback.shipping.service;

public interface SippingService {
    boolean addCountries(String index, String csv);
    boolean addEms(String index, String csv);
    boolean addEco(String index, String csv);

    Double getShippingCost(String countryName, double weight);
}
