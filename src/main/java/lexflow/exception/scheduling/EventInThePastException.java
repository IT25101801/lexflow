package lexflow.exception.scheduling;

import lexflow.exception.BusinessRuleViolationException;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
public class EventInThePastException extends BusinessRuleViolationException {
	
	private final LocalDate date;
	private final LocalTime startTime;
	
	public EventInThePastException(LocalDate date, LocalTime startTime) {
		super(String.format("Event cannot be scheduled for %s at %s, which is in the past", date, startTime));
		this.date = date;
		this.startTime = startTime;
	}
	
}
