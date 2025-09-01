package dev.shrekback.shipping.dao;

import dev.shrekback.shipping.model.BulkMailRate;
import dev.shrekback.shipping.model.EcoPost;
import dev.shrekback.shipping.model.ShippingCountry;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface ShippingECORepository extends CrudRepository<EcoPost, String> {
}
