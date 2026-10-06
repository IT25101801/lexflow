package lexflow.service;

import lexflow.dao.EventDao;
import lexflow.dao.TaskDao;
import lexflow.entity.case_.Case;
import lexflow.entity.case_.Task;
import lexflow.entity.consultation.Consultation;
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
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EventService {
	
	private final EventDao eventDao;
	private final TaskDao taskDao;
private final ConsultationService consultationService;
	
	// Create
	
	@Transactional
	public ConsultationEvent schedule(Consultation consultation, String subject, LocalDate date, LocalTime startTime, LocalTime endTime, String location, String notes) {
		validateSchedule(date, startTime, endTime);
		checkAllAvailability(consultation.getAssociate(), consultation.getParalegal(), consultation.getClient(), date, startTime, endTime, null);
		
		consultationService.markScheduled(consultation);
		ConsultationEvent consultationEvent = new ConsultationEvent(subject, date, startTime, endTime, location, notes, LocalDateTime.now(), consultation);
		
		return (ConsultationEvent) eventDao.insert(consultationEvent);
	}
	
	public CaseEvent schedule(Case case_, Task task, String subject, LocalDate date, LocalTime startTime, LocalTime endTime, String location, String notes) {
		validateSchedule(date, startTime, endTime);
		checkAllAvailability(case_.getAssociate(), case_.getParalegal(), case_.getClient(), date, startTime, endTime, null);
		
		CaseEvent caseEvent = new CaseEvent(subject, date, startTime, endTime, location, notes, LocalDateTime.now(), case_, task.getTaskId());
		
		return (CaseEvent) eventDao.insert(caseEvent);
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
		List<Event> events = findAllForAssociate(associate);
		
		for (Event event : events) {
			
			if (event.getEventId().equals(eventId)) {
				return event;
			}
			
		}
		
		throw new AccessDeniedException("Not assigned to you.");
	}
	
	public Event findOwnedByParalegal(Long eventId, Paralegal paralegal) {
		List<Event> events = findAllForParalegal(paralegal);
		
		for (Event event : events) {
			
			if (event.getEventId().equals(eventId)) {
				return event;
			}
			
		}
		
		throw new AccessDeniedException("Not assigned to you.");
	}
	
	public Event findOwnedByClient(Long eventId, Client client) {
		List<Event> events = findAllForClient(client);
		
		for (Event event : events) {
			
			if (event.getEventId().equals(eventId)) {
				return event;
			}
			
		}
		
		throw new AccessDeniedException("Not your event.");
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
		List<Event> allEvents = eventDao.findAll();
		
		for (Event event : allEvents) {
			Associate eventAssociate = getAssociate(event);
			
			if (eventAssociate != null && eventAssociate.getUserId().equals(associate.getUserId())) {
				events.add(event);
			}
			
		}
		
		return events;
	}
	
	public List<Event> findAllForParalegal(Paralegal paralegal) {
		List<Event> events = new ArrayList<>();
		List<Event> allEvents = eventDao.findAll();
		
		for (Event event : allEvents) {
			Paralegal eventParalegal = getParalegal(event);
			
			if (eventParalegal != null && eventParalegal.getUserId().equals(paralegal.getUserId())) {
				events.add(event);
			}
			
		}
		
		return events;
	}
	
	public List<Event> findAllForClient(Client client) {
		List<Event> events = new ArrayList<>();
		List<Event> allEvents = eventDao.findAll();
		
		for (Event event : allEvents) {
			Client eventClient = getClient(event);
			
			if (eventClient != null && eventClient.getUserId().equals(client.getUserId())) {
				events.add(event);
			}
			
		}
		
		return events;
	}
	
	public List<ConsultationEvent> findAllForConsultations() {
		List<ConsultationEvent> consultationEvents = new ArrayList<>();
		List<Event> allEvents = eventDao.findAll();
		
		for (Event event : allEvents) {
			
			if (event instanceof ConsultationEvent consultationEvent) {
				consultationEvents.add(consultationEvent);
			}
			
		}
		
		return consultationEvents;
	}
	
	public List<Event> upcoming(List<? extends Event> events) {
		List<Event> upcomingEvents = new ArrayList<>();
		
		for (Event event : events) {
			
			if (event.getEventStatus() == EventStatus.SCHEDULED) {
				upcomingEvents.add(event);
			}
			
		}
		
		Collections.reverse(upcomingEvents);
		
		return upcomingEvents;
	}
	
	public List<Event> past(List<? extends Event> events) {
		List<Event> pastEvents = new ArrayList<>();
		
		for (Event event : events) {
			
			if (event.getEventStatus() != EventStatus.SCHEDULED) {
				pastEvents.add(event);
			}
			
		}
		
		return pastEvents;
	}
	
	// Update
	
	@Transactional
	public Event reschedule(Event event, String subject, LocalDate date, LocalTime startTime, LocalTime endTime, String location, String notes) {
		return stateFor(event.getEventStatus()).reschedule(event, this, subject, date, startTime, endTime, location, notes);
	}
	
	public Event complete(Event event) {
		return stateFor(event.getEventStatus()).complete(event, this);
	}
	
	@Transactional
	public Event cancel(Event event) {
		return stateFor(event.getEventStatus()).cancel(event, this);
	}
	
	public Event updateSubject(Event event, String subject) {
		event.setSubject(subject);
		
		return eventDao.update(event);
	}

	// State Pattern
	
	public Event applyReschedule(Event event, String subject, LocalDate date, LocalTime startTime, LocalTime endTime, String location, String notes) {
		validateSchedule(date, startTime, endTime);
		
		if (event instanceof ConsultationEvent consultationEvent) {
			Consultation consultation = consultationEvent.getConsultation();
			checkAllAvailability(consultation.getAssociate(), consultation.getParalegal(), consultation.getClient(), date, startTime, endTime, event.getEventId());
		} else {
			Case case_ = ((CaseEvent) event).getCase_();
			checkAllAvailability(case_.getAssociate(), case_.getParalegal(), case_.getClient(), date, startTime, endTime, event.getEventId());
		}
		
		event.setSubject(subject);
		event.setDate(date);
		event.setStartTime(startTime);
		event.setEndTime(endTime);
		event.setLocation(location);
		event.setNotes(notes);
		Event rescheduledEvent = eventDao.update(event);
		
		if (event instanceof CaseEvent caseEvent) {
			taskDao.updateTitle(caseEvent.getTaskId(), subject);
		}
		
		return rescheduledEvent;
	}
	
	public Event applyCompletion(Event event) {
		LocalDateTime startsAt = LocalDateTime.of(event.getDate(), event.getStartTime());
		LocalDateTime completedAt = LocalDateTime.now();
		
		if (startsAt.isAfter(completedAt)) {
			throw new EventNotYetStartedException(event.getEventId(), startsAt);
		}
		
		event.setEventStatus(EventStatus.COMPLETED);
		event.setCompletedAt(completedAt);
		
		return eventDao.update(event);
	}
	
	public Event applyCancellation(Event event) {
		event.setEventStatus(EventStatus.CANCELED);
		event.setCanceledAt(LocalDateTime.now());
		Event canceledEvent = eventDao.update(event);
		
		if (event instanceof ConsultationEvent consultationEvent) {
			consultationService.markUnscheduled(consultationEvent.getConsultation());
		}
		
		return canceledEvent;
	}
	
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
