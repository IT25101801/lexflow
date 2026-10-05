package lexflow.exception.finance;

import lexflow.exception.BusinessRuleViolationException;
import lombok.Getter;

@Getter
public class BillableAmountNotEditableException extends BusinessRuleViolationException {
	
	private final Long taskId;
	
	public BillableAmountNotEditableException(Long taskId) {
		super(String.format("Task %d's billable amount cannot be edited: it is already billed on an invoice", taskId));
		this.taskId = taskId;
	}
	
}
