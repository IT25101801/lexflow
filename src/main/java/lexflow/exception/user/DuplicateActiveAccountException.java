package lexflow.exception.user;

import lexflow.exception.BusinessRuleViolationException;
import lombok.Getter;

@Getter
public class DuplicateActiveAccountException extends BusinessRuleViolationException {
	
	private final String fieldLabel;
	private final String value;
	
	public DuplicateActiveAccountException(String fieldLabel, String value) {
		super(String.format("Another active account already uses the %s %s", fieldLabel, value));
		this.fieldLabel = fieldLabel;
		this.value = value;
	}
	
}
