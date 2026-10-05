package lexflow.entity.user;

import lexflow.entity.document.NicDocument;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class Client extends User {
	
	private BigDecimal walletBalance;
	
	public Client(String email, String password, String name, LocalDate dateOfBirth, Gender gender, List<String> addresses, List<String> phoneNumbers, String nicNumber, LocalDateTime registeredAt) {
		super(Role.CLIENT, email, password, name, dateOfBirth, gender, addresses, phoneNumbers, nicNumber, registeredAt);
		this.walletBalance = BigDecimal.ZERO;
	}
	
	public Client(Long userId, String userCode, String email, String password, String name, LocalDate dateOfBirth, Gender gender, List<String> addresses, List<String> phoneNumbers, String nicNumber, NicDocument nicDocument, AccountStatus accountStatus, LocalDateTime registeredAt, BigDecimal walletBalance) {
		super(userId, Role.CLIENT, userCode, email, password, name, dateOfBirth, gender, addresses, phoneNumbers, nicNumber, nicDocument, accountStatus, registeredAt);
		this.walletBalance = walletBalance;
	}
	
}
