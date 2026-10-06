package lexflow.exception.user;

import lexflow.exception.BusinessRuleViolationException;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
public class OutstandingWalletBalanceException extends BusinessRuleViolationException {
	
	private final Long clientId;
	private final BigDecimal walletBalance;
	
	public OutstandingWalletBalanceException(Long clientId, BigDecimal walletBalance) {
		super(String.format("This client cannot be deleted: the trust account still holds %,.2f, which the client must collect from the finance officer first", walletBalance));
		this.clientId = clientId;
		this.walletBalance = walletBalance;
	}
	
}
