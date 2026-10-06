package lexflow.exception.user;

import lexflow.entity.user.AccountStatus;
import lexflow.exception.InvalidStateException;
import lombok.Getter;

@Getter
public class InvalidAccountStateException extends InvalidStateException {
	
	private final Long userId;
	private final AccountStatus currentStatus;
	private final AccountStatus requiredStatus;
	
	public InvalidAccountStateException(Long userId, AccountStatus currentStatus, AccountStatus requiredStatus) {
		super(String.format("This account is %s, but it must be %s to do this.", currentStatus.name().toLowerCase().replace('_', ' '), requiredStatus.name().toLowerCase().replace('_', ' ')));
		this.userId = userId;
		this.currentStatus = currentStatus;
		this.requiredStatus = requiredStatus;
	}
	
}
