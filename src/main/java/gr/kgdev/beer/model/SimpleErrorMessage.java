package gr.kgdev.beer.model;

import java.time.LocalDateTime;
import java.util.UUID;

import com.google.gson.Gson;

public class SimpleErrorMessage {

	private static final Gson GSON = new Gson();

	private String message;
	private String tag = UUID.randomUUID().toString();
	private String datetime = LocalDateTime.now().toString();
	
	public SimpleErrorMessage(String message) {
		super();
		this.message = message;
	}
	
	public SimpleErrorMessage(String message, String tag, String datetime) {
		this(message);
		this.tag = tag;
		this.datetime = datetime;
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
	

	public String getDatetime() {
		return datetime;
	}

	public void setDatetime(String datetime) {
		this.datetime = datetime;
	}

	@Override
	public String toString() {
		return GSON.toJson(this);
	}

}
