package lexflow.entity.document;

import java.time.LocalDateTime;

public class NicDocument extends UserDocument {
	
	public NicDocument(String filePath, String originalFilename, LocalDateTime uploadedAt, Long userId) {
		super(DocumentType.NIC_DOCUMENT, filePath, originalFilename, uploadedAt, userId);
	}
	
	public NicDocument(Long documentId, String filePath, String originalFilename, LocalDateTime uploadedAt, Long userId) {
		super(documentId, DocumentType.NIC_DOCUMENT, filePath, originalFilename, uploadedAt, userId);
	}
	
}
