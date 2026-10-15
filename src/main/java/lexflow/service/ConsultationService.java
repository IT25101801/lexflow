package lexflow.service;

import lexflow.dao.CaseDao;
import lexflow.dao.ConsultationDao;
import lexflow.dao.EventDao;
import lexflow.entity.case_.Case;
import lexflow.entity.consultation.CaseType;
import lexflow.entity.consultation.Consultation;
import lexflow.entity.consultation.ConsultationStatus;
import lexflow.entity.scheduling.ConsultationEvent;
import lexflow.entity.scheduling.EventStatus;
import lexflow.entity.user.Associate;
import lexflow.entity.user.Client;
import lexflow.entity.user.Paralegal;
import lexflow.exception.consultation.ConsultationHasCaseException;
import lexflow.observer.ConsultationEventsCancellationRequested;
import lexflow.state.consultation.*;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ConsultationService {
	
	private final ConsultationDao consultationDao;
	private final CaseDao caseDao;
	private final EventDao eventDao;
	private final ApplicationEventPublisher eventPublisher;
	private final DocumentService documentService;
	
	// Create
	
	@Transactional
	public Consultation request(Client client, CaseType caseType, List<String> opposingParties, String description, List<MultipartFile> consultationFiles) {
		LocalDateTime requestedAt = LocalDateTime.now();
		Consultation consultation = new Consultation(client, caseType, opposingParties, description, requestedAt);
		
		consultationDao.insert(consultation);
		documentService.attach(consultation, consultationFiles, requestedAt);
		
		return consultation;
	}
	
	// Read
	
	public Consultation findById(Long consultationId) {
		return consultationDao.findById(consultationId);
	}
	
	public Consultation findOwnedByAssociate(Long consultationId, Associate associate) {
		Consultation consultation = findById(consultationId);
		
		if (consultation.getAssociate() == null || !consultation.getAssociate().getUserId().equals(associate.getUserId())) {
			throw new AccessDeniedException("Not assigned to you.");
		}
		
		return consultation;
	}
	
	public Consultation findOwnedByParalegal(Long consultationId, Paralegal paralegal) {
		Consultation consultation = findById(consultationId);
		
		if (consultation.getParalegal() == null || !consultation.getParalegal().getUserId().equals(paralegal.getUserId())) {
			throw new AccessDeniedException("Not assigned to you.");
		}
		
		return consultation;
	}
	
	public Consultation findOwnedByClient(Long consultationId, Client client) {
		Consultation consultation = findById(consultationId);
		Case case_ = caseDao.findByConsultationId(consultationId);
		Client effectiveClient = case_ != null ? case_.getClient() : consultation.getClient();
		
		if (!effectiveClient.getUserId().equals(client.getUserId())) {
			throw new AccessDeniedException("Not your consultation.");
		}
		
		return consultation;
	}
	
	public List<Consultation> findAll() {
		return consultationDao.findAll();
	}
	
	public List<Consultation> findAllForAssociate(Associate associate) {
		return consultationDao.findByAssociateId(associate.getUserId());
	}
	
	public List<Consultation> findAllForParalegal(Paralegal paralegal) {
		return consultationDao.findByParalegalId(paralegal.getUserId());
	}
	
	public List<Consultation> findAllForClient(Client client) {
		return consultationDao.findByClientId(client.getUserId());
	}
	
	public List<Consultation> findAwaitingReviewForPartner() {
		return consultationDao.findByStatus(ConsultationStatus.PENDING);
	}
	
	public List<Consultation> findAwaitingReviewForAssociate(Associate associate) {
		return consultationDao.findByAssociateIdAndStatus(associate.getUserId(), ConsultationStatus.ACCEPTED_BY_PARTNER);
	}
	
	public List<Consultation> findAwaitingReviewForParalegal(Paralegal paralegal) {
		return consultationDao.findByParalegalIdAndStatus(paralegal.getUserId(), ConsultationStatus.ACCEPTED_BY_PARTNER);
	}
	
	public List<Consultation> findByKeyword(String keyword) {
		
		if (keyword == null || keyword.isBlank()) {
			return findAll();
		}
		
		return consultationDao.findByClientKeyword(keyword);
	}
	
	public List<Consultation> findAwaitingCase() {
		return withoutCase(findAll());
	}
	
	public List<Consultation> findAwaitingCaseForAssociate(Associate associate) {
		return withoutCase(findAllForAssociate(associate));
	}
	
	public List<Consultation> findAwaitingCaseForParalegal(Paralegal paralegal) {
		return withoutCase(findAllForParalegal(paralegal));
	}
	
	public boolean hasCompletedMeeting(Consultation consultation) {
		List<ConsultationEvent> consultationEvents = eventDao.findByConsultationId(consultation.getConsultationId());
		boolean completed = false;
		
		for (ConsultationEvent consultationEvent : consultationEvents) {
			
			if (consultationEvent.getEventStatus() == EventStatus.SCHEDULED) {
				return false;
			}
			
			if (consultationEvent.getEventStatus() == EventStatus.COMPLETED) {
				completed = true;
			}
			
		}
		
		return completed;
	}
	
	// Update
	
	@Transactional
	public Consultation updateDetails(Consultation consultation, CaseType caseType, List<String> opposingParties, String description, List<MultipartFile> consultationFiles, List<Long> documentIdsToDelete, List<Long> documentIds, List<String> originalFilenames) {
		return stateFor(consultation.getConsultationStatus()).updateDetails(consultation, this, caseType, opposingParties, description, consultationFiles, documentIdsToDelete, documentIds, originalFilenames);
	}
	
	@Transactional
	public Consultation reassignStaff(Consultation consultation, Associate associate, Paralegal paralegal) {
		Case case_ = caseDao.findByConsultationId(consultation.getConsultationId());
		
		if (case_ != null) {
			throw new ConsultationHasCaseException(consultation.getConsultationId(), case_.getCaseId());
		}
		
		return stateFor(consultation.getConsultationStatus()).reassignStaff(consultation, this, associate, paralegal);
	}
	
	@Transactional
	public Consultation partnerAccept(Consultation consultation, Associate associate, Paralegal paralegal) {
		return stateFor(consultation.getConsultationStatus()).partnerAccept(consultation, this, associate, paralegal);
	}
	
	@Transactional
	public Consultation partnerReject(Consultation consultation, String rejectionReason) {
		return stateFor(consultation.getConsultationStatus()).partnerReject(consultation, this, rejectionReason);
	}
	
	@Transactional
	public Consultation markScheduled(Consultation consultation) {
		return stateFor(consultation.getConsultationStatus()).markScheduled(consultation, this);
	}
	
	@Transactional
	public Consultation markUnscheduled(Consultation consultation) {
		return stateFor(consultation.getConsultationStatus()).markUnscheduled(consultation, this);
	}
	
	@Transactional
	public Consultation associateAccept(Consultation consultation) {
		return stateFor(consultation.getConsultationStatus()).associateAccept(consultation, this);
	}
	
	@Transactional
	public Consultation associateReject(Consultation consultation, String rejectionReason) {
		return stateFor(consultation.getConsultationStatus()).associateReject(consultation, this, rejectionReason);
	}
	
	// Delete
	
	@Transactional
	public void delete(Consultation consultation) {
		stateFor(consultation.getConsultationStatus()).delete(consultation, this);
	}
	
	// State Pattern
	
	public Consultation applyDetails(Consultation consultation, CaseType caseType, List<String> opposingParties, String description, List<MultipartFile> consultationFiles, List<Long> documentIdsToDelete, List<Long> documentIds, List<String> originalFilenames) {
		consultation.setCaseType(caseType);
		consultation.setOpposingParties(opposingParties);
		consultation.setDescription(description);
		
		documentService.attach(consultation, consultationFiles, LocalDateTime.now());
		documentService.delete(consultation, documentIdsToDelete);
		
		if (documentIds != null) {
			
			for (int i = 0; i < documentIds.size(); i++) {
				Long documentId = documentIds.get(i);
				
				if (documentIdsToDelete != null && documentIdsToDelete.contains(documentId)) {
					continue;
				}
				
				documentService.rename(consultation, documentId, originalFilenames.get(i));
			}
			
		}
		
		return consultationDao.update(consultation);
	}
	
	public Consultation applyReassignment(Consultation consultation, Associate associate, Paralegal paralegal, boolean cancelScheduledEvents) {
		
		if (cancelScheduledEvents) {
			eventPublisher.publishEvent(new ConsultationEventsCancellationRequested(consultation.getConsultationId()));
		}
		
		consultation.setAssociate(associate);
		consultation.setParalegal(paralegal);
		
		return consultationDao.update(consultation);
	}
	
	public Consultation applyStatusChange(Consultation consultation) {
		return consultationDao.update(consultation);
	}
	
	public void applyDelete(Consultation consultation) {
		documentService.deleteAll(consultation);
		consultationDao.deleteById(consultation.getConsultationId());
	}
	
	private ConsultationState stateFor(ConsultationStatus consultationStatus) {
		
		if (consultationStatus == ConsultationStatus.PENDING) {
			return new PendingConsultationState();
		} else if (consultationStatus == ConsultationStatus.ACCEPTED_BY_PARTNER) {
			return new AcceptedByPartnerConsultationState();
		} else if (consultationStatus == ConsultationStatus.SCHEDULED) {
			return new ScheduledConsultationState();
		} else if (consultationStatus == ConsultationStatus.ACCEPTED_BY_ASSOCIATE) {
			return new AcceptedByAssociateConsultationState();
		} else if (consultationStatus == ConsultationStatus.REJECTED_BY_PARTNER) {
			return new RejectedByPartnerConsultationState();
		} else {
			return new RejectedByAssociateConsultationState();
		}
		
	}
	
	private List<Consultation> withoutCase(List<Consultation> consultations) {
		List<Consultation> awaitingCase = new ArrayList<>();
		
		for (Consultation consultation : consultations) {
			
			if (consultation.getConsultationStatus() == ConsultationStatus.ACCEPTED_BY_ASSOCIATE && caseDao.findByConsultationId(consultation.getConsultationId()) == null) {
				awaitingCase.add(consultation);
			}
			
		}
		
		return awaitingCase;
	}
	
}
