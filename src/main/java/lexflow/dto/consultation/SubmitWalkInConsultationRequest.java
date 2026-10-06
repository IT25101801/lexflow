package lexflow.dto.consultation;

import lexflow.util.ValidationUtil;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
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
public class SubmitWalkInConsultationRequest {
	
	@NotBlank(message = "Client code is required")
	@Pattern(regexp = "CLT-\\d+-\\d{8}", message = "Client code must look like CLT-31-70617268")
	private String clientCode;
	
	@NotNull(message = "Case type is required")
	private CaseType caseType;
	
	@NotEmpty(message = "At least one opposing party is required")
	private List<@NotBlank(message = "Each opposing party is required") @Size(max = 800, message = "Each opposing party must be at most 800 characters") String> opposingParties;
	
	@NotBlank(message = "Description is required")
	private String description;
	
	private List<MultipartFile> consultationFiles;
	
	@AssertTrue(message = "Opposing parties must not repeat")
	public boolean isOpposingPartiesUnique() {
		return ValidationUtil.hasNoRepeats(opposingParties);
	}
	
}
