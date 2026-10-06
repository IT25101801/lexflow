package lexflow.state.scheduling;

import lexflow.entity.scheduling.Event;
import lexflow.entity.scheduling.EventStatus;
import lexflow.exception.scheduling.InvalidEventStateException;
import lexflow.service.EventService;

import java.time.LocalDate;
import java.time.LocalTime;

public class CanceledEventState implements EventState {
	
	@Override
	public Event reschedule(Event event, EventService eventService, String subject, LocalDate date, LocalTime startTime, LocalTime endTime, String location, String notes) {
		throw new InvalidEventStateException(event.getEventId(), event.getEventStatus(), EventStatus.SCHEDULED);
	}
	
	@Override
	public Event complete(Event event, EventService eventService) {
		throw new InvalidEventStateException(event.getEventId(), event.getEventStatus(), EventStatus.SCHEDULED);
	}
	
	@Override
	public Event cancel(Event event, EventService eventService) {
		throw new InvalidEventStateException(event.getEventId(), event.getEventStatus(), EventStatus.SCHEDULED);
	}
	
}
