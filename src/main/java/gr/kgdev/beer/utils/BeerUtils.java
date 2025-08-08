package gr.kgdev.beer.utils;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Base64;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import com.google.gson.Gson;

import gr.kgdev.beer.core.Beer;
import gr.kgdev.beer.model.Credentials;
import gr.kgdev.beer.model.exceptions.BadRequestException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class BeerUtils {

	private static final Gson GSON = new Gson();

	public static String parseReqBody(HttpServletRequest req) throws IOException {
        var bodyAsBytes = IOUtils.toByteArray(req.getInputStream());
        var body = StringUtils.toEncodedString(bodyAsBytes, Charset.forName(req.getCharacterEncoding()));
        return body;
	}
	
	@SuppressWarnings("unchecked")
	public static <T> T parseBody(HttpServletRequest req, Class<?> clazz) throws BadRequestException {
		try {
			return (T) GSON.fromJson(BeerUtils.parseReqBody(req), clazz);
		} catch (Exception e) {
			throw new BadRequestException("Could not parse request's body");
		}
	}
	
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
			return (T) GSON.fromJson(new JSONObject(queriesMap).toString(), clazz);
		} catch (Exception e) {
			throw new BadRequestException("Could not parse request's query params");
		}
	}
	
	public static String getPathParam(HttpServletRequest req, String paramName) {
		if (paramName.startsWith(":")) {
			paramName = paramName.substring(1);
		}
		return (String) req.getAttribute(Beer.PATH_PREFIX + paramName);
	}
	
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

	public static void setReqUser(HttpServletRequest req, Object user) {
		req.setAttribute("user", user);
	}
	
	@SuppressWarnings("unchecked")
	public static <T> T getReqUser(HttpServletRequest req) {
		return (T) req.getAttribute("user");
	}
	
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
	
	public static <T> T redirect(HttpServletResponse res, String url) throws IOException {
		res.sendRedirect(url);
		return null;
	}
	
	public static String json(Object object) {
		if (object instanceof Collection<?>)
			return new JSONArray((Collection<?>) object).toString();
		else if (object instanceof Map<?, ?>)
			return new JSONObject((Map<?,?>) object).toString();
		else if (object instanceof String) {
			String str = (String) object;
			return str.startsWith("{") || str.startsWith("[") ? new JSONObject(str).toString() : str;
		}
		else
			return new JSONObject(object).toString();
	}
}
