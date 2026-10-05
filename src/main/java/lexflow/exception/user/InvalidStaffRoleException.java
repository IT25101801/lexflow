package lexflow.exception.user;

import lexflow.entity.user.Role;
import lexflow.exception.BusinessRuleViolationException;
import lombok.Getter;

@Getter
public class InvalidStaffRoleException extends BusinessRuleViolationException {
	
	private final Role role;
	
	public InvalidStaffRoleException(Role role) {
		super(String.format("%s is not a staff role: clients register through the client registration page", role));
		this.role = role;
	}
	
}
