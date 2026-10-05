package lexflow.state.consultation;

import lexflow.entity.consultation.Consultation;
import lexflow.entity.user.Associate;
import lexflow.entity.user.Paralegal;
import lexflow.service.ConsultationService;

public class RejectedByAssociateConsultationState implements ConsultationState {
	
	@Override
	public Consultation reassignStaff(Consultation consultation, ConsultationService consultationService, Associate associate, Paralegal paralegal) {
		return consultationService.applyReassignment(consultation, associate, paralegal, false);
	}
	
}
