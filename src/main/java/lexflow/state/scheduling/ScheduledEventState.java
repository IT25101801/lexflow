package lexflow.state.scheduling;

import lexflow.entity.scheduling.Event;

public class ScheduledEventState implements EventState {
	
	@Override
	public void guardReschedule(Event event) {
	}
	
	@Override
	public void guardComplete(Event event) {
	}
	
	@Override
	public void guardCancel(Event event) {
	}
	
}
