package lexflow.entity.user;

import lexflow.entity.document.NicDocument;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public abstract class User {
	
	protected Long userId;
	protected final Role role;
	protected String userCode;
	protected final String email;
	protected String password;
	protected String name;
	protected LocalDate dateOfBirth;
	protected Gender gender;
	protected List<String> addresses;
	protected List<String> phoneNumbers;
	protected final String nicNumber;
	protected NicDocument nicDocument;
	protected AccountStatus accountStatus;
	protected final LocalDateTime registeredAt;
	
	protected User(Role role, String email, String password, String name, LocalDate dateOfBirth, Gender gender, List<String> addresses, List<String> phoneNumbers, String nicNumber, LocalDateTime registeredAt) {
		this.role = role;
		this.email = email;
		this.password = password;
		this.name = name;
		this.dateOfBirth = dateOfBirth;
		this.gender = gender;
		this.addresses = addresses;
		this.phoneNumbers = phoneNumbers;
		this.nicNumber = nicNumber;
		this.accountStatus = AccountStatus.ACTIVE;
		this.registeredAt = registeredAt;
	}
	
	protected User(Long userId, Role role, String userCode, String email, String password, String name, LocalDate dateOfBirth, Gender gender, List<String> addresses, List<String> phoneNumbers, String nicNumber, NicDocument nicDocument, AccountStatus accountStatus, LocalDateTime registeredAt) {
		this.userId = userId;
		this.role = role;
		this.userCode = userCode;
		this.email = email;
		this.password = password;
		this.name = name;
		this.dateOfBirth = dateOfBirth;
		this.gender = gender;
		this.addresses = addresses;
		this.phoneNumbers = phoneNumbers;
		this.nicNumber = nicNumber;
		this.nicDocument = nicDocument;
		this.accountStatus = accountStatus;
		this.registeredAt = registeredAt;
	}
	
}
