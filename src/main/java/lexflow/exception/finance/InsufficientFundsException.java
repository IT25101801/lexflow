package lexflow.exception.finance;

import lexflow.exception.BusinessRuleViolationException;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
public class InsufficientFundsException extends BusinessRuleViolationException {
	
	private final Long clientId;
	private final BigDecimal availableAmount;
	private final BigDecimal requiredAmount;
	
	public InsufficientFundsException(Long clientId, BigDecimal availableAmount, BigDecimal requiredAmount) {
		super(String.format("Client %d has insufficient funds: available %,.2f, required %,.2f", clientId, availableAmount, requiredAmount));
		this.clientId = clientId;
		this.availableAmount = availableAmount;
		this.requiredAmount = requiredAmount;
	}
	
}
