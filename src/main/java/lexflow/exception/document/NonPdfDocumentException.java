package lexflow.exception.document;

import lexflow.exception.BusinessRuleViolationException;
import lombok.Getter;

@Getter
public class NonPdfDocumentException extends BusinessRuleViolationException {
	
	private final String documentLabel;
	private final String filename;
	
	public NonPdfDocumentException(String documentLabel, String filename) {
		super(String.format("%s document must be a PDF file, but %s is not", documentLabel, filename));
		this.documentLabel = documentLabel;
		this.filename = filename;
	}
	
}
