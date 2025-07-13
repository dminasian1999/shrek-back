package dev.shrekback.post.dao;

import dev.shrekback.post.model.Receipt;
import org.springframework.data.repository.CrudRepository;

public interface ReceiptRepository extends CrudRepository<Receipt, String> {
}
