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
	public void onConsultationEventsCancellationRequested(ConsultationEventsCancellationRequested requested) {
		List<ConsultationEvent> consultationEvents = eventDao.findByConsultationId(requested.consultationId());
		
		for (ConsultationEvent consultationEvent : consultationEvents) {
			
			if (consultationEvent.getEventStatus() != EventStatus.SCHEDULED) {
				continue;
			}
			
			consultationEvent.setEventStatus(EventStatus.CANCELED);
			consultationEvent.setCanceledAt(LocalDateTime.now());
			eventDao.update(consultationEvent);
		}
		
	}
	
	@EventListener
	public void onCaseEventsCancellationRequested(CaseEventsCancellationRequested requested) {
		List<CaseEvent> caseEvents = eventDao.findByCaseId(requested.caseId());
		
		for (CaseEvent caseEvent : caseEvents) {
			
			if (caseEvent.getEventStatus() != EventStatus.SCHEDULED) {
				continue;
			}
			
			caseEvent.setEventStatus(EventStatus.CANCELED);
			caseEvent.setCanceledAt(LocalDateTime.now());
			eventDao.update(caseEvent);
		}
		
	}
	
}
