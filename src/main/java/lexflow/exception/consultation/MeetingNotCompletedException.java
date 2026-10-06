package lexflow.exception.consultation;

import lexflow.exception.BusinessRuleViolationException;
import lombok.Getter;

@Getter
public class MeetingNotCompletedException extends BusinessRuleViolationException {
	
	private final Long consultationId;
	
	public MeetingNotCompletedException(Long consultationId) {
		super("This consultation cannot be accepted or rejected before its meeting is marked complete");
		this.consultationId = consultationId;
	}
	
}
