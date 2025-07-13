package dev.shrekback.accounting.dto.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class InvalidEmailExeption extends RuntimeException {

	private static final long serialVersionUID = 1L;

}
