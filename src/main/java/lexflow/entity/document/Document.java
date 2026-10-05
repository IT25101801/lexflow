package lexflow.entity.document;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public abstract class Document {
	
	protected Long documentId;
	protected final DocumentType documentType;
	protected String filePath;
	protected String originalFilename;
	protected LocalDateTime uploadedAt;
	
	protected Document(DocumentType documentType, String filePath, String originalFilename, LocalDateTime uploadedAt) {
		this.documentType = documentType;
		this.filePath = filePath;
		this.originalFilename = originalFilename;
		this.uploadedAt = uploadedAt;
	}
	
	protected Document(Long documentId, DocumentType documentType, String filePath, String originalFilename, LocalDateTime uploadedAt) {
		this.documentId = documentId;
		this.documentType = documentType;
		this.filePath = filePath;
		this.originalFilename = originalFilename;
		this.uploadedAt = uploadedAt;
	}
	
}
