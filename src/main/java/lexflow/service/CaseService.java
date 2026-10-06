package lexflow.service;

import lexflow.dao.CaseDao;
import lexflow.entity.case_.Case;
import lexflow.entity.case_.CaseResolutionStatus;
import lexflow.entity.case_.CaseStatus;
import lexflow.entity.consultation.Consultation;
import lexflow.entity.user.Associate;
import lexflow.entity.user.Client;
import lexflow.entity.user.Paralegal;
import lexflow.observer.CaseEventsCancellationRequested;
import lexflow.state.case_.CaseState;
import lexflow.state.case_.ClosedCaseState;
import lexflow.state.case_.OpenCaseState;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CaseService {
	
	private final CaseDao caseDao;
	
	private final ApplicationEventPublisher eventPublisher;
	
	// Create
	
	@Transactional
	public Case open(Consultation consultation) {
		Case case_ = new Case(consultation, consultation.getClient(), consultation.getAssociate(), consultation.getParalegal(), LocalDateTime.now());
		
		return caseDao.insert(case_);
	}
	
	// Read
	
	public Case findById(Long caseId) {
		return caseDao.findById(caseId);
	}
	
	public Case findByConsultation(Consultation consultation) {
		return caseDao.findByConsultationId(consultation.getConsultationId());
	}
	
	public Case findOwnedByAssociate(Long caseId, Associate associate) {
		Case case_ = findById(caseId);
		
		if (case_.getAssociate() == null || !case_.getAssociate().getUserId().equals(associate.getUserId())) {
			throw new AccessDeniedException("Not assigned to you.");
		}
		
		return case_;
	}
	
	public Case findOwnedByParalegal(Long caseId, Paralegal paralegal) {
		Case case_ = findById(caseId);
		
		if (case_.getParalegal() == null || !case_.getParalegal().getUserId().equals(paralegal.getUserId())) {
			throw new AccessDeniedException("Not assigned to you.");
		}
		
		return case_;
	}
	
	public Case findOwnedByClient(Long caseId, Client client) {
		Case case_ = findById(caseId);
		
		if (!case_.getClient().getUserId().equals(client.getUserId())) {
			throw new AccessDeniedException("Not your case.");
		}
		
		return case_;
	}
	
	public List<Case> findAllForAssociate(Associate associate) {
		return caseDao.findByAssociateId(associate.getUserId());
	}
	
	public List<Case> findAllForParalegal(Paralegal paralegal) {
		return caseDao.findByParalegalId(paralegal.getUserId());
	}
	
	public List<Case> findAllForClient(Client client) {
		return caseDao.findByClientId(client.getUserId());
	}
	
	public List<Case> findAllOpen() {
		return caseDao.findByStatus(CaseStatus.OPEN);
	}
	
	public List<Case> findAllClosed() {
		return caseDao.findByStatus(CaseStatus.CLOSED);
	}
	
	public List<Case> findOpenForAssociate(Associate associate) {
		return caseDao.findByAssociateIdAndStatus(associate.getUserId(), CaseStatus.OPEN);
	}
	
	public List<Case> findOpenForParalegal(Paralegal paralegal) {
		return caseDao.findByParalegalIdAndStatus(paralegal.getUserId(), CaseStatus.OPEN);
	}
	
	public List<Case> findOpenForClient(Client client) {
		return caseDao.findByClientIdAndStatus(client.getUserId(), CaseStatus.OPEN);
	}
	
	public List<Case> findClosedForAssociate(Associate associate) {
		return caseDao.findByAssociateIdAndStatus(associate.getUserId(), CaseStatus.CLOSED);
	}
	
	public List<Case> findClosedForParalegal(Paralegal paralegal) {
		return caseDao.findByParalegalIdAndStatus(paralegal.getUserId(), CaseStatus.CLOSED);
	}
	
	public List<Case> findClosedForClient(Client client) {
		return caseDao.findByClientIdAndStatus(client.getUserId(), CaseStatus.CLOSED);
	}
	
	// Update
	
	@Transactional
	public Case reassignStaff(Case case_, Associate associate, Paralegal paralegal) {
		return stateFor(case_.getCaseStatus()).reassignStaff(case_, this, associate, paralegal);
	}
	
	@Transactional
	public Case close(Case case_, CaseResolutionStatus caseResolutionStatus) {
		return stateFor(case_.getCaseStatus()).close(case_, this, caseResolutionStatus);
	}
	
	// State Pattern
	
	public Case applyReassignment(Case case_, Associate associate, Paralegal paralegal, boolean cancelScheduledEvents) {
		
		if (cancelScheduledEvents) {
			eventPublisher.publishEvent(new CaseEventsCancellationRequested(case_.getCaseId()));
		}
		
		case_.setAssociate(associate);
		case_.setParalegal(paralegal);
		
		return caseDao.update(case_);
	}
	
	public Case applyClose(Case case_, CaseResolutionStatus caseResolutionStatus) {
		eventPublisher.publishEvent(new CaseEventsCancellationRequested(case_.getCaseId()));
		
		case_.setCaseStatus(CaseStatus.CLOSED);
		case_.setCaseResolutionStatus(caseResolutionStatus);
		case_.setClosedAt(LocalDateTime.now());
		
		return caseDao.update(case_);
	}
	
	private CaseState stateFor(CaseStatus caseStatus) {
		
		if (caseStatus == CaseStatus.OPEN) {
			return new OpenCaseState();
		} else {
			return new ClosedCaseState();
		}
		
	}
	
}
