package lexflow.dto.consultation;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RejectConsultationRequest {
	
	@NotBlank(message = "Rejection reason is required")
	private String rejectionReason;
	
}
