package lexflow.exception.case_;

import lexflow.exception.BusinessRuleViolationException;
import lombok.Getter;

@Getter
public class NoTaskTypeSelectedException extends BusinessRuleViolationException {
	
	private final String taskTitle;
	
	public NoTaskTypeSelectedException(String taskTitle) {
		super(String.format("Task \"%s\" cannot be saved: select documenting, scheduling or billing", taskTitle));
		this.taskTitle = taskTitle;
	}
	
}
