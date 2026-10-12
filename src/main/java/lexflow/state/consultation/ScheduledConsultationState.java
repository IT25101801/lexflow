package lexflow.state.consultation;

import lexflow.entity.case_.Case;
import lexflow.entity.consultation.CaseType;
import lexflow.entity.consultation.Consultation;
import lexflow.entity.consultation.ConsultationStatus;
import lexflow.entity.user.Associate;
import lexflow.entity.user.Paralegal;
import lexflow.exception.consultation.InvalidConsultationStateException;
import lexflow.exception.consultation.MeetingNotCompletedException;
import lexflow.service.ConsultationService;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public class ScheduledConsultationState implements ConsultationState {
	
	@Override
	public Consultation updateDetails(Consultation consultation, ConsultationService consultationService, CaseType caseType, List<String> opposingParties, String description, List<MultipartFile> consultationFiles, List<Long> documentIdsToDelete, List<Long> documentIds, List<String> originalFilenames) {
		throw new InvalidConsultationStateException(consultation.getConsultationId(), consultation.getConsultationStatus(), ConsultationStatus.PENDING);
	}
	
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
	public Consultation partnerAccept(Consultation consultation, ConsultationService consultationService, Associate associate, Paralegal paralegal) {
		throw new InvalidConsultationStateException(consultation.getConsultationId(), consultation.getConsultationStatus(), ConsultationStatus.PENDING);
	}
	
	@Override
	public Consultation partnerReject(Consultation consultation, ConsultationService consultationService, String rejectionReason) {
		throw new InvalidConsultationStateException(consultation.getConsultationId(), consultation.getConsultationStatus(), ConsultationStatus.PENDING);
	}
	
	@Override
	public Consultation markScheduled(Consultation consultation, ConsultationService consultationService) {
		throw new InvalidConsultationStateException(consultation.getConsultationId(), consultation.getConsultationStatus(), ConsultationStatus.ACCEPTED_BY_PARTNER);
	}
	
	@Override
	public Consultation markUnscheduled(Consultation consultation, ConsultationService consultationService) {
		consultation.setConsultationStatus(ConsultationStatus.ACCEPTED_BY_PARTNER);
		
		return consultationService.applyStatusChange(consultation);
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
	
	@Override
	public Case openCase(Consultation consultation, ConsultationService consultationService) {
		throw new InvalidConsultationStateException(consultation.getConsultationId(), consultation.getConsultationStatus(), ConsultationStatus.ACCEPTED_BY_ASSOCIATE);
	}
	
	@Override
	public void delete(Consultation consultation, ConsultationService consultationService) {
		throw new InvalidConsultationStateException(consultation.getConsultationId(), consultation.getConsultationStatus(), ConsultationStatus.PENDING);
	}
	
}
