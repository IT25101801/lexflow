package lexflow.exception.document;

import lexflow.exception.BusinessRuleViolationException;
import lombok.Getter;

@Getter
public class MissingRequiredDocumentException extends BusinessRuleViolationException {
	
	private final String documentLabel;
	
	public MissingRequiredDocumentException(String documentLabel) {
		super(String.format("%s document is required", documentLabel));
		this.documentLabel = documentLabel;
	}
	
}
