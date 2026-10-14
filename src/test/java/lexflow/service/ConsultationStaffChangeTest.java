package lexflow.service;

import lexflow.dao.CaseDao;
import lexflow.dao.ConsultationDao;
import lexflow.dao.EventDao;
import lexflow.entity.case_.Case;
import lexflow.entity.case_.CaseStatus;
import lexflow.entity.consultation.Consultation;
import lexflow.entity.consultation.ConsultationStatus;
import lexflow.entity.scheduling.EventStatus;
import lexflow.entity.user.Associate;
import lexflow.entity.user.Paralegal;
import lexflow.exception.consultation.ConsultationHasCaseException;
import lexflow.exception.consultation.InvalidConsultationStateException;
import lexflow.exception.consultation.MeetingNotCompletedException;
import lexflow.observer.ConsultationEventsCancellationRequested;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ConsultationStaffChangeTest {
	
	private ConsultationDao consultationDao;
	private CaseDao caseDao;
	private EventDao eventDao;
	private ApplicationEventPublisher eventPublisher;
	private ConsultationService consultationService;
	private Associate associate;
	private Paralegal paralegal;
	private Associate otherAssociate;
	private Paralegal otherParalegal;
	
	@BeforeEach
	void setUp() {
		consultationDao = mock(ConsultationDao.class);
		caseDao = mock(CaseDao.class);
		eventDao = mock(EventDao.class);
		eventPublisher = mock(ApplicationEventPublisher.class);
		consultationService = new ConsultationService(consultationDao, caseDao, eventDao, eventPublisher, mock(DocumentService.class));
		associate = TestData.associate(2L);
		paralegal = TestData.paralegal(3L);
		otherAssociate = TestData.associate(20L);
		otherParalegal = TestData.paralegal(30L);
		
		when(consultationDao.update(any(Consultation.class))).thenAnswer(invocation -> invocation.getArgument(0));
	}
	
	private Consultation consultation(ConsultationStatus consultationStatus, EventStatus... meetingStatuses) {
		Consultation consultation = TestData.consultation(10L, consultationStatus, associate, paralegal);
		List<lexflow.entity.scheduling.ConsultationEvent> meetings = new java.util.ArrayList<>();
		
		for (int i = 0; i < meetingStatuses.length; i++) {
			meetings.add(TestData.meeting(i + 1L, consultation, meetingStatuses[i]));
		}
		
		when(eventDao.findByConsultationId(10L)).thenReturn(meetings);
		
		return consultation;
	}
	
	@Test
	void acceptedByPartnerCanBeReassignedFreelyWithoutChangingStatus() {
		Consultation consultation = consultation(ConsultationStatus.ACCEPTED_BY_PARTNER);
		
		consultationService.reassignStaff(consultation, otherAssociate, otherParalegal);
		
		assertEquals(ConsultationStatus.ACCEPTED_BY_PARTNER, consultation.getConsultationStatus());
		assertEquals(20L, consultation.getAssociate().getUserId());
		assertEquals(30L, consultation.getParalegal().getUserId());
		verify(eventPublisher, never()).publishEvent(any(Object.class));
	}
	
	@Test
	void bookedMeetingIsCanceledAndStatusResetWhenTheAssociateChanges() {
		Consultation consultation = consultation(ConsultationStatus.SCHEDULED, EventStatus.SCHEDULED);
		
		consultationService.reassignStaff(consultation, otherAssociate, paralegal);
		
		assertEquals(ConsultationStatus.ACCEPTED_BY_PARTNER, consultation.getConsultationStatus());
		verify(eventPublisher).publishEvent(any(ConsultationEventsCancellationRequested.class));
	}
	
	@Test
	void bookedMeetingIsCanceledAndStatusResetWhenOnlyTheParalegalChanges() {
		Consultation consultation = consultation(ConsultationStatus.SCHEDULED, EventStatus.SCHEDULED);
		
		consultationService.reassignStaff(consultation, associate, otherParalegal);
		
		assertEquals(ConsultationStatus.ACCEPTED_BY_PARTNER, consultation.getConsultationStatus());
		assertEquals(30L, consultation.getParalegal().getUserId());
		verify(eventPublisher).publishEvent(any(ConsultationEventsCancellationRequested.class));
	}
	
	@Test
	void heldMeetingWithANewAssociateSendsTheConsultationBackToAcceptedByPartner() {
		Consultation consultation = consultation(ConsultationStatus.SCHEDULED, EventStatus.COMPLETED);
		
		consultationService.reassignStaff(consultation, otherAssociate, paralegal);
		
		assertEquals(ConsultationStatus.ACCEPTED_BY_PARTNER, consultation.getConsultationStatus());
		assertEquals(20L, consultation.getAssociate().getUserId());
	}
	
	@Test
	void heldMeetingStaysValidWhenOnlyTheParalegalChanges() {
		Consultation consultation = consultation(ConsultationStatus.SCHEDULED, EventStatus.COMPLETED);
		
		consultationService.reassignStaff(consultation, associate, otherParalegal);
		
		assertEquals(ConsultationStatus.SCHEDULED, consultation.getConsultationStatus());
		assertEquals(30L, consultation.getParalegal().getUserId());
		assertEquals(2L, consultation.getAssociate().getUserId());
		verify(eventPublisher, never()).publishEvent(any(Object.class));
		assertDoesNotThrow(() -> consultationService.associateAccept(consultation));
		assertEquals(ConsultationStatus.ACCEPTED_BY_ASSOCIATE, consultation.getConsultationStatus());
	}
	
	@Test
	void acceptedByAssociateGoesBackToAcceptedByPartnerWhenTheAssociateChanges() {
		Consultation consultation = consultation(ConsultationStatus.ACCEPTED_BY_ASSOCIATE, EventStatus.COMPLETED);
		
		consultationService.reassignStaff(consultation, otherAssociate, paralegal);
		
		assertEquals(ConsultationStatus.ACCEPTED_BY_PARTNER, consultation.getConsultationStatus());
	}
	
	@Test
	void acceptedByAssociateKeepsItsStatusWhenOnlyTheParalegalChanges() {
		Consultation consultation = consultation(ConsultationStatus.ACCEPTED_BY_ASSOCIATE, EventStatus.COMPLETED);
		
		consultationService.reassignStaff(consultation, associate, otherParalegal);
		
		assertEquals(ConsultationStatus.ACCEPTED_BY_ASSOCIATE, consultation.getConsultationStatus());
		assertEquals(30L, consultation.getParalegal().getUserId());
	}
	
	@Test
	void pendingAndRejectedConsultationsCannotBeReassigned() {
		for (ConsultationStatus blocked : new ConsultationStatus[] {ConsultationStatus.PENDING, ConsultationStatus.REJECTED_BY_PARTNER, ConsultationStatus.REJECTED_BY_ASSOCIATE}) {
			Consultation consultation = consultation(blocked);
			
			assertThrows(InvalidConsultationStateException.class, () -> consultationService.reassignStaff(consultation, otherAssociate, otherParalegal), blocked.name());
		}
		
	}
	
	@Test
	void aConsultationThatAlreadyHasACaseCannotBeReassigned() {
		Consultation consultation = consultation(ConsultationStatus.ACCEPTED_BY_ASSOCIATE, EventStatus.COMPLETED);
		Case case_ = TestData.case_(100L, consultation, CaseStatus.OPEN);
		when(caseDao.findByConsultationId(10L)).thenReturn(case_);
		
		assertThrows(ConsultationHasCaseException.class, () -> consultationService.reassignStaff(consultation, otherAssociate, otherParalegal));
		
		assertEquals(ConsultationStatus.ACCEPTED_BY_ASSOCIATE, consultation.getConsultationStatus());
		assertEquals(2L, consultation.getAssociate().getUserId());
		verify(consultationDao, never()).update(any(Consultation.class));
	}
	
	@Test
	void hasCompletedMeetingIsTrueOnlyWhenAMeetingWasHeldAndNoneIsWaiting() {
		assertFalse(consultationService.hasCompletedMeeting(consultation(ConsultationStatus.SCHEDULED)));
		assertFalse(consultationService.hasCompletedMeeting(consultation(ConsultationStatus.SCHEDULED, EventStatus.SCHEDULED)));
		assertFalse(consultationService.hasCompletedMeeting(consultation(ConsultationStatus.SCHEDULED, EventStatus.CANCELED)));
		assertTrue(consultationService.hasCompletedMeeting(consultation(ConsultationStatus.SCHEDULED, EventStatus.COMPLETED)));
		assertTrue(consultationService.hasCompletedMeeting(consultation(ConsultationStatus.SCHEDULED, EventStatus.CANCELED, EventStatus.COMPLETED)));
	}
	
	@Test
	void aNewAssociateMustHoldANewMeetingEvenThoughAnOldOneWasCompleted() {
		Consultation consultation = consultation(ConsultationStatus.SCHEDULED, EventStatus.COMPLETED, EventStatus.SCHEDULED);
		
		assertThrows(MeetingNotCompletedException.class, () -> consultationService.associateAccept(consultation));
		assertThrows(MeetingNotCompletedException.class, () -> consultationService.associateReject(consultation, "No grounds"));
		assertEquals(ConsultationStatus.SCHEDULED, consultation.getConsultationStatus());
	}
	
	@Test
	void afterTheNewMeetingIsCompletedTheNewAssociateCanDecide() {
		Consultation consultation = consultation(ConsultationStatus.SCHEDULED, EventStatus.COMPLETED, EventStatus.COMPLETED);
		
		consultationService.associateAccept(consultation);
		
		assertEquals(ConsultationStatus.ACCEPTED_BY_ASSOCIATE, consultation.getConsultationStatus());
	}
	
	@Test
	void accessFollowsTheConsultationsOwnStaff() {
		Consultation consultation = consultation(ConsultationStatus.ACCEPTED_BY_PARTNER);
		when(consultationDao.findById(10L)).thenReturn(consultation);
		
		assertSame(consultation, consultationService.findOwnedByAssociate(10L, associate));
		assertSame(consultation, consultationService.findOwnedByParalegal(10L, paralegal));
		assertThrows(AccessDeniedException.class, () -> consultationService.findOwnedByAssociate(10L, otherAssociate));
		assertThrows(AccessDeniedException.class, () -> consultationService.findOwnedByParalegal(10L, otherParalegal));
		verify(caseDao, never()).findByConsultationId(any());
	}
	
}
