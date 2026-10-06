package lexflow.state.scheduling;

import lexflow.entity.scheduling.Event;
import lexflow.service.EventService;

import java.time.LocalDate;
import java.time.LocalTime;

public interface EventState {

	Event reschedule(Event event, EventService eventService, String subject, LocalDate date, LocalTime startTime, LocalTime endTime, String location, String notes);

	Event complete(Event event, EventService eventService);

	Event cancel(Event event, EventService eventService);

}
