package lexflow.state.consultation;

import lexflow.entity.consultation.Consultation;
import lexflow.entity.consultation.ConsultationStatus;
import lexflow.entity.user.Associate;
import lexflow.entity.user.Paralegal;
import lexflow.exception.consultation.MeetingNotCompletedException;
import lexflow.service.ConsultationService;

public class ScheduledConsultationState implements ConsultationState {
	
	@Override
	public Consultation reassignStaff(Consultation consultation, ConsultationService consultationService, Associate associate, Paralegal paralegal) {
		boolean associateChanged = consultation.getAssociate() == null || !consultation.getAssociate().getUserId().equals(associate.getUserId());
		boolean paralegalChanged = consultation.getParalegal() == null || !consultation.getParalegal().getUserId().equals(paralegal.getUserId());
		boolean changed = associateChanged || paralegalChanged;
		
		if (changed) {
			consultation.setConsultationStatus(ConsultationStatus.ACCEPTED_BY_PARTNER);
		}
		
		return consultationService.applyReassignment(consultation, associate, paralegal, changed);
	}
	
	@Override
	public Consultation associateAccept(Consultation consultation, ConsultationService consultationService) {
		
		if (!consultationService.hasCompletedMeeting(consultation)) {
			throw new MeetingNotCompletedException(consultation.getConsultationId());
		}
		
		consultation.setConsultationStatus(ConsultationStatus.ACCEPTED_BY_ASSOCIATE);
		
		return consultationService.applyStatusChange(consultation);
	}
	
	@Override
	public Consultation associateReject(Consultation consultation, ConsultationService consultationService, String rejectionReason) {
		
		if (!consultationService.hasCompletedMeeting(consultation)) {
			throw new MeetingNotCompletedException(consultation.getConsultationId());
		}
		
		consultation.setRejectionReason(rejectionReason);
		consultation.setConsultationStatus(ConsultationStatus.REJECTED_BY_ASSOCIATE);
		
		return consultationService.applyStatusChange(consultation);
	}
	
}
