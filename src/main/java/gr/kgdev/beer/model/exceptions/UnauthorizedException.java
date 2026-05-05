package gr.kgdev.beer.model.exceptions;

public class UnauthorizedException extends RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public UnauthorizedException() {
		super();
	}
	
	public UnauthorizedException(String message) {
		super(message);
	}

	public UnauthorizedException(String message, Throwable e) {
		super(message, e);
	}
}
