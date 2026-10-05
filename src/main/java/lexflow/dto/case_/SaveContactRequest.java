package lexflow.dto.case_;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class SaveContactRequest {
	
	private String name;
	
	private List<@NotBlank(message = "Each phone number is required") @Pattern(regexp = "0[0-9]{9}", message = "Each phone number must be 10 digits starting with 0") String> phoneNumbers;
	
	private List<@NotBlank(message = "Each email is required") @Email(message = "Each email must be a valid email address") @Size(max = 255, message = "Each email must be at most 255 characters") String> emails;
	
	private List<@NotBlank(message = "Each address is required") @Size(max = 800, message = "Each address must be at most 800 characters") String> addresses;
	
	private String notes;
	
}
