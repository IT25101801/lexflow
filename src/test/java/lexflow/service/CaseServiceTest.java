package lexflow.service;

import lexflow.dao.CaseDao;
import lexflow.dao.ConsultationDao;
import lexflow.dao.InvoiceDao;
import lexflow.dao.TaskDao;
import lexflow.entity.case_.Case;
import lexflow.entity.case_.CaseResolutionStatus;
import lexflow.entity.case_.CaseStatus;
import lexflow.entity.case_.Task;
import lexflow.entity.consultation.Consultation;
import lexflow.entity.consultation.ConsultationStatus;
import lexflow.entity.finance.InvoiceStatus;
import lexflow.entity.user.Associate;
import lexflow.entity.user.Paralegal;
import lexflow.exception.case_.InvalidCaseStateException;
import lexflow.exception.case_.UnsettledBillingException;
import lexflow.observer.CaseEventsCancellationRequested;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CaseServiceTest {

	private CaseDao caseDao;
	private ConsultationDao consultationDao;
	private TaskDao taskDao;
	private InvoiceDao invoiceDao;
	private ApplicationEventPublisher eventPublisher;
	private CaseService caseService;
	private Case openCase;

	@BeforeEach
	void setUp() {
		caseDao = mock(CaseDao.class);
		consultationDao = mock(ConsultationDao.class);
		taskDao = mock(TaskDao.class);
		invoiceDao = mock(InvoiceDao.class);
		eventPublisher = mock(ApplicationEventPublisher.class);
		caseService = new CaseService(caseDao, consultationDao, taskDao, invoiceDao, eventPublisher);

		Consultation consultation = TestData.consultation(10L, ConsultationStatus.ACCEPTED_BY_ASSOCIATE, TestData.associate(2L), TestData.paralegal(3L));
		openCase = TestData.case_(100L, consultation, CaseStatus.OPEN);

		when(caseDao.update(any(Case.class))).thenAnswer(invocation -> invocation.getArgument(0));
		when(taskDao.findUnbilledByCaseId(100L)).thenReturn(List.of());
		when(invoiceDao.findByCaseIdAndStatus(100L, InvoiceStatus.UNPAID)).thenReturn(List.of());
	}

	private Task unbilledTask() {
		return new Task(1L, 100L, "Court fee", false, false, true, null, new BigDecimal("50.00"), null, LocalDateTime.now());
	}

	@Test
	void closeSucceedsWhenNothingIsUnbilledOrUnpaid() {
		Case closed = caseService.close(openCase, CaseResolutionStatus.WON);

		assertEquals(CaseStatus.CLOSED, closed.getCaseStatus());
		assertEquals(CaseResolutionStatus.WON, closed.getCaseResolutionStatus());
		assertNotNull(closed.getClosedAt());
		verify(eventPublisher).publishEvent(any(CaseEventsCancellationRequested.class));
	}

	@Test
	void closeIsBlockedByAnUnbilledBillingTask() {
		when(taskDao.findUnbilledByCaseId(100L)).thenReturn(List.of(unbilledTask()));

		UnsettledBillingException e = assertThrows(UnsettledBillingException.class, () -> caseService.close(openCase, CaseResolutionStatus.WON));

		assertEquals(1, e.getUnbilledTaskCount());
		assertEquals(0, e.getUnpaidInvoiceCount());
		assertEquals(CaseStatus.OPEN, openCase.getCaseStatus());
		verify(caseDao, never()).update(any(Case.class));
		verify(eventPublisher, never()).publishEvent(any(Object.class));
	}

	@Test
	void closeIsBlockedByAnUnpaidInvoice() {
		when(invoiceDao.findByCaseIdAndStatus(100L, InvoiceStatus.UNPAID)).thenReturn(List.of(TestData.invoice(5L, 100L, InvoiceStatus.UNPAID)));

		UnsettledBillingException e = assertThrows(UnsettledBillingException.class, () -> caseService.close(openCase, CaseResolutionStatus.LOST));

		assertEquals(0, e.getUnbilledTaskCount());
		assertEquals(1, e.getUnpaidInvoiceCount());
		assertEquals(CaseStatus.OPEN, openCase.getCaseStatus());
		assertNull(openCase.getClosedAt());
	}

	@Test
	void closeOnlyLooksAtUnpaidInvoicesSoVoidedAndPaidOnesDoNotBlock() {
		caseService.close(openCase, CaseResolutionStatus.SETTLED);

		verify(invoiceDao).findByCaseIdAndStatus(100L, InvoiceStatus.UNPAID);
		verify(invoiceDao, never()).findByCaseIdAndStatus(100L, InvoiceStatus.VOIDED);
		verify(invoiceDao, never()).findByCaseIdAndStatus(100L, InvoiceStatus.PAID);
	}

	@Test
	void closingAnAlreadyClosedCaseIsRefused() {
		Case closedCase = TestData.case_(101L, openCase.getConsultation(), CaseStatus.CLOSED);

		assertThrows(InvalidCaseStateException.class, () -> caseService.close(closedCase, CaseResolutionStatus.WON));
	}

	@Test
	void reassigningACaseAlsoMovesTheConsultationToTheNewStaffWithoutChangingItsStatus() {
		Associate newAssociate = TestData.associate(20L);
		Paralegal newParalegal = TestData.paralegal(30L);
		Consultation consultation = openCase.getConsultation();

		Case reassigned = caseService.reassignStaff(openCase, newAssociate, newParalegal);

		assertEquals(20L, reassigned.getAssociate().getUserId());
		assertEquals(30L, reassigned.getParalegal().getUserId());
		assertEquals(20L, consultation.getAssociate().getUserId());
		assertEquals(30L, consultation.getParalegal().getUserId());
		assertEquals(ConsultationStatus.ACCEPTED_BY_ASSOCIATE, consultation.getConsultationStatus());

		ArgumentCaptor<Consultation> captor = ArgumentCaptor.forClass(Consultation.class);
		verify(consultationDao).update(captor.capture());
		assertSame(consultation, captor.getValue());
		verify(eventPublisher).publishEvent(any(CaseEventsCancellationRequested.class));
	}

	@Test
	void reassigningACaseWithTheSameStaffCancelsNothing() {
		Case sameStaff = caseService.reassignStaff(openCase, openCase.getAssociate(), openCase.getParalegal());

		assertSame(openCase, sameStaff);
		verify(eventPublisher, never()).publishEvent(any(Object.class));
	}

	@Test
	void reassigningAClosedCaseIsRefused() {
		Case closedCase = TestData.case_(101L, openCase.getConsultation(), CaseStatus.CLOSED);

		assertThrows(InvalidCaseStateException.class, () -> caseService.reassignStaff(closedCase, TestData.associate(20L), TestData.paralegal(30L)));
		verify(consultationDao, never()).update(any(Consultation.class));
	}

}
