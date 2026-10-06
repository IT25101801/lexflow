package lexflow.exception.case_;

import lexflow.exception.BusinessRuleViolationException;
import lombok.Getter;

@Getter
public class IncompleteSchedulingTaskException extends BusinessRuleViolationException {
	
	private final String taskTitle;
	
	public IncompleteSchedulingTaskException(String taskTitle) {
		super(String.format("Scheduling task \"%s\" cannot be saved: date, start time, end time, and location are required", taskTitle));
		this.taskTitle = taskTitle;
	}
	
}
