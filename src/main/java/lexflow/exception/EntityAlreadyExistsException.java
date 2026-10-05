package lexflow.exception;

import lombok.Getter;

@Getter
public class EntityAlreadyExistsException extends LexFlowException {
	
	private final String entityType;
	private final String identifier;
	
	public EntityAlreadyExistsException(String entityType, String identifier) {
		super(String.format("%s already exists: %s", entityType, identifier));
		this.entityType = entityType;
		this.identifier = identifier;
	}
	
}
