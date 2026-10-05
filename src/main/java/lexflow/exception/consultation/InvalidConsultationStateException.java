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
		super(String.format("Consultation %d cannot be processed: status must be %s, but it is currently %s", consultationId, requiredStatus, currentStatus));
		this.consultationId = consultationId;
		this.currentStatus = currentStatus;
		this.requiredStatus = requiredStatus;
	}
	
}
