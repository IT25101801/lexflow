package lexflow.exception.case_;

import lexflow.exception.BusinessRuleViolationException;
import lombok.Getter;

@Getter
public class TaskNotDeletableException extends BusinessRuleViolationException {
	
	private final String taskTitle;
	
	public TaskNotDeletableException(String taskTitle) {
		super(String.format("Task \"%s\" cannot be deleted: it still has an event, documents or an invoice", taskTitle));
		this.taskTitle = taskTitle;
	}
	
}
