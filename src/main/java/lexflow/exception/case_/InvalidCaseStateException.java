package lexflow.exception.case_;

import lexflow.entity.case_.CaseStatus;
import lexflow.exception.InvalidStateException;
import lombok.Getter;

@Getter
public class InvalidCaseStateException extends InvalidStateException {
	
	private final Long caseId;
	private final CaseStatus currentStatus;
	private final CaseStatus requiredStatus;
	
	public InvalidCaseStateException(Long caseId, CaseStatus currentStatus, CaseStatus requiredStatus) {
		super(String.format("This case is %s, but it must be %s to do this.", currentStatus.name().toLowerCase().replace('_', ' '), requiredStatus.name().toLowerCase().replace('_', ' ')));
		this.caseId = caseId;
		this.currentStatus = currentStatus;
		this.requiredStatus = requiredStatus;
	}
	
}
