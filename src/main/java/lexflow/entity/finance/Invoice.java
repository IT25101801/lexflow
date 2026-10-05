package lexflow.entity.finance;

import lexflow.entity.document.InvoiceDocument;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
public class Invoice {
	
	private Long invoiceId;
	private String invoiceCode;
	private final Long caseId;
	private final BigDecimal totalAmount;
	private InvoiceStatus invoiceStatus;
	private final LocalDateTime compiledAt;
	private InvoiceDocument invoiceDocument;
	private LocalDateTime paidAt;
	
	public Invoice(Long caseId, BigDecimal totalAmount, LocalDateTime compiledAt) {
		this.caseId = caseId;
		this.totalAmount = totalAmount;
		this.invoiceStatus = InvoiceStatus.UNPAID;
		this.compiledAt = compiledAt;
	}
	
	public Invoice(Long invoiceId, String invoiceCode, Long caseId, BigDecimal totalAmount, InvoiceStatus invoiceStatus, LocalDateTime compiledAt, InvoiceDocument invoiceDocument, LocalDateTime paidAt) {
		this.invoiceId = invoiceId;
		this.invoiceCode = invoiceCode;
		this.caseId = caseId;
		this.totalAmount = totalAmount;
		this.invoiceStatus = invoiceStatus;
		this.compiledAt = compiledAt;
		this.invoiceDocument = invoiceDocument;
		this.paidAt = paidAt;
	}
	
}
