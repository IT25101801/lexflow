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
		super(String.format("This case cannot be processed: status must be %s, but it is currently %s", requiredStatus, currentStatus));
		this.caseId = caseId;
		this.currentStatus = currentStatus;
		this.requiredStatus = requiredStatus;
	}
	
}
