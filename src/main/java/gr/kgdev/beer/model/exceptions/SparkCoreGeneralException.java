package gr.kgdev.beer.model.exceptions;

public class SparkCoreGeneralException extends Exception {

	private static final long serialVersionUID = 1L;

	public SparkCoreGeneralException(String message) {
		super(message);
	}
	
	public SparkCoreGeneralException(String message, Exception ex) {
		super(message, ex);
	}
}
