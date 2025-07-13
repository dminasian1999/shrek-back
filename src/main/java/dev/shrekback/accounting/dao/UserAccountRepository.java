package dev.shrekback.accounting.dao;

import dev.shrekback.accounting.model.UserAccount;
import org.springframework.data.repository.CrudRepository;

public interface UserAccountRepository extends CrudRepository<UserAccount, String> {

}
