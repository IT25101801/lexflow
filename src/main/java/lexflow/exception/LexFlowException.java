package lexflow.exception;

public abstract class LexFlowException extends RuntimeException {
	
	protected LexFlowException(String message) {
		super(message);
	}
	
	protected LexFlowException(String message, Throwable cause) {
		super(message, cause);
	}
	
}
