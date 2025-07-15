package gr.kgdev.beer.model;

import org.json.JSONObject;

public class SimpleErrorMessage {

	private String message;
	private String tag;

	public SimpleErrorMessage(String message, String tag) {
		super();
		this.message = message;
		this.tag = tag;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}
	
	
	public String getTag() {
		return tag;
	}

	public void setTag(String tag) {
		this.tag = tag;
	}

	@Override
	public String toString() {
		return new JSONObject(this).toString();
	}

}
