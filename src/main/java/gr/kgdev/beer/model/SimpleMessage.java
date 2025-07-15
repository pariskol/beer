package gr.kgdev.beer.model;

import org.json.JSONObject;

public class SimpleMessage {

	private String message;

	public SimpleMessage(String message) {
		super();
		this.message = message;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}
	
	@Override
	public String toString() {
		return new JSONObject(this).toString();
	}

}
