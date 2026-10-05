package lexflow.exception.user;

import lexflow.exception.BusinessRuleViolationException;
import lombok.Getter;

import java.util.List;

@Getter
public class OpenCaseAssignmentException extends BusinessRuleViolationException {
	
	private final Long userId;
	private final List<String> caseCodes;
	
	public OpenCaseAssignmentException(Long userId, List<String> caseCodes) {
		super(String.format("User %d cannot be deleted: still assigned to %s %s", userId, caseCodes.size() == 1 ? "open case" : "open cases", String.join(", ", caseCodes)));
		this.userId = userId;
		this.caseCodes = caseCodes;
	}
	
}
