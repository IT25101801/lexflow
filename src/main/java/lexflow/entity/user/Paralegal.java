package lexflow.entity.user;

import lexflow.entity.document.NicDocument;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class Paralegal extends User {
	
	public Paralegal(String email, String password, String name, LocalDate dateOfBirth, Gender gender, List<String> addresses, List<String> phoneNumbers, String nicNumber, LocalDateTime registeredAt) {
		super(Role.PARALEGAL, email, password, name, dateOfBirth, gender, addresses, phoneNumbers, nicNumber, registeredAt);
	}
	
	public Paralegal(Long userId, String userCode, String email, String password, String name, LocalDate dateOfBirth, Gender gender, List<String> addresses, List<String> phoneNumbers, String nicNumber, NicDocument nicDocument, AccountStatus accountStatus, LocalDateTime registeredAt) {
		super(userId, Role.PARALEGAL, userCode, email, password, name, dateOfBirth, gender, addresses, phoneNumbers, nicNumber, nicDocument, accountStatus, registeredAt);
	}
	
}
