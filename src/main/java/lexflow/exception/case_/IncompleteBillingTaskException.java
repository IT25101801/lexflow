package lexflow.exception.case_;

import lexflow.exception.BusinessRuleViolationException;
import lombok.Getter;

@Getter
public class IncompleteBillingTaskException extends BusinessRuleViolationException {
	
	private final String taskTitle;
	
	public IncompleteBillingTaskException(String taskTitle) {
		super(String.format("Billing task \"%s\" cannot be added: an amount is required", taskTitle));
		this.taskTitle = taskTitle;
	}
	
}
