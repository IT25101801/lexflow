package lexflow.state.user;

import lexflow.entity.user.User;
import lexflow.service.UserService;

public class DeletedAccountState implements AccountState {
	
	@Override
	public User reactivate(User user, UserService userService, String temporaryPassword) {
		return userService.applyReactivation(user, temporaryPassword);
	}
	
}
