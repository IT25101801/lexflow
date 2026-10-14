package lexflow.exception.consultation;

import lexflow.exception.BusinessRuleViolationException;
import lombok.Getter;

@Getter
public class ConsultationHasCaseException extends BusinessRuleViolationException {

	private final Long consultationId;
	private final Long caseId;

	public ConsultationHasCaseException(Long consultationId, Long caseId) {
		super("The staff of this consultation cannot be changed because it already has a case; reassign the staff on the case instead");
		this.consultationId = consultationId;
		this.caseId = caseId;
	}

}
