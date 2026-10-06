package lexflow.state.user;

import lexflow.entity.user.AccountStatus;
import lexflow.entity.user.User;
import lexflow.exception.user.InvalidAccountStateException;
import lexflow.service.UserService;

public class DeletedAccountState implements AccountState {
	
	@Override
	public User reactivate(User user, UserService userService, String temporaryPassword) {
		return userService.applyReactivation(user, temporaryPassword);
	}
	
	@Override
	public void delete(User user, UserService userService) {
		throw new InvalidAccountStateException(user.getUserId(), user.getAccountStatus(), AccountStatus.ACTIVE);
	}
	
}
