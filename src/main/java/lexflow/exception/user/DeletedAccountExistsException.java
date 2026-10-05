package lexflow.exception.user;

import lexflow.entity.user.Role;
import lexflow.exception.BusinessRuleViolationException;
import lombok.Getter;

@Getter
public class DeletedAccountExistsException extends BusinessRuleViolationException {
	
	private final Role role;
	private final String email;
	private final String nicNumber;
	
	public DeletedAccountExistsException(Role role, String email, String nicNumber) {
		super(String.format("A deleted %s account already uses email %s or NIC number %s; it must be reactivated instead of registered again", role, email, nicNumber));
		this.role = role;
		this.email = email;
		this.nicNumber = nicNumber;
	}
	
}
