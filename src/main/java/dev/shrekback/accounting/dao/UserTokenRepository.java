package dev.shrekback.accounting.dao;

import dev.shrekback.accounting.model.UserToken;
import org.springframework.data.repository.CrudRepository;

public interface UserTokenRepository extends CrudRepository<UserToken, String> {

}
