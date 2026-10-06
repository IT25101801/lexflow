package lexflow.dto.consultation;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lexflow.entity.consultation.CaseType;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Getter
@Setter
public class SubmitOnlineConsultationRequest {
	
	@NotNull(message = "Case type is required")
	private CaseType caseType;
	
	@NotEmpty(message = "At least one opposing party is required")
	private List<@NotBlank(message = "Each opposing party is required") @Size(max = 800, message = "Each opposing party must be at most 800 characters") String> opposingParties;
	
	@NotBlank(message = "Description is required")
	private String description;
	
	private List<MultipartFile> consultationFiles;
	
}
