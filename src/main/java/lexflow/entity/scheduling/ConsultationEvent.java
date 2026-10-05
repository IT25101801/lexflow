package lexflow.entity.scheduling;

import lexflow.entity.consultation.Consultation;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Getter
@Setter
public class ConsultationEvent extends Event {
	
	private final Consultation consultation;
	
	public ConsultationEvent(String subject, LocalDate date, LocalTime startTime, LocalTime endTime, String location, String notes, LocalDateTime scheduledAt, Consultation consultation) {
		super(EventType.CONSULTATION_EVENT, subject, date, startTime, endTime, location, notes, scheduledAt);
		this.consultation = consultation;
	}
	
	public ConsultationEvent(Long eventId, String subject, LocalDate date, LocalTime startTime, LocalTime endTime, String location, String notes, EventStatus eventStatus, LocalDateTime scheduledAt, LocalDateTime completedAt, LocalDateTime canceledAt, Consultation consultation) {
		super(eventId, EventType.CONSULTATION_EVENT, subject, date, startTime, endTime, location, notes, eventStatus, scheduledAt, completedAt, canceledAt);
		this.consultation = consultation;
	}
	
}
