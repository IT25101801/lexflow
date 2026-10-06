package lexflow.dto.user;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.*;
import lexflow.entity.user.Gender;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
public class EditProfileRequest {
	
	@NotBlank(message = "Name is required")
	private String name;
	
	@NotNull(message = "Date of birth is required")
	private LocalDate dateOfBirth;
	
	@NotNull(message = "Gender is required")
	private Gender gender;
	
	@NotEmpty(message = "At least one address is required")
	private List<@NotBlank(message = "Each address is required") @Size(min = 5, max = 800, message = "Each address must be 5 to 800 characters") String> addresses;
	
	@NotEmpty(message = "At least one phone number is required")
	private List<@NotBlank(message = "Each phone number is required") @Pattern(regexp = "0[0-9]{9}", message = "Each phone number must be 10 digits starting with 0") String> phoneNumbers;
	
	private MultipartFile nicFile;
	
	private MultipartFile barFile;
	
	@AssertTrue(message = "Must be at least 18 years old")
	public boolean isAdult() {
		return dateOfBirth == null || !dateOfBirth.isAfter(LocalDate.now().minusYears(18));
	}
	
}
