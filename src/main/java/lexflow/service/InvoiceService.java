package lexflow.service;

import lexflow.dao.InvoiceDao;
import lexflow.entity.case_.Case;
import lexflow.entity.case_.Task;
import lexflow.entity.document.InvoiceDocument;
import lexflow.entity.finance.Invoice;
import lexflow.entity.finance.InvoiceStatus;
import lexflow.entity.finance.TransactionType;
import lexflow.entity.user.Client;
import lexflow.exception.finance.NoChargesSelectedException;
import lexflow.state.finance.InvoiceState;
import lexflow.state.finance.PaidInvoiceState;
import lexflow.state.finance.UnpaidInvoiceState;
import lexflow.state.finance.VoidedInvoiceState;
import lexflow.util.PdfUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.ITemplateEngine;
import org.thymeleaf.context.Context;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class InvoiceService {
	
	private final InvoiceDao invoiceDao;
	private final TaskService taskService;
	private final TransactionService transactionService;
	private final DocumentService documentService;
	private final ITemplateEngine templateEngine;
	
	// Create
	
	@Transactional
	public Invoice compile(Case case_, List<Long> taskIds) {
		
		if (taskIds == null || taskIds.isEmpty()) {
			throw new NoChargesSelectedException(case_.getCaseId());
		}
		
		List<Task> unbilledTasks = taskService.findUnbilledByCase(case_);
		BigDecimal total = BigDecimal.ZERO;
		List<Task> selectedTasks = new ArrayList<>();
		
		for (Task task : unbilledTasks) {
			
			if (taskIds.contains(task.getTaskId())) {
				total = total.add(task.getBillableAmount());
				selectedTasks.add(task);
			}
			
		}
		
		LocalDateTime compiledAt = LocalDateTime.now();
		Invoice invoice = new Invoice(case_.getCaseId(), total, compiledAt);
		invoiceDao.insert(invoice);
		
		taskService.linkToInvoice(selectedTasks, invoice.getInvoiceId());
		
		Context context = new Context();
		context.setVariable("invoice", invoice);
		context.setVariable("case", case_);
		context.setVariable("tasks", selectedTasks);
		
		byte[] pdfBytes = PdfUtil.render(templateEngine.process("pdf/invoice", context), invoice.getInvoiceCode());
		InvoiceDocument invoiceDocument = documentService.attach(case_, invoice, pdfBytes, compiledAt);
		invoice.setInvoiceDocument(invoiceDocument);
		
		return invoice;
	}
	
	// Read
	
	public Invoice findById(Long invoiceId) {
		return invoiceDao.findById(invoiceId);
	}
	
	public List<Invoice> findByCaseAndStatus(Case case_, InvoiceStatus invoiceStatus) {
		return invoiceDao.findByCaseIdAndStatus(case_.getCaseId(), invoiceStatus);
	}
	
	// Update
	
	@Transactional
	public Invoice charge(Case case_, Invoice invoice) {
		return stateFor(invoice.getInvoiceStatus()).charge(invoice, this, case_.getClient());
	}
	
	@Transactional
	public Invoice voidInvoice(Invoice invoice) {
		return stateFor(invoice.getInvoiceStatus()).voidInvoice(invoice, this);
	}
	
	@Transactional
	public Invoice voidAndRefund(Case case_, Invoice invoice) {
		return stateFor(invoice.getInvoiceStatus()).voidAndRefund(invoice, this, case_.getClient());
	}
	
	// State Pattern
	
	public Invoice applyPayment(Invoice invoice, Client client) {
		transactionService.record(client, invoice.getTotalAmount(), TransactionType.CHARGE, invoice.getInvoiceId());
		invoice.setInvoiceStatus(InvoiceStatus.PAID);
		invoice.setPaidAt(LocalDateTime.now());
		
		return invoiceDao.update(invoice);
	}
	
	public Invoice applyVoid(Invoice invoice) {
		releaseInvoiceCharges(invoice);
		invoice.setInvoiceStatus(InvoiceStatus.VOIDED);
		
		return invoiceDao.update(invoice);
	}
	
	public Invoice applyVoidAndRefund(Invoice invoice, Client client) {
		transactionService.record(client, invoice.getTotalAmount(), TransactionType.REFUND, invoice.getInvoiceId());
		releaseInvoiceCharges(invoice);
		invoice.setInvoiceStatus(InvoiceStatus.VOIDED);
		
		return invoiceDao.update(invoice);
	}
	
	private InvoiceState stateFor(InvoiceStatus invoiceStatus) {
		
		if (invoiceStatus == InvoiceStatus.UNPAID) {
			return new UnpaidInvoiceState();
		} else if (invoiceStatus == InvoiceStatus.PAID) {
			return new PaidInvoiceState();
		} else {
			return new VoidedInvoiceState();
		}
		
	}
	
	// Private Helpers
	
	private void releaseInvoiceCharges(Invoice invoice) {
		taskService.unlinkFromInvoice(invoice.getInvoiceId());
	}
	
}
