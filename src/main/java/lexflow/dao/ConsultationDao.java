package lexflow.dao;

import lexflow.entity.consultation.CaseType;
import lexflow.entity.consultation.Consultation;
import lexflow.entity.consultation.ConsultationStatus;
import lexflow.entity.user.Associate;
import lexflow.entity.user.Client;
import lexflow.entity.user.Paralegal;
import lexflow.entity.user.Role;
import lexflow.exception.EntityNotFoundException;
import lexflow.exception.PersistenceException;
import lexflow.util.CodeUtil;
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
public class ConsultationDao {
	
	private static final String SELECT_COLUMNS = """
												 consultation_id,
												 consultation_code,
												 client_id,
												 case_type,
												 description,
												 consultation_status,
												 submitted_at,
												 associate_id,
												 paralegal_id,
												 rejection_reason
												 """;
	
	private final DataSource dataSource;
	
	private final UserDao userDao;
	
	// Create
	public Consultation insert(Consultation consultation) {
		String sql = """
					 INSERT INTO consultations (client_id, case_type, description, consultation_status, submitted_at, associate_id, paralegal_id, rejection_reason)
					 VALUES (?, ?, ?, ?, ?, ?, ?, ?)
					 """;
		String consultationCode;
		Connection connection = DataSourceUtils.getConnection(dataSource);
		
		try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
			statement.setLong(1, consultation.getClient().getUserId());
			statement.setString(2, consultation.getCaseType().name());
			statement.setString(3, consultation.getDescription());
			statement.setString(4, consultation.getConsultationStatus().name());
			statement.setTimestamp(5, Timestamp.valueOf(consultation.getSubmittedAt()));
			
			if (consultation.getAssociate() != null) {
				statement.setLong(6, consultation.getAssociate().getUserId());
			} else {
				statement.setNull(6, Types.BIGINT);
			}
			
			if (consultation.getParalegal() != null) {
				statement.setLong(7, consultation.getParalegal().getUserId());
			} else {
				statement.setNull(7, Types.BIGINT);
			}
			
			statement.setString(8, consultation.getRejectionReason());
			
			statement.executeUpdate();
			
			try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
				
				if (!generatedKeys.next()) {
					throw new PersistenceException("generating consultation ID");
				}
				
				Long generatedId = generatedKeys.getLong(1);
				consultation.setConsultationId(generatedId);
				consultationCode = CodeUtil.forConsultation(generatedId);
			}
			
		} catch (SQLException e) {
			throw new PersistenceException("inserting consultation", e);
		} finally {
			DataSourceUtils.releaseConnection(connection, dataSource);
		}
		
		sql = """
			  UPDATE consultations
			  SET consultation_code = ?
			  WHERE consultation_id = ?
			  """;
		
		connection = DataSourceUtils.getConnection(dataSource);
		
		try (PreparedStatement statement = connection.prepareStatement(sql)) {
			statement.setString(1, consultationCode);
			statement.setLong(2, consultation.getConsultationId());
			
			statement.executeUpdate();
		} catch (SQLException e) {
			throw new PersistenceException("setting consultation code", e);
		} finally {
			DataSourceUtils.releaseConnection(connection, dataSource);
		}
		
		consultation.setConsultationCode(consultationCode);
		
		insertOpposingParties(consultation.getConsultationId(), consultation.getOpposingParties());
		
		return consultation;
	}
	
	// Read
	public Consultation findById(Long consultationId) {
		String sql = """
					 SELECT %s
					 FROM consultations
					 WHERE consultation_id = ?
					 """.formatted(SELECT_COLUMNS);
		Connection connection = DataSourceUtils.getConnection(dataSource);
		
		try (PreparedStatement statement = connection.prepareStatement(sql)) {
			statement.setLong(1, consultationId);
			
			try (ResultSet resultSet = statement.executeQuery()) {
				
				if (resultSet.next()) {
					return mapRow(resultSet);
				}
				
				throw new EntityNotFoundException("Consultation", consultationId.toString());
			}
			
		} catch (SQLException e) {
			throw new PersistenceException("finding consultation by ID", e);
		} finally {
			DataSourceUtils.releaseConnection(connection, dataSource);
		}
		
	}
	
	public List<Consultation> findAll() {
		String sql = """
					 SELECT %s
					 FROM consultations
					 ORDER BY submitted_at DESC
					 """.formatted(SELECT_COLUMNS);
		List<Consultation> consultations = new ArrayList<>();
		Connection connection = DataSourceUtils.getConnection(dataSource);
		
		try (PreparedStatement statement = connection.prepareStatement(sql)) {
			
			try (ResultSet resultSet = statement.executeQuery()) {
				
				while (resultSet.next()) {
					consultations.add(mapRow(resultSet));
				}
				
			}
			
		} catch (SQLException e) {
			throw new PersistenceException("finding consultations", e);
		} finally {
			DataSourceUtils.releaseConnection(connection, dataSource);
		}
		
		return consultations;
	}
	
	public List<Consultation> findByClientId(Long clientId) {
		return findByForeignKey("client_id", clientId);
	}
	
	public List<Consultation> findByAssociateId(Long associateId) {
		return findByForeignKey("associate_id", associateId);
	}
	
	public List<Consultation> findByParalegalId(Long paralegalId) {
		return findByForeignKey("paralegal_id", paralegalId);
	}
	
	public List<Consultation> findByStatus(ConsultationStatus consultationStatus) {
		String sql = """
					 SELECT %s
					 FROM consultations
					 WHERE consultation_status = ?
					 ORDER BY submitted_at DESC
					 """.formatted(SELECT_COLUMNS);
		List<Consultation> consultations = new ArrayList<>();
		Connection connection = DataSourceUtils.getConnection(dataSource);
		
		try (PreparedStatement statement = connection.prepareStatement(sql)) {
			statement.setString(1, consultationStatus.name());
			
			try (ResultSet resultSet = statement.executeQuery()) {
				
				while (resultSet.next()) {
					consultations.add(mapRow(resultSet));
				}
				
			}
			
		} catch (SQLException e) {
			throw new PersistenceException("finding consultations by status", e);
		} finally {
			DataSourceUtils.releaseConnection(connection, dataSource);
		}
		
		return consultations;
	}
	
	public List<Consultation> findByAssociateIdAndStatus(Long associateId, ConsultationStatus consultationStatus) {
		return findByForeignKeyAndStatus("associate_id", associateId, consultationStatus);
	}
	
	public List<Consultation> findByParalegalIdAndStatus(Long paralegalId, ConsultationStatus consultationStatus) {
		return findByForeignKeyAndStatus("paralegal_id", paralegalId, consultationStatus);
	}
	
	public List<Consultation> findByClientKeyword(String keyword) {
		String sql = """
					 SELECT cn.consultation_id,
					        cn.consultation_code,
					        cn.client_id,
					        cn.case_type,
					        cn.description,
					        cn.consultation_status,
					        cn.submitted_at,
					        cn.associate_id,
					        cn.paralegal_id,
					        cn.rejection_reason
					 FROM consultations cn
					 INNER JOIN users u
					     ON u.user_id = cn.client_id
					 WHERE LOWER(u.name) LIKE ?
					 """;
		List<Consultation> consultations = new ArrayList<>();
		Connection connection = DataSourceUtils.getConnection(dataSource);
		
		try (PreparedStatement statement = connection.prepareStatement(sql)) {
			statement.setString(1, "%" + keyword.toLowerCase() + "%");
			
			try (ResultSet resultSet = statement.executeQuery()) {
				
				while (resultSet.next()) {
					consultations.add(mapRow(resultSet));
				}
				
			}
			
		} catch (SQLException e) {
			throw new PersistenceException("finding consultations by client keyword", e);
		} finally {
			DataSourceUtils.releaseConnection(connection, dataSource);
		}
		
		return consultations;
	}
	
	// Update
	public Consultation update(Consultation consultation) {
		String sql = """
					 UPDATE consultations
					 SET case_type           = ?,
					     description         = ?,
					     consultation_status = ?,
					     associate_id        = ?,
					     paralegal_id        = ?,
					     rejection_reason    = ?
					 WHERE consultation_id = ?
					 """;
		Connection connection = DataSourceUtils.getConnection(dataSource);
		
		try (PreparedStatement statement = connection.prepareStatement(sql)) {
			statement.setString(1, consultation.getCaseType().name());
			statement.setString(2, consultation.getDescription());
			statement.setString(3, consultation.getConsultationStatus().name());
			
			if (consultation.getAssociate() != null) {
				statement.setLong(4, consultation.getAssociate().getUserId());
			} else {
				statement.setNull(4, Types.BIGINT);
			}
			
			if (consultation.getParalegal() != null) {
				statement.setLong(5, consultation.getParalegal().getUserId());
			} else {
				statement.setNull(5, Types.BIGINT);
			}
			
			statement.setString(6, consultation.getRejectionReason());
			
			statement.setLong(7, consultation.getConsultationId());
			
			statement.executeUpdate();
		} catch (SQLException e) {
			throw new PersistenceException("updating consultation", e);
		} finally {
			DataSourceUtils.releaseConnection(connection, dataSource);
		}
		
		replaceOpposingParties(consultation.getConsultationId(), consultation.getOpposingParties());
		
		return consultation;
	}
	
	// Delete
	public void deleteById(Long consultationId) {
		String sql = """
					 DELETE
					 FROM consultation_opposing_parties
					 WHERE consultation_id = ?
					 """;
		Connection connection = DataSourceUtils.getConnection(dataSource);
		
		try (PreparedStatement statement = connection.prepareStatement(sql)) {
			statement.setLong(1, consultationId);
			
			statement.executeUpdate();
		} catch (SQLException e) {
			throw new PersistenceException("deleting consultation opposing parties", e);
		} finally {
			DataSourceUtils.releaseConnection(connection, dataSource);
		}
		
		sql = """
			  DELETE
			  FROM consultations
			  WHERE consultation_id = ?
			  """;
		
		connection = DataSourceUtils.getConnection(dataSource);
		
		try (PreparedStatement statement = connection.prepareStatement(sql)) {
			statement.setLong(1, consultationId);
			
			statement.executeUpdate();
		} catch (SQLException e) {
			throw new PersistenceException("deleting consultation", e);
		} finally {
			DataSourceUtils.releaseConnection(connection, dataSource);
		}
		
	}
	
	// Private Helpers
	private void insertOpposingParties(Long consultationId, List<String> opposingParties) {
		
		if (opposingParties == null) {
			return;
		}
		
		String sql = """
					 INSERT INTO consultation_opposing_parties (consultation_id, opposing_party)
					 VALUES (?, ?)
					 """;
		Connection connection = DataSourceUtils.getConnection(dataSource);
		
		try (PreparedStatement statement = connection.prepareStatement(sql)) {
			
			for (String opposingParty : opposingParties) {
				
				if (opposingParty == null || opposingParty.isBlank()) {
					continue;
				}
				
				statement.setLong(1, consultationId);
				statement.setString(2, opposingParty);
				
				statement.executeUpdate();
			}
			
		} catch (SQLException e) {
			throw new PersistenceException("inserting consultation opposing parties", e);
		} finally {
			DataSourceUtils.releaseConnection(connection, dataSource);
		}
		
	}
	
	private Consultation mapRow(ResultSet resultSet) throws SQLException {
		Long consultationId = resultSet.getLong("consultation_id");
		String consultationCode = resultSet.getString("consultation_code");
		Client client = (Client) userDao.findByIdAndRole(resultSet.getLong("client_id"), Role.CLIENT);
		CaseType caseType = CaseType.valueOf(resultSet.getString("case_type"));
		List<String> opposingParties = findOpposingParties(consultationId);
		String description = resultSet.getString("description");
		ConsultationStatus consultationStatus = ConsultationStatus.valueOf(resultSet.getString("consultation_status"));
		LocalDateTime submittedAt = resultSet.getTimestamp("submitted_at").toLocalDateTime();
		Associate associate = null;
		long associateId = resultSet.getLong("associate_id");
		
		if (!resultSet.wasNull()) {
			associate = (Associate) userDao.findByIdAndRole(associateId, Role.ASSOCIATE);
		}
		
		Paralegal paralegal = null;
		long paralegalId = resultSet.getLong("paralegal_id");
		
		if (!resultSet.wasNull()) {
			paralegal = (Paralegal) userDao.findByIdAndRole(paralegalId, Role.PARALEGAL);
		}
		
		String rejectionReason = resultSet.getString("rejection_reason");
		
		return new Consultation(consultationId, consultationCode, client, caseType, opposingParties, description, consultationStatus, submittedAt, associate, paralegal, rejectionReason);
	}
	
	private List<String> findOpposingParties(Long consultationId) {
		String sql = """
					 SELECT opposing_party
					 FROM consultation_opposing_parties
					 WHERE consultation_id = ?
					 """;
		List<String> opposingParties = new ArrayList<>();
		Connection connection = DataSourceUtils.getConnection(dataSource);
		
		try (PreparedStatement statement = connection.prepareStatement(sql)) {
			statement.setLong(1, consultationId);
			
			try (ResultSet resultSet = statement.executeQuery()) {
				
				while (resultSet.next()) {
					opposingParties.add(resultSet.getString("opposing_party"));
				}
				
			}
			
		} catch (SQLException e) {
			throw new PersistenceException("finding consultation opposing parties", e);
		} finally {
			DataSourceUtils.releaseConnection(connection, dataSource);
		}
		
		return opposingParties;
	}
	
	private List<Consultation> findByForeignKey(String column, Long userId) {
		String sql = """
					 SELECT %s
					 FROM consultations
					 WHERE %s = ?
					 ORDER BY submitted_at DESC
					 """.formatted(SELECT_COLUMNS, column);
		List<Consultation> consultations = new ArrayList<>();
		Connection connection = DataSourceUtils.getConnection(dataSource);
		
		try (PreparedStatement statement = connection.prepareStatement(sql)) {
			statement.setLong(1, userId);
			
			try (ResultSet resultSet = statement.executeQuery()) {
				
				while (resultSet.next()) {
					consultations.add(mapRow(resultSet));
				}
				
			}
			
		} catch (SQLException e) {
			throw new PersistenceException("finding consultations by " + column, e);
		} finally {
			DataSourceUtils.releaseConnection(connection, dataSource);
		}
		
		return consultations;
	}
	
	private List<Consultation> findByForeignKeyAndStatus(String column, Long userId, ConsultationStatus consultationStatus) {
		String sql = """
					 SELECT %s
					 FROM consultations
					 WHERE %s = ?
					   AND consultation_status = ?
					 ORDER BY submitted_at DESC
					 """.formatted(SELECT_COLUMNS, column);
		List<Consultation> consultations = new ArrayList<>();
		Connection connection = DataSourceUtils.getConnection(dataSource);
		
		try (PreparedStatement statement = connection.prepareStatement(sql)) {
			statement.setLong(1, userId);
			statement.setString(2, consultationStatus.name());
			
			try (ResultSet resultSet = statement.executeQuery()) {
				
				while (resultSet.next()) {
					consultations.add(mapRow(resultSet));
				}
				
			}
			
		} catch (SQLException e) {
			throw new PersistenceException("finding consultations by " + column + " and status", e);
		} finally {
			DataSourceUtils.releaseConnection(connection, dataSource);
		}
		
		return consultations;
	}
	
	private void replaceOpposingParties(Long consultationId, List<String> opposingParties) {
		String sql = """
					 DELETE
					 FROM consultation_opposing_parties
					 WHERE consultation_id = ?
					 """;
		Connection connection = DataSourceUtils.getConnection(dataSource);
		
		try (PreparedStatement statement = connection.prepareStatement(sql)) {
			statement.setLong(1, consultationId);
			
			statement.executeUpdate();
		} catch (SQLException e) {
			throw new PersistenceException("replacing consultation opposing parties", e);
		} finally {
			DataSourceUtils.releaseConnection(connection, dataSource);
		}
		
		insertOpposingParties(consultationId, opposingParties);
	}
	
}
