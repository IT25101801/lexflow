package lexflow.state.scheduling;

import lexflow.entity.scheduling.Event;

public interface EventState {
	
	void guardReschedule(Event event);
	
	void guardComplete(Event event);
	
	void guardCancel(Event event);
	
}
