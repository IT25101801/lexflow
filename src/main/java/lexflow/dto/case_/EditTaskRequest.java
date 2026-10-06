package lexflow.dto.case_;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Getter
@Setter
public class EditTaskRequest {
	
	@NotBlank(message = "Title is required")
	@Size(max = 255, message = "Title must be at most 255 characters")
	private String title;
	
	private Boolean documentingTask;
	
	private List<MultipartFile> caseFiles;
	
	private Boolean schedulingTask;
	
	private LocalDate date;
	
	private LocalTime startTime;
	
	private LocalTime endTime;
	
	@Size(max = 255, message = "Location must be at most 255 characters")
	private String location;
	
	private String notes;
	
	private Boolean billingTask;
	
	@DecimalMin(value = "0.01", message = "Billable amount must be at least 0.01")
	private BigDecimal billableAmount;
	
}
