package lexflow.exception.scheduling;

import lexflow.exception.BusinessRuleViolationException;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class EventNotYetStartedException extends BusinessRuleViolationException {
	
	private final Long eventId;
	private final LocalDateTime startsAt;
	
	public EventNotYetStartedException(Long eventId, LocalDateTime startsAt) {
		super(String.format("Event %d cannot be marked complete before it starts (scheduled for %s)", eventId, startsAt));
		this.eventId = eventId;
		this.startsAt = startsAt;
	}
	
}
