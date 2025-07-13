package dev.shrekback.accounting.dto;

import lombok.Getter;

@Getter
public class UserRegisterDto {
	String firstName;
	String lastName;
	String login;
	String password;
}
