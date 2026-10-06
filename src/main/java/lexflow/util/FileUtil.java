package lexflow.util;

import lexflow.entity.document.DocumentType;
import lexflow.exception.PersistenceException;
import lexflow.exception.document.DocumentTooLargeException;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

public class FileUtil {
	
	private static final Path UPLOAD_DIRECTORY = Paths.get("uploads");
	private static final String DEFAULT_FILENAME = "file";
	private static final long MAX_FILE_SIZE_MEGABYTES = 50;
	
	// Create
	
	public static String store(MultipartFile file, Long ownerId, DocumentType documentType) {
		
		if (file == null || file.isEmpty()) {
			return null;
		}
		
		if (file.getSize() > MAX_FILE_SIZE_MEGABYTES * 1024 * 1024) {
			throw new DocumentTooLargeException(file.getOriginalFilename(), MAX_FILE_SIZE_MEGABYTES);
		}
		
		try {
			Files.createDirectories(UPLOAD_DIRECTORY);
			Path target = resolveTarget(ownerId, documentType, file.getOriginalFilename());
			file.transferTo(target);
			
			return target.toString();
		} catch (IOException e) {
			throw new PersistenceException("storing file", e);
		}
		
	}
	
	public static String store(byte[] content, Long ownerId, DocumentType documentType, String originalFilename) {
		
		try {
			Files.createDirectories(UPLOAD_DIRECTORY);
			Path target = resolveTarget(ownerId, documentType, originalFilename);
			Files.write(target, content);
			
			return target.toString();
		} catch (IOException e) {
			throw new PersistenceException("storing file", e);
		}
		
	}
	
	// Read
	
	public static Resource loadAsResource(String filePath) {
		
		try {
			Path path = Paths.get(filePath);
			Resource resource = new UrlResource(path.toUri());
			
			if (!resource.exists() || !resource.isReadable()) {
				throw new PersistenceException("loading file: not found at " + filePath);
			}
			
			return resource;
		} catch (MalformedURLException e) {
			throw new PersistenceException("loading file", e);
		}
		
	}
	
	// Delete
	
	public static void delete(String filePath) {
		
		if (filePath == null) {
			return;
		}
		
		try {
			Files.deleteIfExists(Paths.get(filePath));
		} catch (IOException e) {
			throw new PersistenceException("deleting file", e);
		}
		
	}
	
	// Private Helpers
	
	private static Path resolveTarget(Long ownerId, DocumentType documentType, String originalFilename) {
		String randomSuffix = UUID.randomUUID().toString().substring(0, 6);
		String filename = ownerId + "-" + documentType.name() + "-" + randomSuffix + "-" + cleanFilename(originalFilename);
		Path target = UPLOAD_DIRECTORY.resolve(filename);
		Path uploadDirectory = UPLOAD_DIRECTORY.toAbsolutePath().normalize();
		
		if (!uploadDirectory.equals(target.toAbsolutePath().normalize().getParent())) {
			throw new PersistenceException("storing file: " + filename + " resolves outside the upload directory");
		}
		
		return target;
	}
	
	private static String cleanFilename(String originalFilename) {
		
		if (originalFilename == null) {
			return DEFAULT_FILENAME;
		}
		
		int lastSeparator = Math.max(originalFilename.lastIndexOf('/'), originalFilename.lastIndexOf('\\'));
		String filename = originalFilename.substring(lastSeparator + 1).replaceAll("[<>:\"|?*\\x00-\\x1F]", "_").trim();
		
		if (filename.isEmpty() || filename.equals(".") || filename.equals("..")) {
			return DEFAULT_FILENAME;
		}
		
		return filename;
	}
	
}
