package dev.shrekback.accounting.model;

import lombok.Getter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Document(collection = "tokens")
public class UserToken {

	LocalDateTime expirationDate;
	String login;
	@Id
	String token;

	public UserToken(String login) {
		this.login = login;
		this.token = UUID.randomUUID().toString();
		this.expirationDate = LocalDateTime.now().plusHours(1);
	}
}
