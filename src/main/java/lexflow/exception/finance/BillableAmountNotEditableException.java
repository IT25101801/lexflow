package lexflow.exception.finance;

import lexflow.exception.BusinessRuleViolationException;
import lombok.Getter;

@Getter
public class BillableAmountNotEditableException extends BusinessRuleViolationException {
	
	private final Long taskId;
	
	public BillableAmountNotEditableException(Long taskId) {
		super("This task's billable amount cannot be edited: it is already billed on an invoice");
		this.taskId = taskId;
	}
	
}
