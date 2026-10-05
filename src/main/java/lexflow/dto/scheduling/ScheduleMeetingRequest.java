package lexflow.dto.scheduling;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
public class ScheduleMeetingRequest {
	
	@NotBlank(message = "Subject is required")
	@Size(max = 255, message = "Subject must be at most 255 characters")
	private String subject;
	
	@NotNull(message = "Date is required")
	@FutureOrPresent(message = "Date cannot be in the past")
	private LocalDate date;
	
	@NotNull(message = "Start time is required")
	private LocalTime startTime;
	
	@NotNull(message = "End time is required")
	private LocalTime endTime;
	
	@NotBlank(message = "Location is required")
	@Size(max = 255, message = "Location must be at most 255 characters")
	private String location;
	
	private String notes;
	
}
