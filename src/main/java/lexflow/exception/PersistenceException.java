package lexflow.exception;

import lombok.Getter;

@Getter
public class PersistenceException extends LexFlowException {
	
	private final String operation;
	
	public PersistenceException(String operation) {
		super(String.format("Persistence failure during %s", operation));
		this.operation = operation;
	}
	
	public PersistenceException(String operation, Throwable cause) {
		super(String.format("Persistence failure during %s", operation), cause);
		this.operation = operation;
	}
	
}
