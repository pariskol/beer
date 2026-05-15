package gr.kgdev.beer.utils;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.List;

import org.slf4j.LoggerFactory;

import gr.kgdev.beer.model.SimpleMessage;
import jakarta.servlet.MultipartConfigElement;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;

/**
 * Utility class for handling file-related operations such as uploading,
 * downloading, deleting, and listing files in a servlet-based environment.
 * <p>
 * This class is designed to work with multipart HTTP requests and supports
 * configurable upload limits, caching behavior, and storage locations.
 * <p>
 * All methods are static and intended to be used as helpers in controllers
 * or servlets.
 */
public class BeerFileUtils {

	private static BeerFileConfig fileConfig = 
		new BeerFileConfig()
			.setMaxFileSizeMb(100)
			.setMaxReqSizeMb(100)
			.setFileSizeThreshold(1)
			.setCacheEnabled(true);

	/**
     * Sets the global file configuration used by file upload and download operations.
     *
     * @param config the {@link BeerFileConfig} containing size limits,
     *               thresholds, and caching settings
     */
	public static void setFileConfig(BeerFileConfig config) {
		fileConfig = config;
	}
	
	/**
     * Uploads a file from a multipart HTTP request using the default file configuration.
     *
     * @param req the {@link HttpServletRequest} containing the multipart data
     * @param res the {@link HttpServletResponse}
     * @param partName the name of the multipart form field containing the file
     * @param uploadLocation the directory where the file should be stored
     * @return a {@link SimpleMessage} indicating upload success
     * @throws Exception if file upload or request parsing fails
     */
	public static SimpleMessage uploadFile(HttpServletRequest req, HttpServletResponse res, String partName, String uploadLocation) throws Exception {
		return uploadFile(req, res, partName, uploadLocation, fileConfig); 
	}
	
	/**
     * Uploads a file from a multipart HTTP request using a custom file configuration.
     *
     * @param req the {@link HttpServletRequest} containing the multipart data
     * @param res the {@link HttpServletResponse}
     * @param partName the name of the multipart form field containing the file
     * @param uploadLocation the directory where the file should be stored
     * @param config the {@link BeerFileConfig} to apply for this upload
     * @return a {@link SimpleMessage} indicating upload success
     * @throws Exception if file upload or request parsing fails
     */
	public static SimpleMessage uploadFile(HttpServletRequest req, HttpServletResponse res, String partName, String uploadLocation, BeerFileConfig config) throws Exception {
			var logger = LoggerFactory.getLogger(BeerFileUtils.class);
			var location = uploadLocation;
			var multipartConfigElement = new MultipartConfigElement(location, config.getMaxFileSize(),
					config.getMaxReqSize(), config.getFileSizeThreshold());
			req.setAttribute("org.eclipse.jetty.multipartConfig", multipartConfigElement);
	
			var parts = req.getParts();
			for (Part part : parts) {
				logger.debug("Name: " + part.getName());
				logger.debug("Size: " + part.getSize());
				logger.debug("Filename: " + part.getSubmittedFileName());
			}
	
			var fileName = getRequestFileName(req, partName);
			logger.info("Uploading file: " + fileName);
	
			var uploadedFile = req.getPart(partName);
			Files.createDirectories(Paths.get(location));
			var out = Paths.get(location + "/" + fileName);
			try (final var in = uploadedFile.getInputStream()) {
				Files.copy(in, out, StandardCopyOption.REPLACE_EXISTING);
				uploadedFile.delete();
			}
			
			return new SimpleMessage("File has been uploaded");
	}
	
	/**
     * Streams a file to the HTTP response for download or inline display.
     *
     * @param req the {@link HttpServletRequest}
     * @param res the {@link HttpServletResponse} to write the file content to
     * @param uploadLocation the base directory where files are stored
     * @param path the relative path or filename of the file to download
     * @param isInline whether the file should be displayed inline instead of downloaded
     * @return {@code null} after the response is written
     * @throws Exception if the file cannot be found or streamed
     */
	public static String downloadFile(HttpServletRequest req, HttpServletResponse res, String uploadLocation, String path, Boolean isInline) throws Exception {
	    return downloadFile(res, new File(uploadLocation + "/" + URLDecoder.decode(path, "UTF-8")), isInline);
	}

	/**
     * Deletes a file from the upload directory.
     *
     * @param req the {@link HttpServletRequest}
     * @param res the {@link HttpServletResponse}
     * @param uploadLocation the base directory where files are stored
     * @param path the relative path or filename of the file to delete
     * @return a {@link SimpleMessage} indicating deletion result
     * @throws Exception if file deletion fails
     */
	public static SimpleMessage deleteFile(HttpServletRequest req, HttpServletResponse res, String uploadLocation, String path) throws Exception {
	    var file = new File(uploadLocation + "/" + path);
	    file.delete();
	    
	    return new SimpleMessage("");
	}

	/**
     * Lists all files in the given upload directory.
     * <p>
     * Only regular files are included (directories are excluded), and
     * results are sorted by last modified time.
     *
     * @param uploadLocation the directory to scan
     * @return a list of file names
     * @throws IOException if directory access fails
     */
	public static List<String> listFiles(String uploadLocation) throws IOException {
		var dir = uploadLocation;
	    try (var stream = Files.list(Paths.get(dir))){
	        return stream
	          .filter(file -> !Files.isDirectory(file))
	          .map(Path::toFile)
	          .sorted(Comparator.comparingLong(File::lastModified))
	          .map(File::getName)
	          .toList();
	    }
	}

	/**
     * Extracts and decodes the original filename from a multipart request part.
     *
     * @param req the {@link HttpServletRequest} containing the multipart data
     * @param partName the name of the multipart form field
     * @return the decoded filename
     * @throws UnsupportedEncodingException if UTF-8 decoding fails
     * @throws IOException if request access fails
     * @throws ServletException if multipart parsing fails
     */
	public static String getRequestFileName(HttpServletRequest req, String partName) throws UnsupportedEncodingException, IOException, ServletException {
		return URLDecoder.decode(req.getPart(partName).getSubmittedFileName().replaceAll(" ",""), "UTF-8");
	}
	
	/**
     * Writes a file to the HTTP response output stream.
     * <p>
     * Sets appropriate content type, cache headers, and content disposition.
     *
     * @param res the {@link HttpServletResponse} to write to
     * @param file the file to stream
     * @param makeInlineFiles whether the file should be displayed inline
     * @return {@code null} after streaming is complete
     * @throws IOException if file reading or response writing fails
     */
	private static String downloadFile(HttpServletResponse res, File file, boolean makeInlineFiles) throws IOException {
	    String contentType = Files.probeContentType(file.toPath());
	    res.setContentType(contentType != null ? contentType : "application/octet-stream");

	    if (fileConfig.getCacheEnabled()) {
	    	res.setHeader("Cache-Control", "public, max-age=86400"); // cache for 1 day
	    }
	    
	    res.setHeader(
	        "Content-Disposition",
	        (makeInlineFiles ? "inline" : "attachment") +
	        "; filename=\"" + file.getName() + "\""
	    );

	    try (var in = Files.newInputStream(file.toPath());
	         var out = res.getOutputStream()) {
	        in.transferTo(out);
	        out.flush();
	    }
        
        return null;
	}

}
