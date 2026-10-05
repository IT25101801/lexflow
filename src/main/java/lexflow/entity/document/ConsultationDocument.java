package lexflow.entity.document;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class ConsultationDocument extends Document {
	
	private final Long consultationId;
	
	public ConsultationDocument(String filePath, String originalFilename, LocalDateTime uploadedAt, Long consultationId) {
		super(DocumentType.CONSULTATION_DOCUMENT, filePath, originalFilename, uploadedAt);
		this.consultationId = consultationId;
	}
	
	public ConsultationDocument(Long documentId, String filePath, String originalFilename, LocalDateTime uploadedAt, Long consultationId) {
		super(documentId, DocumentType.CONSULTATION_DOCUMENT, filePath, originalFilename, uploadedAt);
		this.consultationId = consultationId;
	}
	
}
