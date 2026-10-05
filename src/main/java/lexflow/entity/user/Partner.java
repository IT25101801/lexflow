package lexflow.entity.user;

import lexflow.entity.document.BarDocument;
import lexflow.entity.document.NicDocument;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class Partner extends User {
	
	private String barNumber;
	private BarDocument barDocument;
	
	public Partner(String email, String password, String name, LocalDate dateOfBirth, Gender gender, List<String> addresses, List<String> phoneNumbers, String nicNumber, LocalDateTime registeredAt, String barNumber) {
		super(Role.PARTNER, email, password, name, dateOfBirth, gender, addresses, phoneNumbers, nicNumber, registeredAt);
		this.barNumber = barNumber;
	}
	
	public Partner(Long userId, String userCode, String email, String password, String name, LocalDate dateOfBirth, Gender gender, List<String> addresses, List<String> phoneNumbers, String nicNumber, NicDocument nicDocument, AccountStatus accountStatus, LocalDateTime registeredAt, String barNumber, BarDocument barDocument) {
		super(userId, Role.PARTNER, userCode, email, password, name, dateOfBirth, gender, addresses, phoneNumbers, nicNumber, nicDocument, accountStatus, registeredAt);
		this.barNumber = barNumber;
		this.barDocument = barDocument;
	}
	
}
