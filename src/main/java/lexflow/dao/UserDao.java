package lexflow.dao;

import lexflow.entity.document.NicDocument;
import lexflow.entity.user.*;
import lexflow.exception.EntityNotFoundException;
import lexflow.exception.PersistenceException;
import lexflow.exception.user.ActiveConsultationAssignmentException;
import lexflow.exception.user.DuplicateActiveAccountException;
import lexflow.exception.user.OpenCaseAssignmentException;
import lexflow.exception.user.OutstandingWalletBalanceException;
import lexflow.util.IdUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class UserDao {
	
	private static final int OUTSTANDING_WALLET_BALANCE_ERROR_NUMBER = 50001;
	private static final int ACTIVE_CONSULTATION_ASSIGNMENT_ERROR_NUMBER = 50002;
	private static final int OPEN_CASE_ASSIGNMENT_ERROR_NUMBER = 50003;
	private static final int UNIQUE_INDEX_VIOLATION_ERROR_NUMBER = 2601;
	
	private static final String SELECT_COLUMNS = """
												 user_id,
												 role,
												 user_code,
												 email,
												 password,
												 name,
												 date_of_birth,
												 gender,
												 nic_number,
												 account_status,
												 registered_at,
												 bar_number,
												 wallet_balance
												 """;
	
	private final DataSource dataSource;
	private final DocumentDao documentDao;
	
	// ---- Create ----
	
	public User insert(User user) {
		String sql = """
					 INSERT INTO users (role, email, password, name, date_of_birth, gender, nic_number, account_status, registered_at, bar_number, wallet_balance)
					 VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
					 """;
		
		String barNumber = null;
		BigDecimal walletBalance = null;
		String userCode;
		
		if (user instanceof Partner partner) {
			barNumber = partner.getBarNumber();
		} else if (user instanceof Associate associate) {
			barNumber = associate.getBarNumber();
		} else if (user instanceof Client client) {
			walletBalance = client.getWalletBalance();
		}
		
		Connection connection = DataSourceUtils.getConnection(dataSource);
		
		try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
			statement.setString(1, user.getRole().name());
			statement.setString(2, user.getEmail());
			statement.setString(3, user.getPassword());
			statement.setString(4, user.getName());
			statement.setDate(5, Date.valueOf(user.getDateOfBirth()));
			statement.setString(6, user.getGender().name());
			statement.setString(7, user.getNicNumber());
			statement.setString(8, user.getAccountStatus().name());
			statement.setTimestamp(9, Timestamp.valueOf(user.getRegisteredAt()));
			
			if (barNumber != null) {
				statement.setString(10, barNumber);
			} else {
				statement.setNull(10, Types.VARCHAR);
			}
			
			if (walletBalance != null) {
				statement.setBigDecimal(11, walletBalance);
			} else {
				statement.setNull(11, Types.DECIMAL);
			}
			
			statement.executeUpdate();
			
			try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
				
				if (!generatedKeys.next()) {
					throw new PersistenceException("generating user ID");
				}
				
				Long generatedId = generatedKeys.getLong(1);
				user.setUserId(generatedId);
				userCode = IdUtil.generateUserCode(user.getRole(), generatedId);
			}
			
		} catch (SQLException e) {
			
			if (e.getErrorCode() == UNIQUE_INDEX_VIOLATION_ERROR_NUMBER) {
				throw toDuplicateActiveAccountException(e, user);
			}
			
			throw new PersistenceException("inserting user", e);
		} finally {
			DataSourceUtils.releaseConnection(connection, dataSource);
		}
		
		sql = """
			  UPDATE users
			  SET user_code = ?
			  WHERE user_id = ?
			  """;
		
		connection = DataSourceUtils.getConnection(dataSource);
		
		try (PreparedStatement statement = connection.prepareStatement(sql)) {
			statement.setString(1, userCode);
			statement.setLong(2, user.getUserId());
			
			statement.executeUpdate();
		} catch (SQLException e) {
			throw new PersistenceException("setting user code", e);
		} finally {
			DataSourceUtils.releaseConnection(connection, dataSource);
		}
		
		user.setUserCode(userCode);
		
		insertPhoneNumbers(user.getUserId(), user.getPhoneNumbers());
		insertAddresses(user.getUserId(), user.getAddresses());
		
		return user;
	}
	
	// ---- Read ----
	
	public User findById(Long userId) {
		String sql = """
					 SELECT %s
					 FROM users
					 WHERE user_id = ?
					 """.formatted(SELECT_COLUMNS);
		
		Connection connection = DataSourceUtils.getConnection(dataSource);
		
		try (PreparedStatement statement = connection.prepareStatement(sql)) {
			statement.setLong(1, userId);
			
			try (ResultSet resultSet = statement.executeQuery()) {
				
				if (resultSet.next()) {
					return mapRow(resultSet);
				}
				
				throw new EntityNotFoundException("User", userId.toString());
			}
			
		} catch (SQLException e) {
			throw new PersistenceException("finding user by ID", e);
		} finally {
			DataSourceUtils.releaseConnection(connection, dataSource);
		}
		
	}
	
	public User findByEmail(String email) {
		String sql = """
					 SELECT %s
					 FROM users
					 WHERE email = ?
					   AND account_status = 'ACTIVE'
					 """.formatted(SELECT_COLUMNS);
		
		Connection connection = DataSourceUtils.getConnection(dataSource);
		
		try (PreparedStatement statement = connection.prepareStatement(sql)) {
			statement.setString(1, email);
			
			try (ResultSet resultSet = statement.executeQuery()) {
				
				if (resultSet.next()) {
					return mapRow(resultSet);
				}
				
				throw new EntityNotFoundException("User", email);
			}
			
		} catch (SQLException e) {
			throw new PersistenceException("finding user by email", e);
		} finally {
			DataSourceUtils.releaseConnection(connection, dataSource);
		}
		
	}
	
	public User findByUserCode(String userCode) {
		String sql = """
					 SELECT %s
					 FROM users
					 WHERE user_code = ?
					 """.formatted(SELECT_COLUMNS);
		
		Connection connection = DataSourceUtils.getConnection(dataSource);
		
		try (PreparedStatement statement = connection.prepareStatement(sql)) {
			statement.setString(1, userCode);
			
			try (ResultSet resultSet = statement.executeQuery()) {
				
				if (resultSet.next()) {
					return mapRow(resultSet);
				}
				
				throw new EntityNotFoundException("User", userCode);
			}
			
		} catch (SQLException e) {
			throw new PersistenceException("finding user by user code", e);
		} finally {
			DataSourceUtils.releaseConnection(connection, dataSource);
		}
		
	}
	
	public User findByIdAndRole(Long userId, Role role) {
		String sql = """
					 SELECT %s
					 FROM users
					 WHERE user_id = ?
					   AND role = ?
					 """.formatted(SELECT_COLUMNS);
		
		Connection connection = DataSourceUtils.getConnection(dataSource);
		
		try (PreparedStatement statement = connection.prepareStatement(sql)) {
			statement.setLong(1, userId);
			statement.setString(2, role.name());
			
			try (ResultSet resultSet = statement.executeQuery()) {
				
				if (resultSet.next()) {
					return mapRow(resultSet);
				}
				
				throw new EntityNotFoundException(role.name(), userId.toString());
			}
			
		} catch (SQLException e) {
			throw new PersistenceException("finding user by ID and role", e);
		} finally {
			DataSourceUtils.releaseConnection(connection, dataSource);
		}
		
	}
	
	public List<User> findActiveByRole(Role role) {
		String sql = """
					 SELECT %s
					 FROM users
					 WHERE role = ?
					   AND account_status = 'ACTIVE'
					 ORDER BY user_id ASC
					 """.formatted(SELECT_COLUMNS);
		
		return findUsers(sql, role);
	}
	
	public List<User> findByRole(Role role) {
		String sql = """
					 SELECT %s
					 FROM users
					 WHERE role = ?
					 ORDER BY account_status ASC,
					          user_id ASC
					 """.formatted(SELECT_COLUMNS);
		
		return findUsers(sql, role);
	}
	
	public boolean existsDeleted(Role role, String email, String nicNumber) {
		String sql = """
					 SELECT 1
					 FROM users
					 WHERE role = ?
					   AND account_status = 'DELETED'
					   AND (email = ? OR nic_number = ?)
					 """;
		
		Connection connection = DataSourceUtils.getConnection(dataSource);
		
		try (PreparedStatement statement = connection.prepareStatement(sql)) {
			statement.setString(1, role.name());
			statement.setString(2, email);
			statement.setString(3, nicNumber);
			
			try (ResultSet resultSet = statement.executeQuery()) {
				return resultSet.next();
			}
			
		} catch (SQLException e) {
			throw new PersistenceException("checking for a deleted account", e);
		} finally {
			DataSourceUtils.releaseConnection(connection, dataSource);
		}
		
	}
	
	private List<User> findUsers(String sql, Role role) {
		List<User> users = new ArrayList<>();
		
		Connection connection = DataSourceUtils.getConnection(dataSource);
		
		try (PreparedStatement statement = connection.prepareStatement(sql)) {
			statement.setString(1, role.name());
			
			try (ResultSet resultSet = statement.executeQuery()) {
				
				while (resultSet.next()) {
					users.add(mapRow(resultSet));
				}
				
			}
			
		} catch (SQLException e) {
			throw new PersistenceException("finding users by role", e);
		} finally {
			DataSourceUtils.releaseConnection(connection, dataSource);
		}
		
		return users;
	}
	
	// ---- Update ----
	
	public User update(User user) {
		String sql = """
					 UPDATE users
					 SET password       = ?,
					     name           = ?,
					     date_of_birth  = ?,
					     gender         = ?,
					     bar_number     = ?,
					     wallet_balance = ?
					 WHERE user_id = ?
					 """;
		
		String barNumber = null;
		BigDecimal walletBalance = null;
		
		if (user instanceof Partner partner) {
			barNumber = partner.getBarNumber();
		} else if (user instanceof Associate associate) {
			barNumber = associate.getBarNumber();
		} else if (user instanceof Client client) {
			walletBalance = client.getWalletBalance();
		}
		
		Connection connection = DataSourceUtils.getConnection(dataSource);
		
		try (PreparedStatement statement = connection.prepareStatement(sql)) {
			statement.setString(1, user.getPassword());
			statement.setString(2, user.getName());
			statement.setDate(3, Date.valueOf(user.getDateOfBirth()));
			statement.setString(4, user.getGender().name());
			
			if (barNumber != null) {
				statement.setString(5, barNumber);
			} else {
				statement.setNull(5, Types.VARCHAR);
			}
			
			if (walletBalance != null) {
				statement.setBigDecimal(6, walletBalance);
			} else {
				statement.setNull(6, Types.DECIMAL);
			}
			
			statement.setLong(7, user.getUserId());
			
			statement.executeUpdate();
		} catch (SQLException e) {
			throw new PersistenceException("updating user", e);
		} finally {
			DataSourceUtils.releaseConnection(connection, dataSource);
		}
		
		replacePhoneNumbers(user.getUserId(), user.getPhoneNumbers());
		replaceAddresses(user.getUserId(), user.getAddresses());
		
		return user;
	}
	
	public void reactivateById(Long userId, String password) {
		String sql = """
					 UPDATE users
					 SET account_status = 'ACTIVE',
					     password       = ?
					 WHERE user_id = ?
					 """;
		
		Connection connection = DataSourceUtils.getConnection(dataSource);
		
		try (PreparedStatement statement = connection.prepareStatement(sql)) {
			statement.setString(1, password);
			statement.setLong(2, userId);
			
			statement.executeUpdate();
		} catch (SQLException e) {
			
			if (e.getErrorCode() == UNIQUE_INDEX_VIOLATION_ERROR_NUMBER) {
				throw toDuplicateActiveAccountException(e, findById(userId));
			}
			
			throw new PersistenceException("reactivating user", e);
		} finally {
			DataSourceUtils.releaseConnection(connection, dataSource);
		}
		
	}
	
	// ---- Delete ----
	
	public void deleteById(Long userId) {
		String sql = """
					 UPDATE users
					 SET account_status = 'DELETED'
					 WHERE user_id = ?
					 """;
		
		Connection connection = DataSourceUtils.getConnection(dataSource);
		
		try (PreparedStatement statement = connection.prepareStatement(sql)) {
			statement.setLong(1, userId);
			
			statement.executeUpdate();
		} catch (SQLException e) {
			
			if (e.getErrorCode() == OUTSTANDING_WALLET_BALANCE_ERROR_NUMBER && findById(userId) instanceof Client client) {
				throw new OutstandingWalletBalanceException(userId, client.getWalletBalance());
			} else if (e.getErrorCode() == OPEN_CASE_ASSIGNMENT_ERROR_NUMBER) {
				throw new OpenCaseAssignmentException(userId, findOpenCaseCodes(userId));
			} else if (e.getErrorCode() == ACTIVE_CONSULTATION_ASSIGNMENT_ERROR_NUMBER) {
				throw new ActiveConsultationAssignmentException(userId, findActiveConsultationCodes(userId));
			}
			
			throw new PersistenceException("deleting user", e);
		} finally {
			DataSourceUtils.releaseConnection(connection, dataSource);
		}
		
	}
	
	// ---- Private Helpers ----
	
	private List<String> findOpenCaseCodes(Long userId) {
		String sql = """
					 SELECT case_code
					 FROM cases
					 WHERE case_status = 'OPEN'
					   AND (client_id = ? OR associate_id = ? OR paralegal_id = ?)
					 ORDER BY case_id
					 """;
		
		return findCodesForUser(sql, userId, "finding open cases blocking a user deletion");
	}
	
	private List<String> findActiveConsultationCodes(Long userId) {
		String sql = """
					 SELECT cn.consultation_code
					 FROM consultations cn
					 WHERE (cn.consultation_status IN ('PENDING', 'ACCEPTED_BY_PARTNER', 'SCHEDULED') OR (cn.consultation_status = 'ACCEPTED_BY_ASSOCIATE' AND NOT EXISTS (SELECT 1
					                                                                                                                                                       FROM cases cs
					                                                                                                                                                       WHERE cs.consultation_id = cn.consultation_id)))
					   AND (cn.client_id = ? OR cn.associate_id = ? OR cn.paralegal_id = ?)
					 ORDER BY cn.consultation_id
					 """;
		
		return findCodesForUser(sql, userId, "finding active consultations blocking a user deletion");
	}
	
	private List<String> findCodesForUser(String sql, Long userId, String operation) {
		List<String> codes = new ArrayList<>();
		
		Connection connection = DataSourceUtils.getConnection(dataSource);
		
		try (PreparedStatement statement = connection.prepareStatement(sql)) {
			statement.setLong(1, userId);
			statement.setLong(2, userId);
			statement.setLong(3, userId);
			
			try (ResultSet resultSet = statement.executeQuery()) {
				
				while (resultSet.next()) {
					codes.add(resultSet.getString(1));
				}
				
			}
			
		} catch (SQLException e) {
			throw new PersistenceException(operation, e);
		} finally {
			DataSourceUtils.releaseConnection(connection, dataSource);
		}
		
		return codes;
	}
	
	private RuntimeException toDuplicateActiveAccountException(SQLException e, User user) {
		String message = e.getMessage();
		
		if (message.contains("ux_users_email")) {
			return new DuplicateActiveAccountException("email", user.getEmail());
		} else if (message.contains("ux_users_nic_number")) {
			return new DuplicateActiveAccountException("NIC number", user.getNicNumber());
		} else if (message.contains("ux_users_bar_number") && user instanceof Partner partner) {
			return new DuplicateActiveAccountException("bar number", partner.getBarNumber());
		} else if (message.contains("ux_users_bar_number") && user instanceof Associate associate) {
			return new DuplicateActiveAccountException("bar number", associate.getBarNumber());
		}
		
		return new PersistenceException("saving user", e);
	}
	
	private void insertPhoneNumbers(Long userId, List<String> phoneNumbers) {
		
		if (phoneNumbers == null) {
			return;
		}
		
		String sql = """
					 INSERT INTO user_phone_numbers (user_id, phone_number)
					 VALUES (?, ?)
					 """;
		
		Connection connection = DataSourceUtils.getConnection(dataSource);
		
		try (PreparedStatement statement = connection.prepareStatement(sql)) {
			
			for (String phoneNumber : phoneNumbers) {
				
				if (phoneNumber == null || phoneNumber.isBlank()) {
					continue;
				}
				
				statement.setLong(1, userId);
				statement.setString(2, phoneNumber);
				
				statement.executeUpdate();
			}
			
		} catch (SQLException e) {
			throw new PersistenceException("inserting phone numbers", e);
		} finally {
			DataSourceUtils.releaseConnection(connection, dataSource);
		}
		
	}
	
	private void insertAddresses(Long userId, List<String> addresses) {
		
		if (addresses == null) {
			return;
		}
		
		String sql = """
					 INSERT INTO user_addresses (user_id, address)
					 VALUES (?, ?)
					 """;
		
		Connection connection = DataSourceUtils.getConnection(dataSource);
		
		try (PreparedStatement statement = connection.prepareStatement(sql)) {
			
			for (String address : addresses) {
				
				if (address == null || address.isBlank()) {
					continue;
				}
				
				statement.setLong(1, userId);
				statement.setString(2, address);
				
				statement.executeUpdate();
			}
			
		} catch (SQLException e) {
			throw new PersistenceException("inserting addresses", e);
		} finally {
			DataSourceUtils.releaseConnection(connection, dataSource);
		}
		
	}
	
	private List<String> findPhoneNumbers(Long userId) {
		String sql = """
					 SELECT phone_number
					 FROM user_phone_numbers
					 WHERE user_id = ?
					 """;
		
		List<String> phoneNumbers = new ArrayList<>();
		
		Connection connection = DataSourceUtils.getConnection(dataSource);
		
		try (PreparedStatement statement = connection.prepareStatement(sql)) {
			statement.setLong(1, userId);
			
			try (ResultSet resultSet = statement.executeQuery()) {
				
				while (resultSet.next()) {
					phoneNumbers.add(resultSet.getString("phone_number"));
				}
				
			}
			
		} catch (SQLException e) {
			throw new PersistenceException("finding phone numbers", e);
		} finally {
			DataSourceUtils.releaseConnection(connection, dataSource);
		}
		
		return phoneNumbers;
	}
	
	private List<String> findAddresses(Long userId) {
		String sql = """
					 SELECT address
					 FROM user_addresses
					 WHERE user_id = ?
					 """;
		
		List<String> addresses = new ArrayList<>();
		
		Connection connection = DataSourceUtils.getConnection(dataSource);
		
		try (PreparedStatement statement = connection.prepareStatement(sql)) {
			statement.setLong(1, userId);
			
			try (ResultSet resultSet = statement.executeQuery()) {
				
				while (resultSet.next()) {
					addresses.add(resultSet.getString("address"));
				}
				
			}
			
		} catch (SQLException e) {
			throw new PersistenceException("finding addresses", e);
		} finally {
			DataSourceUtils.releaseConnection(connection, dataSource);
		}
		
		return addresses;
	}
	
	private void replacePhoneNumbers(Long userId, List<String> phoneNumbers) {
		String sql = """
					 DELETE
					 FROM user_phone_numbers
					 WHERE user_id = ?
					 """;
		
		Connection connection = DataSourceUtils.getConnection(dataSource);
		
		try (PreparedStatement statement = connection.prepareStatement(sql)) {
			statement.setLong(1, userId);
			
			statement.executeUpdate();
		} catch (SQLException e) {
			throw new PersistenceException("clearing phone numbers", e);
		} finally {
			DataSourceUtils.releaseConnection(connection, dataSource);
		}
		
		insertPhoneNumbers(userId, phoneNumbers);
	}
	
	private void replaceAddresses(Long userId, List<String> addresses) {
		String sql = """
					 DELETE
					 FROM user_addresses
					 WHERE user_id = ?
					 """;
		
		Connection connection = DataSourceUtils.getConnection(dataSource);
		
		try (PreparedStatement statement = connection.prepareStatement(sql)) {
			statement.setLong(1, userId);
			
			statement.executeUpdate();
		} catch (SQLException e) {
			throw new PersistenceException("clearing addresses", e);
		} finally {
			DataSourceUtils.releaseConnection(connection, dataSource);
		}
		
		insertAddresses(userId, addresses);
	}
	
	private User mapRow(ResultSet resultSet) throws SQLException {
		Long userId = resultSet.getLong("user_id");
		Role role = Role.valueOf(resultSet.getString("role"));
		String userCode = resultSet.getString("user_code");
		String email = resultSet.getString("email");
		String password = resultSet.getString("password");
		String name = resultSet.getString("name");
		LocalDate dateOfBirth = resultSet.getDate("date_of_birth").toLocalDate();
		Gender gender = Gender.valueOf(resultSet.getString("gender"));
		List<String> addresses = findAddresses(userId);
		List<String> phoneNumbers = findPhoneNumbers(userId);
		String nicNumber = resultSet.getString("nic_number");
		NicDocument nicDocument = documentDao.findNicDocumentByUserId(userId);
		AccountStatus accountStatus = AccountStatus.valueOf(resultSet.getString("account_status"));
		LocalDateTime registeredAt = resultSet.getTimestamp("registered_at").toLocalDateTime();
		
		if (role == Role.PARTNER) {
			return new Partner(userId, userCode, email, password, name, dateOfBirth, gender, addresses, phoneNumbers, nicNumber, nicDocument, accountStatus, registeredAt, resultSet.getString("bar_number"), documentDao.findBarDocumentByUserId(userId));
		}
		
		if (role == Role.ASSOCIATE) {
			return new Associate(userId, userCode, email, password, name, dateOfBirth, gender, addresses, phoneNumbers, nicNumber, nicDocument, accountStatus, registeredAt, resultSet.getString("bar_number"), documentDao.findBarDocumentByUserId(userId));
		}
		
		if (role == Role.PARALEGAL) {
			return new Paralegal(userId, userCode, email, password, name, dateOfBirth, gender, addresses, phoneNumbers, nicNumber, nicDocument, accountStatus, registeredAt);
		}
		
		if (role == Role.CLIENT) {
			return new Client(userId, userCode, email, password, name, dateOfBirth, gender, addresses, phoneNumbers, nicNumber, nicDocument, accountStatus, registeredAt, resultSet.getBigDecimal("wallet_balance"));
		}
		
		if (role == Role.CLIENT_RELATIONS_OFFICER) {
			return new ClientRelationsOfficer(userId, userCode, email, password, name, dateOfBirth, gender, addresses, phoneNumbers, nicNumber, nicDocument, accountStatus, registeredAt);
		}
		
		return new FinanceOfficer(userId, userCode, email, password, name, dateOfBirth, gender, addresses, phoneNumbers, nicNumber, nicDocument, accountStatus, registeredAt);
		
	}
	
}
