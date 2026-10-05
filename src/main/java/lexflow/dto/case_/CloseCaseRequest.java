package lexflow.dto.case_;

import jakarta.validation.constraints.NotNull;
import lexflow.entity.case_.CaseResolutionStatus;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CloseCaseRequest {
	
	@NotNull(message = "Resolution status is required")
	private CaseResolutionStatus caseResolutionStatus;
	
}
