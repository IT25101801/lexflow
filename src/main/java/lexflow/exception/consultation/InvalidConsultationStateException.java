package lexflow.exception.consultation;

import lexflow.entity.consultation.ConsultationStatus;
import lexflow.exception.InvalidStateException;
import lombok.Getter;

@Getter
public class InvalidConsultationStateException extends InvalidStateException {
	
	private final Long consultationId;
	private final ConsultationStatus currentStatus;
	private final ConsultationStatus requiredStatus;
	
	public InvalidConsultationStateException(Long consultationId, ConsultationStatus currentStatus, ConsultationStatus requiredStatus) {
		super(String.format("This consultation is %s, but it must be %s to do this.", currentStatus.name().toLowerCase().replace('_', ' '), requiredStatus.name().toLowerCase().replace('_', ' ')));
		this.consultationId = consultationId;
		this.currentStatus = currentStatus;
		this.requiredStatus = requiredStatus;
	}
	
}
