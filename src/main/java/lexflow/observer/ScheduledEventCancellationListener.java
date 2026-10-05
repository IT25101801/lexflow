package lexflow.observer;

import lexflow.dao.EventDao;
import lexflow.entity.scheduling.CaseEvent;
import lexflow.entity.scheduling.ConsultationEvent;
import lexflow.entity.scheduling.EventStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ScheduledEventCancellationListener {
	
	private final EventDao eventDao;
	
	@EventListener
	public void onCaseEventsCancellationRequested(CaseEventsCancellationRequested requested) {
		List<CaseEvent> events = eventDao.findByCaseId(requested.caseId());
		
		for (CaseEvent event : events) {
			
			if (event.getEventStatus() != EventStatus.SCHEDULED) {
				continue;
			}
			
			event.setEventStatus(EventStatus.CANCELED);
			event.setCanceledAt(LocalDateTime.now());
			eventDao.update(event);
		}
		
	}
	
	@EventListener
	public void onConsultationEventsCancellationRequested(ConsultationEventsCancellationRequested requested) {
		List<ConsultationEvent> meetings = eventDao.findByConsultationId(requested.consultationId());
		
		for (ConsultationEvent meeting : meetings) {
			
			if (meeting.getEventStatus() != EventStatus.SCHEDULED) {
				continue;
			}
			
			meeting.setEventStatus(EventStatus.CANCELED);
			meeting.setCanceledAt(LocalDateTime.now());
			eventDao.update(meeting);
		}
		
	}
	
}
