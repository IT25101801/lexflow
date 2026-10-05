package lexflow.state.scheduling;

import lexflow.entity.scheduling.Event;
import lexflow.entity.scheduling.EventStatus;
import lexflow.exception.scheduling.InvalidEventStateException;

public class CompletedEventState implements EventState {
	
	@Override
	public void guardReschedule(Event event) {
		throw new InvalidEventStateException(event.getEventId(), event.getEventStatus(), EventStatus.SCHEDULED);
	}
	
	@Override
	public void guardComplete(Event event) {
		throw new InvalidEventStateException(event.getEventId(), event.getEventStatus(), EventStatus.SCHEDULED);
	}
	
	@Override
	public void guardCancel(Event event) {
		throw new InvalidEventStateException(event.getEventId(), event.getEventStatus(), EventStatus.SCHEDULED);
	}
	
}
