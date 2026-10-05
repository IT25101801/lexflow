package lexflow.exception.user;

import lexflow.exception.BusinessRuleViolationException;

public class MissingBarNumberException extends BusinessRuleViolationException {
	
	public MissingBarNumberException() {
		super("Bar number is required for Partner and Associate staff");
	}
	
}
