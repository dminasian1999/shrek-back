package dev.shrekback.shipping.dao;

import dev.shrekback.shipping.model.Ems;
import dev.shrekback.shipping.model.ShippingCountry;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface ShippingEMSRepository extends CrudRepository<Ems, String> {
}
