package lexflow.entity.case_;

import lexflow.entity.scheduling.CaseEvent;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
public class Task {
	
	private Long taskId;
	private final Long caseId;
	private String title;
	private boolean documentingTask;
	private boolean schedulingTask;
	private boolean billingTask;
	private CaseEvent scheduledEvent;
	private BigDecimal billableAmount;
	private Long billedInvoiceId;
	private final LocalDateTime loggedAt;
	
	public Task(Long caseId, String title, boolean documentingTask, boolean schedulingTask, boolean billingTask, BigDecimal billableAmount, LocalDateTime loggedAt) {
		this.caseId = caseId;
		this.title = title;
		this.documentingTask = documentingTask;
		this.schedulingTask = schedulingTask;
		this.billingTask = billingTask;
		this.billableAmount = billableAmount;
		this.loggedAt = loggedAt;
	}
	
	public Task(Long taskId, Long caseId, String title, boolean documentingTask, boolean schedulingTask, boolean billingTask, CaseEvent scheduledEvent, BigDecimal billableAmount, Long billedInvoiceId, LocalDateTime loggedAt) {
		this.taskId = taskId;
		this.caseId = caseId;
		this.title = title;
		this.documentingTask = documentingTask;
		this.schedulingTask = schedulingTask;
		this.billingTask = billingTask;
		this.scheduledEvent = scheduledEvent;
		this.billableAmount = billableAmount;
		this.billedInvoiceId = billedInvoiceId;
		this.loggedAt = loggedAt;
	}
	
}
