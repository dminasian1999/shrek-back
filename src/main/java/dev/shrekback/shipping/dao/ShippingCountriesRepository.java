package dev.shrekback.shipping.dao;

import dev.shrekback.shipping.model.ShippingCountry;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface ShippingCountriesRepository extends CrudRepository<ShippingCountry, String> {

    List<ShippingCountry> getShippingCountriesByGroupIdEcoPostIsGreaterThanOrGroupIdESMIsGreaterThan(int groupIdEcoPost, int groupIdESM);
}
