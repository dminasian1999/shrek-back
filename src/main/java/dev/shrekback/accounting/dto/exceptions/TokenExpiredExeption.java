package dev.shrekback.accounting.dto.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.FORBIDDEN)
public class TokenExpiredExeption extends RuntimeException {

	private static final long serialVersionUID = 1L;

}
