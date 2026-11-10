package dev.shrekback.accounting.dao;

import dev.shrekback.accounting.model.Order;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends CrudRepository<Order, String> {

    List<Order> findByUserId(String userId);
    Optional<Order> findByPaypalOrderId(String paypalOrderId);


}
