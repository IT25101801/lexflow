package lexflow.exception.scheduling;

import lexflow.exception.BusinessRuleViolationException;
import lombok.Getter;

import java.time.LocalTime;

@Getter
public class InvalidEventTimeRangeException extends BusinessRuleViolationException {
	
	private final LocalTime startTime;
	private final LocalTime endTime;
	
	public InvalidEventTimeRangeException(LocalTime startTime, LocalTime endTime) {
		super(String.format("Event end time %s must be after its start time %s", endTime, startTime));
		this.startTime = startTime;
		this.endTime = endTime;
	}
	
}
