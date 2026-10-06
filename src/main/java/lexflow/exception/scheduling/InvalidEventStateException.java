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
		super(String.format("This event is %s, but it must be %s to do this.", currentStatus.name().toLowerCase().replace('_', ' '), requiredStatus.name().toLowerCase().replace('_', ' ')));
		this.eventId = eventId;
		this.currentStatus = currentStatus;
		this.requiredStatus = requiredStatus;
	}
	
}
