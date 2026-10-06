package lexflow.exception.scheduling;

import lexflow.exception.BusinessRuleViolationException;
import lombok.Getter;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Getter
public class EventNotYetStartedException extends BusinessRuleViolationException {
	
	private final Long eventId;
	private final LocalDateTime startsAt;
	
	public EventNotYetStartedException(Long eventId, LocalDateTime startsAt) {
		super(String.format("This event cannot be marked complete before it starts (scheduled for %s)", startsAt.format(DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a"))));
		this.eventId = eventId;
		this.startsAt = startsAt;
	}
	
}
