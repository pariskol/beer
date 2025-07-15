package gr.kgdev.beer.core;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@FunctionalInterface
public interface RequestHandler {

	public Object handle(HttpServletRequest req, HttpServletResponse res) throws Exception;
}
