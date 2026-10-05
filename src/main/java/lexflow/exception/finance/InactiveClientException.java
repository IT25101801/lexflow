package lexflow.exception.finance;

import lexflow.exception.BusinessRuleViolationException;
import lombok.Getter;

@Getter
public class InactiveClientException extends BusinessRuleViolationException {
	
	private final Long clientId;
	
	public InactiveClientException(Long clientId) {
		super(String.format("Client %d cannot make a transaction: the account does not exist or is not active", clientId));
		this.clientId = clientId;
	}
	
}
