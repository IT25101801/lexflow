package lexflow.entity.document;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class InvoiceDocument extends Document {
	
	private final Long invoiceId;
	
	public InvoiceDocument(String filePath, String originalFilename, LocalDateTime uploadedAt, Long invoiceId) {
		super(DocumentType.INVOICE_DOCUMENT, filePath, originalFilename, uploadedAt);
		this.invoiceId = invoiceId;
	}
	
	public InvoiceDocument(Long documentId, String filePath, String originalFilename, LocalDateTime uploadedAt, Long invoiceId) {
		super(documentId, DocumentType.INVOICE_DOCUMENT, filePath, originalFilename, uploadedAt);
		this.invoiceId = invoiceId;
	}
	
}
