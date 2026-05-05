package gr.kgdev.beer.model.exceptions;

public class ForbiddenException extends RuntimeException {

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
