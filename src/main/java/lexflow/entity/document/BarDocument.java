package lexflow.entity.document;

import java.time.LocalDateTime;

public class BarDocument extends UserDocument {
	
	public BarDocument(String filePath, String originalFilename, LocalDateTime uploadedAt, Long userId) {
		super(DocumentType.BAR_DOCUMENT, filePath, originalFilename, uploadedAt, userId);
	}
	
	public BarDocument(Long documentId, String filePath, String originalFilename, LocalDateTime uploadedAt, Long userId) {
		super(documentId, DocumentType.BAR_DOCUMENT, filePath, originalFilename, uploadedAt, userId);
	}
	
}
