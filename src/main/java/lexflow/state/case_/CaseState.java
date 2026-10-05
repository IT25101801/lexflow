package lexflow.state.case_;

import lexflow.entity.case_.Case;
import lexflow.entity.case_.CaseResolutionStatus;
import lexflow.entity.user.Associate;
import lexflow.entity.user.Paralegal;
import lexflow.service.CaseService;

public interface CaseState {
	
	Case reassignStaff(Case case_, CaseService caseService, Associate associate, Paralegal paralegal);
	
	Case close(Case case_, CaseService caseService, CaseResolutionStatus caseResolutionStatus);
	
}
