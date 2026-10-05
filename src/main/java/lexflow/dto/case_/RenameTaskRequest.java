package lexflow.dto.case_;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RenameTaskRequest {
	
	@NotBlank(message = "Title is required")
	@Size(max = 255, message = "Title must be at most 255 characters")
	private String title;
	
}
