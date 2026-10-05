package lexflow.exception.finance;

import lexflow.exception.BusinessRuleViolationException;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
public class InvalidTransactionAmountException extends BusinessRuleViolationException {
	
	private final BigDecimal amount;
	
	public InvalidTransactionAmountException(BigDecimal amount) {
		super(String.format("Transaction amount must be greater than zero, but it is %,.2f", amount));
		this.amount = amount;
	}
	
}
