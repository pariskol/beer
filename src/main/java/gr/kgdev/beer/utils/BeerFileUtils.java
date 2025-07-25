package gr.kgdev.beer.utils;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.SQLException;
import java.util.Comparator;
import java.util.List;

import org.slf4j.LoggerFactory;

import gr.kgdev.beer.model.SimpleMessage;
import gr.kgdev.beer.model.exceptions.BadRequestException;
import gr.kgdev.beer.model.exceptions.SparkCoreGeneralException;
import jakarta.servlet.MultipartConfigElement;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;

public class BeerFileUtils {

	private static Integer maxFileSize = 100000000;
	private static Integer maxReqSize = 101000000;
	private static Integer fileSizeThreshold = 1;
	
	// Test : curl -i -X POST -H 'Content-Type: multipart/form-data' -H 'Authorization: Basic YWRtaW46dGVzdA==' -F "file=@test.txt" 192.168.2.5:8080/api/action/upload?messageId=8
	public static SimpleMessage uploadFile(HttpServletRequest req, HttpServletResponse res, String partName, String uploadLocation) throws SQLException, BadRequestException, SparkCoreGeneralException {

		try {
			var logger = LoggerFactory.getLogger("spark");
			
			var location = uploadLocation;
			
			var subLocation = (String) req.getParameter("location");
			if (subLocation != null) {
				location += "/" + subLocation;
			}
			
			
			var multipartConfigElement = new MultipartConfigElement(location, maxFileSize,
					maxReqSize, fileSizeThreshold);
			req.setAttribute("org.eclipse.jetty.multipartConfig", multipartConfigElement);
	
			var parts = req.getParts();
			for (Part part : parts) {
				logger.debug("Name: " + part.getName());
				logger.debug("Size: " + part.getSize());
				logger.debug("Filename: " + part.getSubmittedFileName());
			}
	
			var fName = getRequestFileName(req, partName);
			logger.info("Uploading file: " + fName);
	
			var uploadedFile = req.getPart(partName);
			Files.createDirectories(Paths.get(location));
			var out = Paths.get(location + "/" + fName);
			try (final var in = uploadedFile.getInputStream()) {
				Files.copy(in, out, StandardCopyOption.REPLACE_EXISTING);
				uploadedFile.delete();
			}
			
			return new SimpleMessage("File has been uploaded");
		} catch (Exception e) {
			throw new SparkCoreGeneralException("File upload failed", e);
		}
	}
	
	public static String downloadFile(HttpServletRequest req, HttpServletResponse res, String uploadLocation, String path, Boolean isInline) throws Exception {
	    return downloadFile(res, new File(uploadLocation + "/" + URLDecoder.decode(path, "UTF-8")), isInline);
	}

	public static SimpleMessage deleteFile(HttpServletRequest req, HttpServletResponse res, String uploadLocation, String path) throws Exception {
	    var file = new File(uploadLocation + "/" + path);
	    file.delete();
	    
	    return new SimpleMessage("");
	}

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

	private static String getRequestFileName(HttpServletRequest req, String partName) throws SparkCoreGeneralException {
		try {
			return URLDecoder.decode(req.getPart(partName).getSubmittedFileName().replaceAll(" ",""), "UTF-8");
		} catch(Exception e) {
			throw new SparkCoreGeneralException("Could not get submitted filename", e);
		}
	}
	
	private static String downloadFile(HttpServletResponse res, File file, boolean makeInlineFiles) throws IOException {
		res.setContentType("application/octet-stream");
		res.setHeader("Content-Disposition",
				makeInlineFiles ? "inline" : "attachment" + 
				"; filename=" + file.getName());
		
		if (makeInlineFiles) {
			res.setHeader("Content-Type", "image/png or image/jpeg or application/pdf");
		}
		
        try(var bufferedInputStream = new BufferedInputStream(new FileInputStream(file));)
        {
            var buffer = new byte[32768];
            int len;
            while ((len = bufferedInputStream.read(buffer)) > 0) {
            	res.getOutputStream().write(buffer,0,len);
            }
        }
        
        return "";
	}

}
