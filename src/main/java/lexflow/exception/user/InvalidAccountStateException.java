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
		super(String.format("User %d cannot be processed: account status must be %s, but it is currently %s", userId, requiredStatus, currentStatus));
		this.userId = userId;
		this.currentStatus = currentStatus;
		this.requiredStatus = requiredStatus;
	}
	
}
