package lexflow.state.consultation;

import lexflow.entity.consultation.CaseType;
import lexflow.entity.consultation.Consultation;
import lexflow.entity.user.Associate;
import lexflow.entity.user.Paralegal;
import lexflow.service.ConsultationService;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ConsultationState {
	
	Consultation updateDetails(Consultation consultation, ConsultationService consultationService, CaseType caseType, List<String> opposingParties, String description, List<MultipartFile> consultationFiles, List<Long> documentIdsToDelete, List<Long> documentIds, List<String> originalFilenames);
	
	Consultation reassignStaff(Consultation consultation, ConsultationService consultationService, Associate associate, Paralegal paralegal);
	
	Consultation partnerAccept(Consultation consultation, ConsultationService consultationService, Associate associate, Paralegal paralegal);
	
	Consultation partnerReject(Consultation consultation, ConsultationService consultationService, String rejectionReason);
	
	Consultation markScheduled(Consultation consultation, ConsultationService consultationService);
	
	Consultation markUnscheduled(Consultation consultation, ConsultationService consultationService);
	
	Consultation associateAccept(Consultation consultation, ConsultationService consultationService);
	
	Consultation associateReject(Consultation consultation, ConsultationService consultationService, String rejectionReason);
	
	void delete(Consultation consultation, ConsultationService consultationService);
	
}
