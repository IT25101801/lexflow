package lexflow.dto.document;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RenameDocumentRequest {
	
	@NotBlank(message = "File name is required")
	@Size(max = 255, message = "File name must be at most 255 characters")
	private String originalFilename;
	
}
