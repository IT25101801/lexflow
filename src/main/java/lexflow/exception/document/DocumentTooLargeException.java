package lexflow.exception.document;

import lexflow.exception.BusinessRuleViolationException;
import lombok.Getter;

@Getter
public class DocumentTooLargeException extends BusinessRuleViolationException {
	
	private final String filename;
	private final long maxFileSizeMegabytes;
	
	public DocumentTooLargeException(String filename, long maxFileSizeMegabytes) {
		super(String.format("%s is larger than the %d MB limit", filename, maxFileSizeMegabytes));
		this.filename = filename;
		this.maxFileSizeMegabytes = maxFileSizeMegabytes;
	}
	
}
