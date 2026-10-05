package lexflow.state.consultation;

import lexflow.entity.consultation.Consultation;
import lexflow.entity.consultation.ConsultationStatus;
import lexflow.entity.user.Associate;
import lexflow.entity.user.Paralegal;
import lexflow.service.ConsultationService;

public class AcceptedByAssociateConsultationState implements ConsultationState {
	
	@Override
	public Consultation reassignStaff(Consultation consultation, ConsultationService consultationService, Associate associate, Paralegal paralegal) {
		boolean associateChanged = consultation.getAssociate() == null || !consultation.getAssociate().getUserId().equals(associate.getUserId());
		boolean paralegalChanged = consultation.getParalegal() == null || !consultation.getParalegal().getUserId().equals(paralegal.getUserId());
		
		if (associateChanged || paralegalChanged) {
			consultation.setConsultationStatus(ConsultationStatus.ACCEPTED_BY_PARTNER);
		}
		
		return consultationService.applyReassignment(consultation, associate, paralegal, false);
	}
	
}
