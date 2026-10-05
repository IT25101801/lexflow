package lexflow.state.case_;

import lexflow.entity.case_.Case;
import lexflow.entity.case_.CaseResolutionStatus;
import lexflow.entity.user.Associate;
import lexflow.entity.user.Paralegal;
import lexflow.service.CaseService;

public class OpenCaseState implements CaseState {
	
	@Override
	public Case reassignStaff(Case case_, CaseService caseService, Associate associate, Paralegal paralegal) {
		boolean associateChanged = case_.getAssociate() == null || !case_.getAssociate().getUserId().equals(associate.getUserId());
		boolean paralegalChanged = case_.getParalegal() == null || !case_.getParalegal().getUserId().equals(paralegal.getUserId());
		
		return caseService.applyReassignment(case_, associate, paralegal, associateChanged || paralegalChanged);
	}
	
	@Override
	public Case close(Case case_, CaseService caseService, CaseResolutionStatus caseResolutionStatus) {
		return caseService.applyClose(case_, caseResolutionStatus);
	}
	
}
