package gr.kgdev.beer.core;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@FunctionalInterface
public interface FilterHandler {

	public void handle(HttpServletRequest req, HttpServletResponse res) throws Exception;
}
