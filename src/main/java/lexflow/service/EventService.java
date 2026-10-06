package lexflow.service;

import lexflow.dao.ConsultationDao;
import lexflow.dao.EventDao;
import lexflow.entity.case_.Case;
import lexflow.entity.case_.Task;
import lexflow.entity.consultation.Consultation;
import lexflow.entity.consultation.ConsultationStatus;
import lexflow.entity.scheduling.CaseEvent;
import lexflow.entity.scheduling.ConsultationEvent;
import lexflow.entity.scheduling.Event;
import lexflow.entity.scheduling.EventStatus;
import lexflow.entity.user.Associate;
import lexflow.entity.user.Client;
import lexflow.entity.user.Paralegal;
import lexflow.entity.user.User;
import lexflow.exception.scheduling.EventConflictException;
import lexflow.exception.scheduling.EventInThePastException;
import lexflow.exception.scheduling.EventNotYetStartedException;
import lexflow.exception.scheduling.InvalidEventTimeRangeException;
import lexflow.state.scheduling.CanceledEventState;
import lexflow.state.scheduling.CompletedEventState;
import lexflow.state.scheduling.EventState;
import lexflow.state.scheduling.ScheduledEventState;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EventService {
	
	private final EventDao eventDao;
	
	private final ConsultationDao consultationDao;
	
	private final ConsultationService consultationService;
	
	// Create
	
	@Transactional
	public ConsultationEvent schedule(Consultation consultation, String subject, LocalDate date, LocalTime startTime, LocalTime endTime, String location, String notes) {
		validateSchedule(date, startTime, endTime);
		checkAllAvailability(consultation.getAssociate(), consultation.getParalegal(), consultation.getClient(), date, startTime, endTime, null);
		
		consultationService.markScheduled(consultation);
		
		ConsultationEvent event = new ConsultationEvent(subject, date, startTime, endTime, location, notes, LocalDateTime.now(), consultation);
		
		return (ConsultationEvent) eventDao.insert(event);
	}
	
	public CaseEvent schedule(Case case_, Task task, String subject, LocalDate date, LocalTime startTime, LocalTime endTime, String location, String notes) {
		validateSchedule(date, startTime, endTime);
		checkAllAvailability(case_.getAssociate(), case_.getParalegal(), case_.getClient(), date, startTime, endTime, null);
		
		CaseEvent event = new CaseEvent(subject, date, startTime, endTime, location, notes, LocalDateTime.now(), case_, task.getTaskId());
		
		return (CaseEvent) eventDao.insert(event);
	}
	
	// Read
	
	public Event findById(Long eventId) {
		return eventDao.findById(eventId);
	}
	
	public ConsultationEvent findOwnedByConsultation(Long eventId, Consultation consultation) {
		Event event = findById(eventId);
		
		if (!(event instanceof ConsultationEvent consultationEvent) || !consultationEvent.getConsultation().getConsultationId().equals(consultation.getConsultationId())) {
			throw new AccessDeniedException("Not a meeting for this consultation.");
		}
		
		return consultationEvent;
	}
	
	public Event findOwnedByAssociate(Long eventId, Associate associate) {
		
		for (Event event : findAllForAssociate(associate)) {
			
			if (event.getEventId().equals(eventId)) {
				return event;
			}
			
		}
		
		throw new AccessDeniedException("Not authorized to view this event.");
	}
	
	public Event findOwnedByParalegal(Long eventId, Paralegal paralegal) {
		
		for (Event event : findAllForParalegal(paralegal)) {
			
			if (event.getEventId().equals(eventId)) {
				return event;
			}
			
		}
		
		throw new AccessDeniedException("Not authorized to view this event.");
	}
	
	public Event findOwnedByClient(Long eventId, Client client) {
		
		for (Event event : findAllForClient(client)) {
			
			if (event.getEventId().equals(eventId)) {
				return event;
			}
			
		}
		
		throw new AccessDeniedException("Not authorized to view this event.");
	}
	
	public List<Event> findAll() {
		return eventDao.findAll();
	}
	
	public List<ConsultationEvent> findByConsultation(Consultation consultation) {
		return eventDao.findByConsultationId(consultation.getConsultationId());
	}
	
	public List<CaseEvent> findByCase(Case case_) {
		return eventDao.findByCaseId(case_.getCaseId());
	}
	
	public List<Event> findAllForAssociate(Associate associate) {
		List<Event> events = new ArrayList<>();
		
		for (Event event : eventDao.findAll()) {
			Associate eventAssociate = getAssociate(event);
			
			if (eventAssociate != null && eventAssociate.getUserId().equals(associate.getUserId())) {
				events.add(event);
			}
			
		}
		
		return events;
	}
	
	public List<Event> findAllForParalegal(Paralegal paralegal) {
		List<Event> events = new ArrayList<>();
		
		for (Event event : eventDao.findAll()) {
			Paralegal eventParalegal = getParalegal(event);
			
			if (eventParalegal != null && eventParalegal.getUserId().equals(paralegal.getUserId())) {
				events.add(event);
			}
			
		}
		
		return events;
	}
	
	public List<Event> findAllForClient(Client client) {
		List<Event> events = new ArrayList<>();
		
		for (Event event : eventDao.findAll()) {
			Client eventClient = getClient(event);
			
			if (eventClient != null && eventClient.getUserId().equals(client.getUserId())) {
				events.add(event);
			}
			
		}
		
		return events;
	}
	
	public List<ConsultationEvent> findAllForConsultations() {
		List<ConsultationEvent> events = new ArrayList<>();
		
		for (Event event : eventDao.findAll()) {
			
			if (event instanceof ConsultationEvent consultationEvent) {
				events.add(consultationEvent);
			}
			
		}
		
		return events;
	}
	
	// Update
	
	public ConsultationEvent reschedule(ConsultationEvent event, String subject, LocalDate date, LocalTime startTime, LocalTime endTime, String location, String notes) {
		stateFor(event.getEventStatus()).guardReschedule(event);
		
		Consultation consultation = event.getConsultation();
		
		validateSchedule(date, startTime, endTime);
		checkAllAvailability(consultation.getAssociate(), consultation.getParalegal(), consultation.getClient(), date, startTime, endTime, event.getEventId());
		
		event.setSubject(subject);
		event.setDate(date);
		event.setStartTime(startTime);
		event.setEndTime(endTime);
		event.setLocation(location);
		event.setNotes(notes);
		
		return (ConsultationEvent) eventDao.update(event);
	}
	
	public CaseEvent reschedule(CaseEvent event, String subject, LocalDate date, LocalTime startTime, LocalTime endTime, String location, String notes) {
		stateFor(event.getEventStatus()).guardReschedule(event);
		
		Case case_ = event.getCase_();
		
		validateSchedule(date, startTime, endTime);
		checkAllAvailability(case_.getAssociate(), case_.getParalegal(), case_.getClient(), date, startTime, endTime, event.getEventId());
		
		event.setSubject(subject);
		event.setDate(date);
		event.setStartTime(startTime);
		event.setEndTime(endTime);
		event.setLocation(location);
		event.setNotes(notes);
		
		return (CaseEvent) eventDao.update(event);
	}
	
	public Event complete(Event event) {
		stateFor(event.getEventStatus()).guardComplete(event);
		
		LocalDateTime startsAt = LocalDateTime.of(event.getDate(), event.getStartTime());
		
		if (startsAt.isAfter(LocalDateTime.now())) {
			throw new EventNotYetStartedException(event.getEventId(), startsAt);
		}
		
		event.setEventStatus(EventStatus.COMPLETED);
		event.setCompletedAt(LocalDateTime.now());
		
		return eventDao.update(event);
	}
	
	@Transactional
	public Event cancel(Event event) {
		stateFor(event.getEventStatus()).guardCancel(event);
		
		event.setEventStatus(EventStatus.CANCELED);
		event.setCanceledAt(LocalDateTime.now());
		
		Event canceledEvent = eventDao.update(event);
		
		if (event instanceof ConsultationEvent consultationEvent) {
			Consultation consultation = consultationEvent.getConsultation();
			consultation.setConsultationStatus(ConsultationStatus.ACCEPTED_BY_PARTNER);
			consultationDao.update(consultation);
		}
		
		return canceledEvent;
	}
	
	// State Pattern
	
	private EventState stateFor(EventStatus eventStatus) {
		
		if (eventStatus == EventStatus.SCHEDULED) {
			return new ScheduledEventState();
		} else if (eventStatus == EventStatus.COMPLETED) {
			return new CompletedEventState();
		} else {
			return new CanceledEventState();
		}
		
	}
	
	// Private Helpers
	
	private void validateSchedule(LocalDate date, LocalTime startTime, LocalTime endTime) {
		
		if (!startTime.isBefore(endTime)) {
			throw new InvalidEventTimeRangeException(startTime, endTime);
		}
		
		LocalDateTime startsAt = LocalDateTime.of(date, startTime);
		
		if (startsAt.isBefore(LocalDateTime.now())) {
			throw new EventInThePastException(date, startTime);
		}
		
	}
	
	private void checkAllAvailability(Associate associate, Paralegal paralegal, Client client, LocalDate date, LocalTime startTime, LocalTime endTime, Long excludeEventId) {
		checkAvailability(associate, findAllForAssociate(associate), date, startTime, endTime, excludeEventId);
		checkAvailability(paralegal, findAllForParalegal(paralegal), date, startTime, endTime, excludeEventId);
		checkAvailability(client, findAllForClient(client), date, startTime, endTime, excludeEventId);
	}
	
	private void checkAvailability(User user, List<Event> userEvents, LocalDate date, LocalTime startTime, LocalTime endTime, Long excludeEventId) {
		
		if (hasConflict(userEvents, date, startTime, endTime, excludeEventId)) {
			throw new EventConflictException(user.getName(), date, startTime, endTime);
		}
		
	}
	
	private boolean hasConflict(List<Event> userEvents, LocalDate date, LocalTime startTime, LocalTime endTime, Long excludeEventId) {
		
		for (Event event : userEvents) {
			
			if (event.getEventId().equals(excludeEventId)) {
				continue;
			}
			
			if (event.getEventStatus() != EventStatus.SCHEDULED) {
				continue;
			}
			
			if (event.getDate().equals(date) && overlaps(startTime, endTime, event.getStartTime(), event.getEndTime())) {
				return true;
			}
			
		}
		
		return false;
	}
	
	private boolean overlaps(LocalTime startA, LocalTime endA, LocalTime startB, LocalTime endB) {
		return startA.isBefore(endB) && startB.isBefore(endA);
	}
	
	private Associate getAssociate(Event event) {
		
		if (event instanceof ConsultationEvent consultationEvent) {
			return consultationEvent.getConsultation().getAssociate();
		} else if (event instanceof CaseEvent caseEvent) {
			return caseEvent.getCase_().getAssociate();
		}
		
		return null;
	}
	
	private Paralegal getParalegal(Event event) {
		
		if (event instanceof ConsultationEvent consultationEvent) {
			return consultationEvent.getConsultation().getParalegal();
		} else if (event instanceof CaseEvent caseEvent) {
			return caseEvent.getCase_().getParalegal();
		}
		
		return null;
	}
	
	private Client getClient(Event event) {
		
		if (event instanceof ConsultationEvent consultationEvent) {
			return consultationEvent.getConsultation().getClient();
		} else if (event instanceof CaseEvent caseEvent) {
			return caseEvent.getCase_().getClient();
		}
		
		return null;
	}
	
}
