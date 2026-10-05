package lexflow.factory;

import lexflow.entity.user.*;
import lexflow.exception.user.InvalidStaffRoleException;
import lexflow.exception.user.MissingBarNumberException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class StaffFactory {
	
	private StaffFactory() {
	}
	
	public static User create(Role role, String email, String hashedPassword, String name, LocalDate dateOfBirth, Gender gender, List<String> addresses, List<String> phoneNumbers, String nicNumber, LocalDateTime registeredAt, String barNumber) {
		
		if (role == Role.PARTNER) {
			requireBarNumber(barNumber);
			return new Partner(email, hashedPassword, name, dateOfBirth, gender, addresses, phoneNumbers, nicNumber, registeredAt, barNumber);
		}
		
		if (role == Role.ASSOCIATE) {
			requireBarNumber(barNumber);
			return new Associate(email, hashedPassword, name, dateOfBirth, gender, addresses, phoneNumbers, nicNumber, registeredAt, barNumber);
		}
		
		if (role == Role.PARALEGAL) {
			return new Paralegal(email, hashedPassword, name, dateOfBirth, gender, addresses, phoneNumbers, nicNumber, registeredAt);
		}
		
		if (role == Role.CLIENT_RELATIONS_OFFICER) {
			return new ClientRelationsOfficer(email, hashedPassword, name, dateOfBirth, gender, addresses, phoneNumbers, nicNumber, registeredAt);
		}
		
		if (role == Role.FINANCE_OFFICER) {
			return new FinanceOfficer(email, hashedPassword, name, dateOfBirth, gender, addresses, phoneNumbers, nicNumber, registeredAt);
		}
		
		throw new InvalidStaffRoleException(role);
	}
	
	private static void requireBarNumber(String barNumber) {
		
		if (barNumber == null || barNumber.isBlank()) {
			throw new MissingBarNumberException();
		}
		
	}
	
}
