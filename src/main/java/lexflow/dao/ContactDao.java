package lexflow.dao;

import lexflow.entity.case_.Contact;
import lexflow.exception.EntityNotFoundException;
import lexflow.exception.PersistenceException;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class ContactDao {
	
	private static final String SELECT_COLUMNS = """
												 contact_id,
												 case_id,
												 name,
												 notes,
												 added_at
												 """;
	
	private final DataSource dataSource;
	
	// Create
	public Contact insert(Contact contact) {
		String sql = """
					 INSERT INTO contacts (case_id, name, notes, added_at)
					 VALUES (?, ?, ?, ?)
					 """;
		Connection connection = DataSourceUtils.getConnection(dataSource);
		
		try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
			statement.setLong(1, contact.getCaseId());
			statement.setString(2, contact.getName());
			statement.setString(3, contact.getNotes());
			statement.setTimestamp(4, Timestamp.valueOf(contact.getAddedAt()));
			
			statement.executeUpdate();
			
			try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
				
				if (!generatedKeys.next()) {
					throw new PersistenceException("generating contact ID");
				}
				
				contact.setContactId(generatedKeys.getLong(1));
			}
			
		} catch (SQLException e) {
			throw new PersistenceException("inserting contact", e);
		} finally {
			DataSourceUtils.releaseConnection(connection, dataSource);
		}
		
		insertPhoneNumbers(contact.getContactId(), contact.getPhoneNumbers());
		insertEmails(contact.getContactId(), contact.getEmails());
		insertAddresses(contact.getContactId(), contact.getAddresses());
		
		return contact;
	}
	
	// Read
	public Contact findById(Long contactId) {
		String sql = """
					 SELECT %s
					 FROM contacts
					 WHERE contact_id = ?
					 """.formatted(SELECT_COLUMNS);
		Connection connection = DataSourceUtils.getConnection(dataSource);
		
		try (PreparedStatement statement = connection.prepareStatement(sql)) {
			statement.setLong(1, contactId);
			
			try (ResultSet resultSet = statement.executeQuery()) {
				
				if (resultSet.next()) {
					return mapRow(resultSet);
				}
				
				throw new EntityNotFoundException("Contact", contactId.toString());
			}
			
		} catch (SQLException e) {
			throw new PersistenceException("finding contact by ID", e);
		} finally {
			DataSourceUtils.releaseConnection(connection, dataSource);
		}
		
	}
	
	public List<Contact> findByCaseId(Long caseId) {
		String sql = """
					 SELECT %s
					 FROM contacts
					 WHERE case_id = ?
					 ORDER BY added_at DESC
					 """.formatted(SELECT_COLUMNS);
		List<Contact> contacts = new ArrayList<>();
		Connection connection = DataSourceUtils.getConnection(dataSource);
		
		try (PreparedStatement statement = connection.prepareStatement(sql)) {
			statement.setLong(1, caseId);
			
			try (ResultSet resultSet = statement.executeQuery()) {
				
				while (resultSet.next()) {
					contacts.add(mapRow(resultSet));
				}
				
			}
			
		} catch (SQLException e) {
			throw new PersistenceException("finding contacts by case", e);
		} finally {
			DataSourceUtils.releaseConnection(connection, dataSource);
		}
		
		return contacts;
	}
	
	// Update
	public Contact update(Contact contact) {
		String sql = """
					 UPDATE contacts
					 SET name  = ?,
					     notes = ?
					 WHERE contact_id = ?
					 """;
		Connection connection = DataSourceUtils.getConnection(dataSource);
		
		try (PreparedStatement statement = connection.prepareStatement(sql)) {
			statement.setString(1, contact.getName());
			statement.setString(2, contact.getNotes());
			statement.setLong(3, contact.getContactId());
			
			statement.executeUpdate();
		} catch (SQLException e) {
			throw new PersistenceException("updating contact", e);
		} finally {
			DataSourceUtils.releaseConnection(connection, dataSource);
		}
		
		deletePhoneNumbers(contact.getContactId());
		deleteEmails(contact.getContactId());
		deleteAddresses(contact.getContactId());
		insertPhoneNumbers(contact.getContactId(), contact.getPhoneNumbers());
		insertEmails(contact.getContactId(), contact.getEmails());
		insertAddresses(contact.getContactId(), contact.getAddresses());
		
		return contact;
	}
	
	// Private Helpers
	private void insertPhoneNumbers(Long contactId, List<String> phoneNumbers) {
		
		if (phoneNumbers == null) {
			return;
		}
		
		String sql = """
					 INSERT INTO contact_phone_numbers (contact_id, phone_number)
					 VALUES (?, ?)
					 """;
		Connection connection = DataSourceUtils.getConnection(dataSource);
		
		try (PreparedStatement statement = connection.prepareStatement(sql)) {
			
			for (String phoneNumber : phoneNumbers) {
				
				if (phoneNumber == null || phoneNumber.isBlank()) {
					continue;
				}
				
				statement.setLong(1, contactId);
				statement.setString(2, phoneNumber);
				
				statement.executeUpdate();
			}
			
		} catch (SQLException e) {
			throw new PersistenceException("inserting contact phone numbers", e);
		} finally {
			DataSourceUtils.releaseConnection(connection, dataSource);
		}
		
	}
	
	private void insertEmails(Long contactId, List<String> emails) {
		
		if (emails == null) {
			return;
		}
		
		String sql = """
					 INSERT INTO contact_emails (contact_id, email)
					 VALUES (?, ?)
					 """;
		Connection connection = DataSourceUtils.getConnection(dataSource);
		
		try (PreparedStatement statement = connection.prepareStatement(sql)) {
			
			for (String email : emails) {
				
				if (email == null || email.isBlank()) {
					continue;
				}
				
				statement.setLong(1, contactId);
				statement.setString(2, email);
				
				statement.executeUpdate();
			}
			
		} catch (SQLException e) {
			throw new PersistenceException("inserting contact emails", e);
		} finally {
			DataSourceUtils.releaseConnection(connection, dataSource);
		}
		
	}
	
	private void insertAddresses(Long contactId, List<String> addresses) {
		
		if (addresses == null) {
			return;
		}
		
		String sql = """
					 INSERT INTO contact_addresses (contact_id, address)
					 VALUES (?, ?)
					 """;
		Connection connection = DataSourceUtils.getConnection(dataSource);
		
		try (PreparedStatement statement = connection.prepareStatement(sql)) {
			
			for (String address : addresses) {
				
				if (address == null || address.isBlank()) {
					continue;
				}
				
				statement.setLong(1, contactId);
				statement.setString(2, address);
				
				statement.executeUpdate();
			}
			
		} catch (SQLException e) {
			throw new PersistenceException("inserting contact addresses", e);
		} finally {
			DataSourceUtils.releaseConnection(connection, dataSource);
		}
		
	}
	
	private Contact mapRow(ResultSet resultSet) throws SQLException {
		Long contactId = resultSet.getLong("contact_id");
		Long caseId = resultSet.getLong("case_id");
		String name = resultSet.getString("name");
		List<String> phoneNumbers = findPhoneNumbers(contactId);
		List<String> emails = findEmails(contactId);
		List<String> addresses = findAddresses(contactId);
		String notes = resultSet.getString("notes");
		LocalDateTime addedAt = resultSet.getTimestamp("added_at").toLocalDateTime();
		
		return new Contact(contactId, caseId, name, phoneNumbers, emails, addresses, notes, addedAt);
	}
	
	private List<String> findPhoneNumbers(Long contactId) {
		String sql = """
					 SELECT phone_number
					 FROM contact_phone_numbers
					 WHERE contact_id = ?
					 """;
		List<String> phoneNumbers = new ArrayList<>();
		Connection connection = DataSourceUtils.getConnection(dataSource);
		
		try (PreparedStatement statement = connection.prepareStatement(sql)) {
			statement.setLong(1, contactId);
			
			try (ResultSet resultSet = statement.executeQuery()) {
				
				while (resultSet.next()) {
					phoneNumbers.add(resultSet.getString("phone_number"));
				}
				
			}
			
		} catch (SQLException e) {
			throw new PersistenceException("finding contact phone numbers", e);
		} finally {
			DataSourceUtils.releaseConnection(connection, dataSource);
		}
		
		return phoneNumbers;
	}
	
	private List<String> findEmails(Long contactId) {
		String sql = """
					 SELECT email
					 FROM contact_emails
					 WHERE contact_id = ?
					 """;
		List<String> emails = new ArrayList<>();
		Connection connection = DataSourceUtils.getConnection(dataSource);
		
		try (PreparedStatement statement = connection.prepareStatement(sql)) {
			statement.setLong(1, contactId);
			
			try (ResultSet resultSet = statement.executeQuery()) {
				
				while (resultSet.next()) {
					emails.add(resultSet.getString("email"));
				}
				
			}
			
		} catch (SQLException e) {
			throw new PersistenceException("finding contact emails", e);
		} finally {
			DataSourceUtils.releaseConnection(connection, dataSource);
		}
		
		return emails;
	}
	
	private List<String> findAddresses(Long contactId) {
		String sql = """
					 SELECT address
					 FROM contact_addresses
					 WHERE contact_id = ?
					 """;
		List<String> addresses = new ArrayList<>();
		Connection connection = DataSourceUtils.getConnection(dataSource);
		
		try (PreparedStatement statement = connection.prepareStatement(sql)) {
			statement.setLong(1, contactId);
			
			try (ResultSet resultSet = statement.executeQuery()) {
				
				while (resultSet.next()) {
					addresses.add(resultSet.getString("address"));
				}
				
			}
			
		} catch (SQLException e) {
			throw new PersistenceException("finding contact addresses", e);
		} finally {
			DataSourceUtils.releaseConnection(connection, dataSource);
		}
		
		return addresses;
	}
	
	private void deletePhoneNumbers(Long contactId) {
		String sql = """
					 DELETE
					 FROM contact_phone_numbers
					 WHERE contact_id = ?
					 """;
		Connection connection = DataSourceUtils.getConnection(dataSource);
		
		try (PreparedStatement statement = connection.prepareStatement(sql)) {
			statement.setLong(1, contactId);
			
			statement.executeUpdate();
		} catch (SQLException e) {
			throw new PersistenceException("deleting contact phone numbers", e);
		} finally {
			DataSourceUtils.releaseConnection(connection, dataSource);
		}
		
	}
	
	private void deleteEmails(Long contactId) {
		String sql = """
					 DELETE
					 FROM contact_emails
					 WHERE contact_id = ?
					 """;
		Connection connection = DataSourceUtils.getConnection(dataSource);
		
		try (PreparedStatement statement = connection.prepareStatement(sql)) {
			statement.setLong(1, contactId);
			
			statement.executeUpdate();
		} catch (SQLException e) {
			throw new PersistenceException("deleting contact emails", e);
		} finally {
			DataSourceUtils.releaseConnection(connection, dataSource);
		}
		
	}
	
	private void deleteAddresses(Long contactId) {
		String sql = """
					 DELETE
					 FROM contact_addresses
					 WHERE contact_id = ?
					 """;
		Connection connection = DataSourceUtils.getConnection(dataSource);
		
		try (PreparedStatement statement = connection.prepareStatement(sql)) {
			statement.setLong(1, contactId);
			
			statement.executeUpdate();
		} catch (SQLException e) {
			throw new PersistenceException("deleting contact addresses", e);
		} finally {
			DataSourceUtils.releaseConnection(connection, dataSource);
		}
		
	}
	
}
