package lexflow.exception.finance;

import lexflow.exception.BusinessRuleViolationException;
import lombok.Getter;

@Getter
public class NoChargesSelectedException extends BusinessRuleViolationException {
	
	private final Long caseId;
	
	public NoChargesSelectedException(Long caseId) {
		super(String.format("No charges were selected to compile an invoice for case %d", caseId));
		this.caseId = caseId;
	}
	
}
