package lexflow.dto.user;

import jakarta.validation.constraints.*;
import lexflow.entity.user.Gender;
import lexflow.util.ValidationUtil;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
public class RegisterClientRequest {
	
	@NotBlank(message = "Email is required")
	@Email(message = "Email must be a valid email address")
	@Size(max = 255, message = "Email must be at most 255 characters")
	private String email;
	
	@NotBlank(message = "Password is required")
	@Size(min = 8, message = "Password must be at least 8 characters")
	private String password;
	
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
	
	@NotBlank(message = "NIC number is required")
	@Pattern(regexp = "[0-9]{9}[VXvx]|[0-9]{12}", message = "NIC number must be 9 digits followed by V or X, or 12 digits")
	private String nicNumber;
	
	private MultipartFile nicFile;
	
	@AssertTrue(message = "Must be at least 18 years old")
	public boolean isAdult() {
		return dateOfBirth == null || !dateOfBirth.isAfter(LocalDate.now().minusYears(18));
	}
	
	@AssertTrue(message = "Phone numbers must not repeat")
	public boolean isPhoneNumbersUnique() {
		return ValidationUtil.hasNoRepeats(phoneNumbers);
	}
	
	@AssertTrue(message = "Addresses must not repeat")
	public boolean isAddressesUnique() {
		return ValidationUtil.hasNoRepeats(addresses);
	}
	
}
