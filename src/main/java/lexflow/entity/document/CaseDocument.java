package lexflow.entity.document;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class CaseDocument extends Document {
	
	private final Long caseId;
	private final boolean inStaging;
	private final Long taskId;
	
	public CaseDocument(String filePath, String originalFilename, LocalDateTime uploadedAt, Long caseId, boolean inStaging, Long taskId) {
		super(DocumentType.CASE_DOCUMENT, filePath, originalFilename, uploadedAt);
		this.caseId = caseId;
		this.inStaging = inStaging;
		this.taskId = taskId;
	}
	
	public CaseDocument(Long documentId, String filePath, String originalFilename, LocalDateTime uploadedAt, Long caseId, boolean inStaging, Long taskId) {
		super(documentId, DocumentType.CASE_DOCUMENT, filePath, originalFilename, uploadedAt);
		this.caseId = caseId;
		this.inStaging = inStaging;
		this.taskId = taskId;
	}
	
}
