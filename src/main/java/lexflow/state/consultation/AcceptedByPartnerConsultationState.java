package lexflow.state.consultation;

import lexflow.entity.consultation.Consultation;
import lexflow.entity.consultation.ConsultationStatus;
import lexflow.entity.user.Associate;
import lexflow.entity.user.Paralegal;
import lexflow.service.ConsultationService;

public class AcceptedByPartnerConsultationState implements ConsultationState {
	
	@Override
	public Consultation reassignStaff(Consultation consultation, ConsultationService consultationService, Associate associate, Paralegal paralegal) {
		return consultationService.applyReassignment(consultation, associate, paralegal, false);
	}
	
	@Override
	public Consultation markScheduled(Consultation consultation, ConsultationService consultationService) {
		consultation.setConsultationStatus(ConsultationStatus.SCHEDULED);
		
		return consultationService.persist(consultation);
	}
	
}
