package lexflow.exception;

import lombok.Getter;

@Getter
public class EntityNotFoundException extends LexFlowException {
	
	private final String entityType;
	private final String identifier;
	
	public EntityNotFoundException(String entityType, String identifier) {
		super(String.format("%s not found: %s", entityType, identifier));
		this.entityType = entityType;
		this.identifier = identifier;
	}
	
}
