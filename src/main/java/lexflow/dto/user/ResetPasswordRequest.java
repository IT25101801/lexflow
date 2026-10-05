package lexflow.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResetPasswordRequest {
	
	@NotBlank(message = "Temporary password is required")
	@Size(min = 8, message = "Temporary password must be at least 8 characters")
	private String temporaryPassword;
	
}
