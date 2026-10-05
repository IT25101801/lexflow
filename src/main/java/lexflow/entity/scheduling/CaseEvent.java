package lexflow.entity.scheduling;

import lexflow.entity.case_.Case;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Getter
@Setter
public class CaseEvent extends Event {
	
	private final Case case_;
	private final Long taskId;
	
	public CaseEvent(String subject, LocalDate date, LocalTime startTime, LocalTime endTime, String location, String notes, LocalDateTime scheduledAt, Case case_, Long taskId) {
		super(EventType.CASE_EVENT, subject, date, startTime, endTime, location, notes, scheduledAt);
		this.case_ = case_;
		this.taskId = taskId;
	}
	
	public CaseEvent(Long eventId, String subject, LocalDate date, LocalTime startTime, LocalTime endTime, String location, String notes, EventStatus eventStatus, LocalDateTime scheduledAt, LocalDateTime completedAt, LocalDateTime canceledAt, Case case_, Long taskId) {
		super(eventId, EventType.CASE_EVENT, subject, date, startTime, endTime, location, notes, eventStatus, scheduledAt, completedAt, canceledAt);
		this.case_ = case_;
		this.taskId = taskId;
	}
	
}
