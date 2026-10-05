package lexflow.dto.consultation;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AssignStaffRequest {
	
	@NotNull(message = "Associate is required")
	private Long associateId;
	
	@NotNull(message = "Paralegal is required")
	private Long paralegalId;
	
}
