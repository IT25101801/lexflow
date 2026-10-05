package lexflow.entity.user;

import lexflow.entity.document.NicDocument;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class FinanceOfficer extends User {
	
	public FinanceOfficer(String email, String password, String name, LocalDate dateOfBirth, Gender gender, List<String> addresses, List<String> phoneNumbers, String nicNumber, LocalDateTime registeredAt) {
		super(Role.FINANCE_OFFICER, email, password, name, dateOfBirth, gender, addresses, phoneNumbers, nicNumber, registeredAt);
	}
	
	public FinanceOfficer(Long userId, String userCode, String email, String password, String name, LocalDate dateOfBirth, Gender gender, List<String> addresses, List<String> phoneNumbers, String nicNumber, NicDocument nicDocument, AccountStatus accountStatus, LocalDateTime registeredAt) {
		super(userId, Role.FINANCE_OFFICER, userCode, email, password, name, dateOfBirth, gender, addresses, phoneNumbers, nicNumber, nicDocument, accountStatus, registeredAt);
	}
	
}
