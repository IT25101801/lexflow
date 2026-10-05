package lexflow.state.user;

import lexflow.entity.user.AccountStatus;
import lexflow.entity.user.User;
import lexflow.exception.user.InvalidAccountStateException;
import lexflow.service.UserService;

public class ActiveAccountState implements AccountState {
	
	@Override
	public User reactivate(User user, UserService userService, String temporaryPassword) {
		throw new InvalidAccountStateException(user.getUserId(), user.getAccountStatus(), AccountStatus.DELETED);
	}
	
}
