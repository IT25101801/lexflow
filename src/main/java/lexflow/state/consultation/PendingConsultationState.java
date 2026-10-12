package lexflow.state.consultation;

import lexflow.entity.case_.Case;
import lexflow.entity.consultation.CaseType;
import lexflow.entity.consultation.Consultation;
import lexflow.entity.consultation.ConsultationStatus;
import lexflow.entity.user.Associate;
import lexflow.entity.user.Paralegal;
import lexflow.exception.consultation.InvalidConsultationStateException;
import lexflow.service.ConsultationService;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public class PendingConsultationState implements ConsultationState {
	
	@Override
	public Consultation updateDetails(Consultation consultation, ConsultationService consultationService, CaseType caseType, List<String> opposingParties, String description, List<MultipartFile> consultationFiles, List<Long> documentIdsToDelete, List<Long> documentIds, List<String> originalFilenames) {
		return consultationService.applyDetails(consultation, caseType, opposingParties, description, consultationFiles, documentIdsToDelete, documentIds, originalFilenames);
	}
	
	@Override
	public Consultation reassignStaff(Consultation consultation, ConsultationService consultationService, Associate associate, Paralegal paralegal) {
		throw new InvalidConsultationStateException(consultation.getConsultationId(), consultation.getConsultationStatus(), ConsultationStatus.ACCEPTED_BY_PARTNER);
	}
	
	@Override
	public Consultation partnerAccept(Consultation consultation, ConsultationService consultationService, Associate associate, Paralegal paralegal) {
		consultation.setAssociate(associate);
		consultation.setParalegal(paralegal);
		consultation.setConsultationStatus(ConsultationStatus.ACCEPTED_BY_PARTNER);
		
		return consultationService.applyStatusChange(consultation);
	}
	
	@Override
	public Consultation partnerReject(Consultation consultation, ConsultationService consultationService, String rejectionReason) {
		consultation.setRejectionReason(rejectionReason);
		consultation.setConsultationStatus(ConsultationStatus.REJECTED_BY_PARTNER);
		
		return consultationService.applyStatusChange(consultation);
	}
	
	@Override
	public Consultation markScheduled(Consultation consultation, ConsultationService consultationService) {
		throw new InvalidConsultationStateException(consultation.getConsultationId(), consultation.getConsultationStatus(), ConsultationStatus.ACCEPTED_BY_PARTNER);
	}
	
	@Override
	public Consultation markUnscheduled(Consultation consultation, ConsultationService consultationService) {
		throw new InvalidConsultationStateException(consultation.getConsultationId(), consultation.getConsultationStatus(), ConsultationStatus.SCHEDULED);
	}
	
	@Override
	public Consultation associateAccept(Consultation consultation, ConsultationService consultationService) {
		throw new InvalidConsultationStateException(consultation.getConsultationId(), consultation.getConsultationStatus(), ConsultationStatus.SCHEDULED);
	}
	
	@Override
	public Consultation associateReject(Consultation consultation, ConsultationService consultationService, String rejectionReason) {
		throw new InvalidConsultationStateException(consultation.getConsultationId(), consultation.getConsultationStatus(), ConsultationStatus.SCHEDULED);
	}
	
	@Override
	public Case openCase(Consultation consultation, ConsultationService consultationService) {
		throw new InvalidConsultationStateException(consultation.getConsultationId(), consultation.getConsultationStatus(), ConsultationStatus.ACCEPTED_BY_ASSOCIATE);
	}
	
	@Override
	public void delete(Consultation consultation, ConsultationService consultationService) {
		consultationService.applyDelete(consultation);
	}
	
}
