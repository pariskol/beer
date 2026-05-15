package gr.kgdev.beer.utils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import com.google.gson.Gson;
import com.google.gson.JsonParser;

import gr.kgdev.beer.core.Beer;
import gr.kgdev.beer.model.Credentials;
import gr.kgdev.beer.model.exceptions.BadRequestException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;


/**
 * General-purpose utility class providing helper methods for
 * HTTP request handling, JSON processing, authentication parsing,
 * and servlet-related convenience operations.
 * <p>
 * This class centralizes common logic used across controllers and
 * middleware layers of the application.
 * <p>
 * All methods are static and stateless.
 */
public class BeerUtils {

	private static final Gson GSON = new Gson();

	/**
     * Reads and returns the raw request body as a string.
     *
     * @param req the {@link HttpServletRequest} to read from
     * @return the request body as a string
     * @throws IOException if reading the input stream fails
     */
	public static String parseReqBody(HttpServletRequest req) throws IOException {
        var bodyAsBytes = IOUtils.toByteArray(req.getInputStream());
        var body = StringUtils.toEncodedString(bodyAsBytes,  StandardCharsets.UTF_8);
        return body;
	}
	
	/**
     * Parses the HTTP request body into an instance of the given class.
     * <p>
     * The request body is expected to be in JSON format.
     *
     * @param <T> the target type
     * @param req the {@link HttpServletRequest} containing the body
     * @param clazz the class to deserialize the body into
     * @return an instance of the requested type
     * @throws BadRequestException if parsing or deserialization fails
     */
	@SuppressWarnings("unchecked")
	public static <T> T parseBody(HttpServletRequest req, Class<?> clazz) throws BadRequestException {
		try {
			return (T) GSON.fromJson(BeerUtils.parseReqBody(req), clazz);
		} catch (Exception e) {
			throw new BadRequestException("Could not parse request's body");
		}
	}
	
	/**
     * Parses HTTP query parameters into an instance of the given class.
     * <p>
     * Single-value parameters are mapped as strings, while multi-value
     * parameters are mapped as arrays.
     *
     * @param <T> the target type
     * @param req the {@link HttpServletRequest} containing query parameters
     * @param clazz the class to deserialize parameters into
     * @return an instance of the requested type
     * @throws BadRequestException if parsing or deserialization fails
     */
	@SuppressWarnings("unchecked")
	public static <T> T parseQueryParams(HttpServletRequest req, Class<?> clazz) throws BadRequestException {
		try {
			var queriesMap = new HashMap<>();
			
			req.getParameterMap().entrySet().forEach( e -> {
				var value = e.getValue();
				if (value.length == 1) {
					queriesMap.put(e.getKey(), value[0]);
				}
				else if (value.length > 1) {
					queriesMap.put(e.getKey(), value);
				}
			});
			return (T) GSON.fromJson(GSON.toJson(queriesMap), clazz);
		} catch (Exception e) {
			throw new BadRequestException("Could not parse request's query params");
		}
	}
	
	/**
     * Retrieves a path parameter previously stored as a request attribute.
     *
     * @param req the {@link HttpServletRequest}
     * @param paramName the name of the path parameter (with or without ':' prefix)
     * @return the value of the path parameter, or {@code null} if not found
     */
	public static String getPathParam(HttpServletRequest req, String paramName) {
		if (paramName.startsWith(":")) {
			paramName = paramName.substring(1);
		}
		return (String) req.getAttribute(Beer.PATH_PREFIX + paramName);
	}
	
	/**
     * Parses HTTP Basic Authentication credentials from the request.
     *
     * @param req the {@link HttpServletRequest} containing the Authorization header
     * @return a {@link Credentials} object containing username and password
     * @throws BadRequestException if the Authorization header is missing or invalid
     */
	public static Credentials parseBasicAuthCredentials(HttpServletRequest req) throws BadRequestException {
		var authorizationHeader = req.getHeader("Authorization");
		if (authorizationHeader == null)
			throw new BadRequestException("No authorization header");
		
		var decodedBytes = Base64.getDecoder().decode(authorizationHeader.replaceAll("Basic ", "").getBytes());
		var decoded = new String(decodedBytes);

		var userData = decoded.split(":");
		var username = userData[0];
		var password = userData[1];
		

		return new Credentials(username, password);
	}

	/**
     * Stores an authenticated user object in the request context.
     *
     * @param req the {@link HttpServletRequest}
     * @param user the user object to store
     */
	public static void setReqUser(HttpServletRequest req, Object user) {
		req.setAttribute("user", user);
	}
	
	/**
     * Retrieves the authenticated user object from the request context.
     *
     * @param <T> the expected user type
     * @param req the {@link HttpServletRequest}
     * @return the stored user object, or {@code null} if not set
     */
	@SuppressWarnings("unchecked")
	public static <T> T getReqUser(HttpServletRequest req) {
		return (T) req.getAttribute("user");
	}
	
	/**
     * Returns a specific segment of the request URI path.
     *
     * @param request the {@link HttpServletRequest}
     * @param index the zero-based index of the path segment
     * @return the path segment, or {@code null} if out of bounds
     */
	public static String getPathSegment(HttpServletRequest request, int index) {
        var pathInfo = request.getRequestURI(); // Returns part after servlet mapping

        if (pathInfo == null || pathInfo.equals("/")) {
            return null;
        }

        var segments = pathInfo.split("/");
        // skip first slash
        var realIndex = index + 1;

        if (realIndex >= 0 && realIndex < segments.length) {
            return segments[realIndex];
        }

        return null;
    }
	
	/**
     * Sends an HTTP redirect response to the given URL.
     *
     * @param <T> the return type (always {@code null})
     * @param res the {@link HttpServletResponse}
     * @param url the URL to redirect to
     * @return {@code null}
     * @throws IOException if sending the redirect fails
     */
	public static <T> T redirect(HttpServletResponse res, String url) throws IOException {
		res.sendRedirect(url);
		return null;
	}
	
	/**
     * Converts an object to its JSON string representation.
     * <p>
     * Supports collections, maps, raw JSON strings, and arbitrary objects.
     *
     * @param object the object to convert
     * @return a JSON-formatted string
     */
	public static String json(Object object) {
	    if (object instanceof String str) {
	        str = str.trim();
	        if (str.startsWith("{") || str.startsWith("[")) {
	            try {
	                // validate and normalize json
	                return GSON.toJson(JsonParser.parseString(str));
	            } catch (Exception e) {
	                return str; 
	            }
	        }
	        return str;
	    }
	    
	    return GSON.toJson(object);
	}
}
