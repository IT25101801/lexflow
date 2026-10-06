package lexflow.dto.finance;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class WithdrawRequest {
	
	@NotBlank(message = "Client code is required")
	@Pattern(regexp = "CLT-\\d+-\\d{8}", message = "Client code must look like CLT-31-70617268")
	private String clientCode;
	
	@NotNull(message = "Amount is required")
	@DecimalMin(value = "0.01", message = "Amount must be at least 0.01")
	private BigDecimal amount;
	
}
