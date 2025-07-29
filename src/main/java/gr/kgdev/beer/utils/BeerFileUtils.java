package gr.kgdev.beer.utils;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
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

public class BeerFileUtils {

	private static BeerFileConfig defaultFileConfig = new BeerFileConfig().setMaxFileSize(100000000).setMaxReqSize(101000000).setFileSizeThreshold(1);
	
	public static SimpleMessage uploadFile(HttpServletRequest req, HttpServletResponse res, String partName, String uploadLocation) throws Exception {
		return uploadFile(req, res, partName, uploadLocation, defaultFileConfig); 
	}
	
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

	private static String getRequestFileName(HttpServletRequest req, String partName) throws UnsupportedEncodingException, IOException, ServletException {
		return URLDecoder.decode(req.getPart(partName).getSubmittedFileName().replaceAll(" ",""), "UTF-8");
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
