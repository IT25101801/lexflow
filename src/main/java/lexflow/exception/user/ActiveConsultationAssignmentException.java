package lexflow.exception.user;

import lexflow.exception.BusinessRuleViolationException;
import lombok.Getter;

import java.util.List;

@Getter
public class ActiveConsultationAssignmentException extends BusinessRuleViolationException {
	
	private final Long userId;
	private final List<String> consultationCodes;
	
	public ActiveConsultationAssignmentException(Long userId, List<String> consultationCodes) {
		super(String.format("This user cannot be deleted: still assigned to %s %s", consultationCodes.size() == 1 ? "active consultation" : "active consultations", String.join(", ", consultationCodes)));
		this.userId = userId;
		this.consultationCodes = consultationCodes;
	}
	
}
