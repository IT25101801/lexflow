package lexflow.state.case_;

import lexflow.entity.case_.Case;
import lexflow.entity.case_.CaseResolutionStatus;
import lexflow.entity.case_.CaseStatus;
import lexflow.entity.user.Associate;
import lexflow.entity.user.Paralegal;
import lexflow.exception.case_.InvalidCaseStateException;
import lexflow.service.CaseService;

public class ClosedCaseState implements CaseState {
	
	@Override
	public Case reassignStaff(Case case_, CaseService caseService, Associate associate, Paralegal paralegal) {
		throw new InvalidCaseStateException(case_.getCaseId(), case_.getCaseStatus(), CaseStatus.OPEN);
	}
	
	@Override
	public Case close(Case case_, CaseService caseService, CaseResolutionStatus caseResolutionStatus) {
		throw new InvalidCaseStateException(case_.getCaseId(), case_.getCaseStatus(), CaseStatus.OPEN);
	}
	
}
