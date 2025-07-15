package gr.kgdev.beer.utils;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PropertiesLoader {

	private static HashMap<String, Properties> propertiesMap = new HashMap<>();
	private static Logger logger = LoggerFactory.getLogger("spark");
	
	static {
		loadProperties("./rest.properties");
	}
	
	public static void setLogger(Logger logger) {
		PropertiesLoader.logger = logger;
	}
	
	public static void loadProperties(String rootPath) {
		try {
			Files.walk(Paths.get(rootPath))
	        .filter(Files::isRegularFile)
	        .filter(path -> path.toString().endsWith(".properties"))
	        .forEach(path -> {
	        	try (var inputStream = new FileInputStream(path.toFile())) {
					var props = new Properties();
					props.load(inputStream);
					propertiesMap.put(path.getFileName().toString(), props);
		        } catch (IOException e) {
		        	if (logger != null)
		    			logger.error(e.getMessage());
		        	else
		        		e.printStackTrace();
				}
	        });
		} catch (IOException e) {
			if (logger != null)
    			logger.error(e.getMessage());
        	else
        		e.printStackTrace();
		}
	}
	
	@SuppressWarnings("unchecked")
	public static <T>T getProperty(String fileKey, String key, Class<?> clazz) {
		var value = propertiesMap.get(fileKey).get(key);
		try {
			var cons = clazz.getConstructor(String.class);
			var returnedValue = cons.newInstance(value.toString());
			return (T) returnedValue;
		} catch (Exception e) {
			if (logger != null)
    			logger.error(e.getMessage());
        	else
        		e.printStackTrace();
		}
		return null;
	}
	
	@SuppressWarnings("unchecked")
	public static <T>T getProperty(String fileKey, String key, Class<?> clazz, Object defaultValue) {
		var value = getProperty(fileKey, key, clazz);
		return value != null ? (T) value : (T) defaultValue;
	}
	
	/**
	 * Returns the first matching key from all loaded properties
	 * 
	 * @param key
	 * @param clazz
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static <T>T getProperty(String key, Class<?> clazz) {
		Object value = null;
		for (var props : propertiesMap.values()) {
			value = props.get(key);
			if (value != null)
				break;
		}
		try {
			var cons = clazz.getConstructor(String.class);
			var returnedValue = cons.newInstance(value.toString());
			return (T) returnedValue;
		} catch (Exception e) {
			if (logger != null)
    			logger.error(e.getMessage());
        	else
        		e.printStackTrace();
		}
		return null;
	}
	
	/**
	 * Returns the first matching key from all loaded properties
	 * 
	 * @param key
	 * @param clazz
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static <T>T getProperty(String key, Class<?> clazz, Object defaultValue) {
		var value = getProperty(key, clazz);
		return value != null ? (T) value : (T) defaultValue;
	}
	
	public static Properties getPropertiesFromFile(String fileNamePart) {
		for (var key : propertiesMap.keySet()) {
			if (key.contains(fileNamePart))
				return propertiesMap.get(key);
		}
		return null;
	}
	
	
}
