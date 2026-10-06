package lexflow.state.user;

import lexflow.entity.user.User;
import lexflow.service.UserService;

public interface AccountState {
	
	User reactivate(User user, UserService userService, String temporaryPassword);
	
	void delete(User user, UserService userService);
	
}
