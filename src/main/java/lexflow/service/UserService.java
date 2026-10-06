package lexflow.service;

import lexflow.dao.UserDao;
import lexflow.entity.user.*;
import lexflow.exception.EntityAlreadyExistsException;
import lexflow.exception.EntityNotFoundException;
import lexflow.exception.user.DeletedAccountExistsException;
import lexflow.factory.StaffFactory;
import lexflow.state.user.AccountState;
import lexflow.state.user.ActiveAccountState;
import lexflow.state.user.DeletedAccountState;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {
	
	private final UserDao userDao;
	private final DocumentService documentService;
	private final PasswordEncoder passwordEncoder;
	
	// ---- Create ----
	
	@Transactional
	public Client register(String email, String password, String name, LocalDate dateOfBirth, Gender gender, List<String> addresses, List<String> phoneNumbers, String nicNumber, MultipartFile nicFile) {
		
		if (findByEmailOrNull(email) != null) {
			throw new EntityAlreadyExistsException("User", email);
		}
		
		if (userDao.existsDeleted(Role.CLIENT, email, nicNumber)) {
			throw new DeletedAccountExistsException(Role.CLIENT, email, nicNumber);
		}
		
		LocalDateTime registeredAt = LocalDateTime.now();
		String hashedPassword = passwordEncoder.encode(password);
		
		Client client = new Client(email, hashedPassword, name, dateOfBirth, gender, addresses, phoneNumbers, nicNumber, registeredAt);
		
		Client insertedClient = (Client) userDao.insert(client);
		
		documentService.attachNicDocument(insertedClient, nicFile, registeredAt);
		
		return insertedClient;
	}
	
	@Transactional
	public User register(Role role, String email, String password, String name, LocalDate dateOfBirth, Gender gender, List<String> addresses, List<String> phoneNumbers, String nicNumber, String barNumber, MultipartFile nicFile, MultipartFile barFile) {
		
		if (findByEmailOrNull(email) != null) {
			throw new EntityAlreadyExistsException("User", email);
		}
		
		// A rehire into the same role is reactivated, not registered again; a promotion is a different role, so it registers.
		if (userDao.existsDeleted(role, email, nicNumber)) {
			throw new DeletedAccountExistsException(role, email, nicNumber);
		}
		
		LocalDateTime registeredAt = LocalDateTime.now();
		
		String hashedPassword = passwordEncoder.encode(password);
		
		User user = StaffFactory.create(role, email, hashedPassword, name, dateOfBirth, gender, addresses, phoneNumbers, nicNumber, registeredAt, barNumber);
		
		User insertedUser = userDao.insert(user);
		
		documentService.attachNicDocument(insertedUser, nicFile, registeredAt);
		
		if (insertedUser instanceof Partner || insertedUser instanceof Associate) {
			documentService.attachBarDocument(insertedUser, barFile, registeredAt);
		}
		
		return insertedUser;
	}
	
	// ---- Read ----
	
	public User findById(Long userId) {
		return userDao.findById(userId);
	}
	
	public User findByUserCode(String userCode) {
		return userDao.findByUserCode(userCode);
	}
	
	public Client findClientById(Long userId) {
		User user = findById(userId);
		
		if (!(user instanceof Client client)) {
			throw new AccessDeniedException("Not a client.");
		}
		
		return client;
	}
	
	public Client findClientByUserCode(String userCode) {
		User user = findByUserCode(userCode);
		
		if (!(user instanceof Client client) || client.getAccountStatus() == AccountStatus.DELETED) {
			throw new EntityNotFoundException("Client", userCode);
		}
		
		return client;
	}
	
	public List<User> findByRoleAndKeyword(Role role, String keyword) {
		List<User> users = userDao.findByRole(role);
		
		if (keyword == null || keyword.isBlank()) {
			return users;
		}
		
		String lowerKeyword = keyword.toLowerCase();
		List<User> matches = new ArrayList<>();
		
		for (User user : users) {
			
			if (matchesKeyword(user, lowerKeyword)) {
				matches.add(user);
			}
			
		}
		
		return matches;
	}
	
	public List<Associate> findActiveAssociates() {
		List<Associate> associates = new ArrayList<>();
		
		for (User user : userDao.findActiveByRole(Role.ASSOCIATE)) {
			associates.add((Associate) user);
		}
		
		return associates;
	}
	
	public List<Paralegal> findActiveParalegals() {
		List<Paralegal> paralegals = new ArrayList<>();
		
		for (User user : userDao.findActiveByRole(Role.PARALEGAL)) {
			paralegals.add((Paralegal) user);
		}
		
		return paralegals;
	}
	
	private boolean matchesKeyword(User user, String lowerKeyword) {
		String barNumber = null;
		
		if (user instanceof Partner partner) {
			barNumber = partner.getBarNumber();
		} else if (user instanceof Associate associate) {
			barNumber = associate.getBarNumber();
		}
		
		return user.getName().toLowerCase().contains(lowerKeyword)
			   || user.getEmail().toLowerCase().contains(lowerKeyword)
			   || user.getUserCode().toLowerCase().contains(lowerKeyword)
			   || user.getNicNumber().toLowerCase().contains(lowerKeyword)
			   || (barNumber != null && barNumber.toLowerCase().contains(lowerKeyword));
	}
	
	public boolean verifyPassword(User user, String rawPassword) {
		return passwordEncoder.matches(rawPassword, user.getPassword());
	}
	
	// ---- Update ----
	
	@Transactional
	public User updateDetails(User user, String name, LocalDate dateOfBirth, Gender gender, List<String> addresses, List<String> phoneNumbers, MultipartFile nicFile, MultipartFile barFile) {
		documentService.requirePdf(nicFile, "NIC");
		documentService.requirePdf(barFile, "Bar");
		
		user.setName(name);
		user.setDateOfBirth(dateOfBirth);
		user.setGender(gender);
		user.setAddresses(addresses);
		user.setPhoneNumbers(phoneNumbers);
		
		documentService.replaceNicDocument(user, nicFile);
		
		if (user instanceof Partner || user instanceof Associate) {
			documentService.replaceBarDocument(user, barFile);
		}
		
		return userDao.update(user);
	}
	
	public User changePassword(User user, String newPassword) {
		user.setPassword(passwordEncoder.encode(newPassword));
		
		return userDao.update(user);
	}
	
	@Transactional
	public User reactivate(User user, String temporaryPassword) {
		return stateFor(user.getAccountStatus()).reactivate(user, this, temporaryPassword);
	}
	
	// ---- Delete ----
	
	public void delete(User user) {
		userDao.deleteById(user.getUserId());
	}
	
	// ---- Private Helpers ----
	
	private User findByEmailOrNull(String email) {
		
		try {
			return userDao.findByEmail(email);
		} catch (EntityNotFoundException e) {
			return null;
		}
		
	}
	
	private AccountState stateFor(AccountStatus accountStatus) {
		
		if (accountStatus == AccountStatus.ACTIVE) {
			return new ActiveAccountState();
		} else {
			return new DeletedAccountState();
		}
		
	}
	
	// ---- State Transitions (called by AccountState implementations) ----
	
	public User applyReactivation(User user, String temporaryPassword) {
		String hashedPassword = passwordEncoder.encode(temporaryPassword);
		
		userDao.reactivateById(user.getUserId(), hashedPassword);
		
		user.setPassword(hashedPassword);
		user.setAccountStatus(AccountStatus.ACTIVE);
		
		return user;
	}
	
}
