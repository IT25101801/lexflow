package lexflow.exception.case_;

import lexflow.exception.BusinessRuleViolationException;
import lombok.Getter;

@Getter
public class IncompleteDocumentingTaskException extends BusinessRuleViolationException {
	
	private final String taskTitle;
	
	public IncompleteDocumentingTaskException(String taskTitle) {
		super(String.format("Documenting task \"%s\" cannot be added: at least one document is required", taskTitle));
		this.taskTitle = taskTitle;
	}
	
}
