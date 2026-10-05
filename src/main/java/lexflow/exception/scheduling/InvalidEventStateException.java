package lexflow.exception.scheduling;

import lexflow.entity.scheduling.EventStatus;
import lexflow.exception.InvalidStateException;
import lombok.Getter;

@Getter
public class InvalidEventStateException extends InvalidStateException {
	
	private final Long eventId;
	private final EventStatus currentStatus;
	private final EventStatus requiredStatus;
	
	public InvalidEventStateException(Long eventId, EventStatus currentStatus, EventStatus requiredStatus) {
		super(String.format("Event %d cannot be processed: status must be %s, but it is currently %s", eventId, requiredStatus, currentStatus));
		this.eventId = eventId;
		this.currentStatus = currentStatus;
		this.requiredStatus = requiredStatus;
	}
	
}
