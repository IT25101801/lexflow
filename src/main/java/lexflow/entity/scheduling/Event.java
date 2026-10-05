package lexflow.entity.scheduling;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Getter
@Setter
public abstract class Event {
	
	protected Long eventId;
	protected final EventType eventType;
	protected String subject;
	protected LocalDate date;
	protected LocalTime startTime;
	protected LocalTime endTime;
	protected String location;
	protected String notes;
	protected EventStatus eventStatus;
	protected LocalDateTime scheduledAt;
	protected LocalDateTime completedAt;
	protected LocalDateTime canceledAt;
	
	protected Event(EventType eventType, String subject, LocalDate date, LocalTime startTime, LocalTime endTime, String location, String notes, LocalDateTime scheduledAt) {
		this.eventType = eventType;
		this.subject = subject;
		this.date = date;
		this.startTime = startTime;
		this.endTime = endTime;
		this.location = location;
		this.notes = notes;
		this.eventStatus = EventStatus.SCHEDULED;
		this.scheduledAt = scheduledAt;
	}
	
	protected Event(Long eventId, EventType eventType, String subject, LocalDate date, LocalTime startTime, LocalTime endTime, String location, String notes, EventStatus eventStatus, LocalDateTime scheduledAt, LocalDateTime completedAt, LocalDateTime canceledAt) {
		this.eventId = eventId;
		this.eventType = eventType;
		this.subject = subject;
		this.date = date;
		this.startTime = startTime;
		this.endTime = endTime;
		this.location = location;
		this.notes = notes;
		this.eventStatus = eventStatus;
		this.scheduledAt = scheduledAt;
		this.completedAt = completedAt;
		this.canceledAt = canceledAt;
	}
	
}
