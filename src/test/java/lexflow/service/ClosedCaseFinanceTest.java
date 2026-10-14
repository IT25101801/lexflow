package lexflow.service;

import lexflow.dao.CaseDao;
import lexflow.dao.ConsultationDao;
import lexflow.dao.InvoiceDao;
import lexflow.dao.TaskDao;
import lexflow.entity.case_.Case;
import lexflow.entity.case_.CaseStatus;
import lexflow.entity.case_.Task;
import lexflow.entity.consultation.Consultation;
import lexflow.entity.consultation.ConsultationStatus;
import lexflow.entity.finance.InvoiceStatus;
import lexflow.exception.case_.InvalidCaseStateException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.thymeleaf.ITemplateEngine;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ClosedCaseFinanceTest {

	private InvoiceDao invoiceDao;
	private TaskDao taskDao;
	private TransactionService transactionService;
	private InvoiceService invoiceService;
	private TaskService taskService;
	private Case closedCase;
	private Case openCase;

	@BeforeEach
	void setUp() {
		invoiceDao = mock(InvoiceDao.class);
		taskDao = mock(TaskDao.class);
		transactionService = mock(TransactionService.class);
		DocumentService documentService = mock(DocumentService.class);
		CaseService caseService = new CaseService(mock(CaseDao.class), mock(ConsultationDao.class), taskDao, invoiceDao, mock(ApplicationEventPublisher.class));
		taskService = new TaskService(taskDao, caseService, documentService, mock(EventService.class));
		invoiceService = new InvoiceService(invoiceDao, mock(CaseDao.class), caseService, taskService, transactionService, documentService, mock(ITemplateEngine.class));

		Consultation consultation = TestData.consultation(10L, ConsultationStatus.ACCEPTED_BY_ASSOCIATE, TestData.associate(2L), TestData.paralegal(3L));
		closedCase = TestData.case_(100L, consultation, CaseStatus.CLOSED);
		openCase = TestData.case_(101L, consultation, CaseStatus.OPEN);
	}

	@Test
	void compileIsRefusedOnAClosedCase() {
		assertThrows(InvalidCaseStateException.class, () -> invoiceService.compile(closedCase, List.of(1L)));
		verify(invoiceDao, never()).insert(any());
	}

	@Test
	void chargeIsRefusedOnAClosedCase() {
		assertThrows(InvalidCaseStateException.class, () -> invoiceService.charge(closedCase, TestData.invoice(5L, 100L, InvoiceStatus.UNPAID)));
		verify(transactionService, never()).record(any(), any(), any(), any());
	}

	@Test
	void voidIsRefusedOnAClosedCase() {
		assertThrows(InvalidCaseStateException.class, () -> invoiceService.void_(closedCase, TestData.invoice(5L, 100L, InvoiceStatus.UNPAID)));
		verify(taskDao, never()).update(any());
	}

	@Test
	void voidAndRefundIsRefusedOnAClosedCase() {
		assertThrows(InvalidCaseStateException.class, () -> invoiceService.voidAndRefund(closedCase, TestData.invoice(5L, 100L, InvoiceStatus.PAID)));
		verify(transactionService, never()).record(any(), any(), any(), any());
	}

	@Test
	void editingABillableAmountIsRefusedOnAClosedCase() {
		Task task = new Task(1L, 100L, "Court fee", false, false, true, null, new BigDecimal("50.00"), null, LocalDateTime.now());

		assertThrows(InvalidCaseStateException.class, () -> taskService.updateBillableAmount(closedCase, task, new BigDecimal("60.00")));
		verify(taskDao, never()).update(any());
	}

	@Test
	void editingABillableAmountStillWorksOnAnOpenCase() {
		Task task = new Task(1L, 101L, "Court fee", false, false, true, null, new BigDecimal("50.00"), null, LocalDateTime.now());
		when(taskDao.update(task)).thenReturn(task);

		taskService.updateBillableAmount(openCase, task, new BigDecimal("60.00"));

		verify(taskDao).update(task);
		org.junit.jupiter.api.Assertions.assertEquals(new BigDecimal("60.00"), task.getBillableAmount());
	}

	@Test
	void chargeOnAnOpenCaseStillGoesThroughTheInvoiceState() {
		lexflow.entity.finance.Invoice invoice = TestData.invoice(5L, 101L, InvoiceStatus.UNPAID);
		when(invoiceDao.update(invoice)).thenReturn(invoice);

		try {
			invoiceService.charge(openCase, invoice);
		} catch (RuntimeException e) {
			org.junit.jupiter.api.Assertions.assertFalse(e instanceof InvalidCaseStateException);
		}

		verify(transactionService).record(any(), any(), any(), any());
	}

}
