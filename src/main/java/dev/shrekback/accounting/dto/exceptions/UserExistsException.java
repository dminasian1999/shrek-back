package dev.shrekback.accounting.dto.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.CONFLICT)
public class UserExistsException extends RuntimeException{

	/**
	 *
	 */
	private static final long serialVersionUID = -40089560762595491L;


}
