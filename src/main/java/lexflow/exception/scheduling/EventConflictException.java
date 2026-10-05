package lexflow.exception.scheduling;

import lexflow.exception.BusinessRuleViolationException;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
public class EventConflictException extends BusinessRuleViolationException {
	
	private final String personName;
	private final LocalDate date;
	private final LocalTime startTime;
	private final LocalTime endTime;
	
	public EventConflictException(String personName, LocalDate date, LocalTime startTime, LocalTime endTime) {
		super(String.format("%s already has an event on %s between %s and %s", personName, date, startTime, endTime));
		this.personName = personName;
		this.date = date;
		this.startTime = startTime;
		this.endTime = endTime;
	}
	
}
