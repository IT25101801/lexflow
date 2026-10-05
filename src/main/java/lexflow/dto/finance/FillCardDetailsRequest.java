package lexflow.dto.finance;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class FillCardDetailsRequest {
	
	@NotNull(message = "Amount is required")
	@DecimalMin(value = "0.01", message = "Amount must be at least 0.01")
	private BigDecimal amount;
	
	@NotBlank(message = "Card number is required")
	@Pattern(regexp = "\\d{16}", message = "Card number must be 16 digits")
	private String cardNumber;
	
	@NotBlank(message = "Cardholder name is required")
	private String cardholder;
	
	@NotBlank(message = "Expiry date is required")
	@Pattern(regexp = "(0[1-9]|1[0-2])/\\d{2}", message = "Expiry date must be in MM/YY format")
	private String expiry;
	
	@NotBlank(message = "CVV is required")
	@Pattern(regexp = "\\d{3}", message = "CVV must be 3 digits")
	private String cvv;
	
}
