package lexflow.entity.document;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public abstract class UserDocument extends Document {
	
	protected final Long userId;
	
	protected UserDocument(DocumentType documentType, String filePath, String originalFilename, LocalDateTime uploadedAt, Long userId) {
		super(documentType, filePath, originalFilename, uploadedAt);
		this.userId = userId;
	}
	
	protected UserDocument(Long documentId, DocumentType documentType, String filePath, String originalFilename, LocalDateTime uploadedAt, Long userId) {
		super(documentId, documentType, filePath, originalFilename, uploadedAt);
		this.userId = userId;
	}
	
}
