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
	
	// ---- Create ----
	
	@Transactional
	public Invoice compile(Case case_, List<Long> taskIds) {
		
		if (taskIds == null || taskIds.isEmpty()) {
			throw new NoChargesSelectedException(case_.getCaseId());
		}
		
		List<Task> unbilled = taskService.findUnbilledByCase(case_);
		BigDecimal total = BigDecimal.ZERO;
		List<Task> toLink = new ArrayList<>();
		
		for (Task task : unbilled) {
			
			if (taskIds.contains(task.getTaskId())) {
				total = total.add(task.getBillableAmount());
				toLink.add(task);
			}
			
		}
		
		LocalDateTime compiledAt = LocalDateTime.now();
		Invoice invoice = new Invoice(case_.getCaseId(), total, compiledAt);
		invoiceDao.insert(invoice);
		
		taskService.linkToInvoice(toLink, invoice.getInvoiceId());
		
		Context context = new Context();
		context.setVariable("invoice", invoice);
		context.setVariable("case", case_);
		context.setVariable("tasks", toLink);
		
		String html = templateEngine.process("pdf/invoice", context);
		byte[] pdfBytes = PdfUtil.render(html, invoice.getInvoiceCode());
		
		InvoiceDocument invoiceDocument = documentService.attachInvoiceDocument(case_, invoice, pdfBytes, compiledAt);
		invoice.setInvoiceDocument(invoiceDocument);
		
		return invoice;
	}
	
	// ---- Read ----
	
	public Invoice findById(Long invoiceId) {
		return invoiceDao.findById(invoiceId);
	}
	
	public List<Invoice> findByCaseAndStatus(Case case_, InvoiceStatus invoiceStatus) {
		return invoiceDao.findByCaseIdAndStatus(case_.getCaseId(), invoiceStatus);
	}
	
	// ---- Update ----
	
	@Transactional
	public Invoice chargeInvoice(Case case_, Invoice invoice) {
		return stateFor(invoice.getInvoiceStatus()).charge(invoice, this, case_.getClient());
	}
	
	@Transactional
	public Invoice voidInvoice(Invoice invoice) {
		return stateFor(invoice.getInvoiceStatus()).voidInvoice(invoice, this);
	}
	
	@Transactional
	public Invoice voidAndRefundInvoice(Case case_, Invoice invoice) {
		return stateFor(invoice.getInvoiceStatus()).voidAndRefund(invoice, this, case_.getClient());
	}
	
	// ---- State Transitions (called by InvoiceState implementations) ----
	
	public Invoice markPaid(Invoice invoice, Client client) {
		transactionService.record(client, invoice.getTotalAmount(), TransactionType.CHARGE, invoice.getInvoiceId());
		invoice.setInvoiceStatus(InvoiceStatus.PAID);
		invoice.setPaidAt(LocalDateTime.now());
		return invoiceDao.update(invoice);
	}
	
	public Invoice markVoided(Invoice invoice) {
		releaseInvoiceCharges(invoice);
		invoice.setInvoiceStatus(InvoiceStatus.VOIDED);
		return invoiceDao.update(invoice);
	}
	
	public Invoice markVoidedWithRefund(Invoice invoice, Client client) {
		transactionService.record(client, invoice.getTotalAmount(), TransactionType.REFUND, invoice.getInvoiceId());
		releaseInvoiceCharges(invoice);
		invoice.setInvoiceStatus(InvoiceStatus.VOIDED);
		return invoiceDao.update(invoice);
	}
	
	// ---- Private Helpers ----
	
	private InvoiceState stateFor(InvoiceStatus invoiceStatus) {
		
		if (invoiceStatus == InvoiceStatus.UNPAID) {
			return new UnpaidInvoiceState();
		} else if (invoiceStatus == InvoiceStatus.PAID) {
			return new PaidInvoiceState();
		} else {
			return new VoidedInvoiceState();
		}
		
	}
	
	private void releaseInvoiceCharges(Invoice invoice) {
		taskService.unlinkFromInvoice(invoice.getInvoiceId());
	}
	
}
