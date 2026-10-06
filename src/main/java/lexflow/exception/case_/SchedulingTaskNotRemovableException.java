package lexflow.exception.case_;

import lexflow.exception.BusinessRuleViolationException;
import lombok.Getter;

@Getter
public class SchedulingTaskNotRemovableException extends BusinessRuleViolationException {
	
	private final String taskTitle;
	
	public SchedulingTaskNotRemovableException(String taskTitle) {
		super(String.format("Task \"%s\" must stay a scheduling task: cancel its event instead", taskTitle));
		this.taskTitle = taskTitle;
	}
	
}
