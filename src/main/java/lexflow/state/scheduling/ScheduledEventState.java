package lexflow.state.scheduling;

import lexflow.entity.scheduling.Event;
import lexflow.service.EventService;

import java.time.LocalDate;
import java.time.LocalTime;

public class ScheduledEventState implements EventState {
	
	@Override
	public Event reschedule(Event event, EventService eventService, String subject, LocalDate date, LocalTime startTime, LocalTime endTime, String location, String notes) {
		return eventService.applyReschedule(event, subject, date, startTime, endTime, location, notes);
	}
	
	@Override
	public Event complete(Event event, EventService eventService) {
		return eventService.applyCompletion(event);
	}
	
	@Override
	public Event cancel(Event event, EventService eventService) {
		return eventService.applyCancellation(event);
	}
	
}
