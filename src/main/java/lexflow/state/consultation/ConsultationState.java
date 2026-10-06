package lexflow.state.consultation;

import lexflow.entity.consultation.CaseType;
import lexflow.entity.consultation.Consultation;
import lexflow.entity.consultation.ConsultationStatus;
import lexflow.entity.user.Associate;
import lexflow.entity.user.Paralegal;
import lexflow.exception.consultation.InvalidConsultationStateException;
import lexflow.service.ConsultationService;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ConsultationState {
	
	default Consultation updateDetails(Consultation consultation, ConsultationService consultationService, CaseType caseType, List<String> opposingParties, String description, List<MultipartFile> consultationFiles, List<Long> documentIdsToDelete, List<Long> documentIds, List<String> originalFilenames) {
		throw new InvalidConsultationStateException(consultation.getConsultationId(), consultation.getConsultationStatus(), ConsultationStatus.PENDING);
	}
	
	default Consultation reassignStaff(Consultation consultation, ConsultationService consultationService, Associate associate, Paralegal paralegal) {
		throw new InvalidConsultationStateException(consultation.getConsultationId(), consultation.getConsultationStatus(), ConsultationStatus.ACCEPTED_BY_PARTNER);
	}
	
	default Consultation partnerAccept(Consultation consultation, ConsultationService consultationService, Associate associate, Paralegal paralegal) {
		throw new InvalidConsultationStateException(consultation.getConsultationId(), consultation.getConsultationStatus(), ConsultationStatus.PENDING);
	}
	
	default Consultation partnerReject(Consultation consultation, ConsultationService consultationService, String rejectionReason) {
		throw new InvalidConsultationStateException(consultation.getConsultationId(), consultation.getConsultationStatus(), ConsultationStatus.PENDING);
	}
	
	default Consultation markScheduled(Consultation consultation, ConsultationService consultationService) {
		throw new InvalidConsultationStateException(consultation.getConsultationId(), consultation.getConsultationStatus(), ConsultationStatus.ACCEPTED_BY_PARTNER);
	}
	
	default Consultation markUnscheduled(Consultation consultation, ConsultationService consultationService) {
		throw new InvalidConsultationStateException(consultation.getConsultationId(), consultation.getConsultationStatus(), ConsultationStatus.SCHEDULED);
	}

	default Consultation associateAccept(Consultation consultation, ConsultationService consultationService) {
		throw new InvalidConsultationStateException(consultation.getConsultationId(), consultation.getConsultationStatus(), ConsultationStatus.SCHEDULED);
	}
	
	default Consultation associateReject(Consultation consultation, ConsultationService consultationService, String rejectionReason) {
		throw new InvalidConsultationStateException(consultation.getConsultationId(), consultation.getConsultationStatus(), ConsultationStatus.SCHEDULED);
	}
	
	default void delete(Consultation consultation, ConsultationService consultationService) {
		throw new InvalidConsultationStateException(consultation.getConsultationId(), consultation.getConsultationStatus(), ConsultationStatus.PENDING);
	}
	
}
