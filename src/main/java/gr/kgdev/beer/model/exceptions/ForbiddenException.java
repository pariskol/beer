package gr.kgdev.beer.model.exceptions;

import java.security.GeneralSecurityException;

public class ForbiddenException extends GeneralSecurityException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	public ForbiddenException(String message) {
		super(message);
	}

	public ForbiddenException(String message, Throwable e) {
		super(message, e);
	}
}
