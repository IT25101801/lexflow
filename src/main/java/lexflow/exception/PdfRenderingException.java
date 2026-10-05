package lexflow.exception;

import lombok.Getter;

@Getter
public class PdfRenderingException extends LexFlowException {
	
	private final String documentName;
	
	public PdfRenderingException(String documentName, Throwable cause) {
		super(String.format("Could not render %s as a PDF", documentName), cause);
		this.documentName = documentName;
	}
	
}
