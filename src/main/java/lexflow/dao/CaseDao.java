package lexflow.dao;

import lexflow.entity.case_.Case;
import lexflow.entity.case_.CaseResolutionStatus;
import lexflow.entity.case_.CaseStatus;
import lexflow.entity.consultation.Consultation;
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
public class CaseDao {
	
	private static final String SELECT_COLUMNS = """
												 case_id,
												 case_code,
												 consultation_id,
												 client_id,
												 associate_id,
												 paralegal_id,
												 case_status,
												 opened_at,
												 case_resolution_status,
												 closed_at
												 """;
	
	private final DataSource dataSource;
	
	private final UserDao userDao;
	
	private final ConsultationDao consultationDao;
	
	// Create
	
	public Case insert(Case case_) {
		String sql = """
					 INSERT INTO cases (consultation_id, client_id, associate_id, paralegal_id, case_status, opened_at, case_resolution_status, closed_at)
					 VALUES (?, ?, ?, ?, ?, ?, ?, ?)
					 """;
		
		String caseCode;
		
		Connection connection = DataSourceUtils.getConnection(dataSource);
		
		try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
			statement.setLong(1, case_.getConsultation().getConsultationId());
			statement.setLong(2, case_.getClient().getUserId());
			statement.setLong(3, case_.getAssociate().getUserId());
			statement.setLong(4, case_.getParalegal().getUserId());
			statement.setString(5, case_.getCaseStatus().name());
			statement.setTimestamp(6, Timestamp.valueOf(case_.getOpenedAt()));
			
			if (case_.getCaseResolutionStatus() != null) {
				statement.setString(7, case_.getCaseResolutionStatus().name());
			} else {
				statement.setNull(7, Types.VARCHAR);
			}
			
			if (case_.getClosedAt() != null) {
				statement.setTimestamp(8, Timestamp.valueOf(case_.getClosedAt()));
			} else {
				statement.setNull(8, Types.TIMESTAMP);
			}
			
			statement.executeUpdate();
			
			try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
				
				if (!generatedKeys.next()) {
					throw new PersistenceException("generating case ID");
				}
				
				Long generatedId = generatedKeys.getLong(1);
				case_.setCaseId(generatedId);
				caseCode = CodeUtil.forCase(generatedId);
			}
			
		} catch (SQLException e) {
			throw new PersistenceException("inserting case", e);
		} finally {
			DataSourceUtils.releaseConnection(connection, dataSource);
		}
		
		sql = """
			  UPDATE cases
			  SET case_code = ?
			  WHERE case_id = ?
			  """;
		
		connection = DataSourceUtils.getConnection(dataSource);
		
		try (PreparedStatement statement = connection.prepareStatement(sql)) {
			statement.setString(1, caseCode);
			statement.setLong(2, case_.getCaseId());
			
			statement.executeUpdate();
		} catch (SQLException e) {
			throw new PersistenceException("setting case code", e);
		} finally {
			DataSourceUtils.releaseConnection(connection, dataSource);
		}
		
		case_.setCaseCode(caseCode);
		
		return case_;
	}
	
	// Read
	
	public Case findById(Long caseId) {
		String sql = """
					 SELECT %s
					 FROM cases
					 WHERE case_id = ?
					 """.formatted(SELECT_COLUMNS);
		
		Connection connection = DataSourceUtils.getConnection(dataSource);
		
		try (PreparedStatement statement = connection.prepareStatement(sql)) {
			statement.setLong(1, caseId);
			
			try (ResultSet resultSet = statement.executeQuery()) {
				
				if (resultSet.next()) {
					return mapRow(resultSet);
				}
				
				throw new EntityNotFoundException("Case", caseId.toString());
			}
			
		} catch (SQLException e) {
			throw new PersistenceException("finding case by ID", e);
		} finally {
			DataSourceUtils.releaseConnection(connection, dataSource);
		}
		
	}
	
	public Case findByConsultationId(Long consultationId) {
		String sql = """
					 SELECT %s
					 FROM cases
					 WHERE consultation_id = ?
					 """.formatted(SELECT_COLUMNS);
		
		Connection connection = DataSourceUtils.getConnection(dataSource);
		
		try (PreparedStatement statement = connection.prepareStatement(sql)) {
			statement.setLong(1, consultationId);
			
			try (ResultSet resultSet = statement.executeQuery()) {
				
				if (resultSet.next()) {
					return mapRow(resultSet);
				}
				
				return null;
			}
			
		} catch (SQLException e) {
			throw new PersistenceException("finding case by consultation", e);
		} finally {
			DataSourceUtils.releaseConnection(connection, dataSource);
		}
		
	}
	
	public List<Case> findByClientId(Long clientId) {
		return findByForeignKey("client_id", clientId);
	}
	
	public List<Case> findByAssociateId(Long associateId) {
		return findByForeignKey("associate_id", associateId);
	}
	
	public List<Case> findByParalegalId(Long paralegalId) {
		return findByForeignKey("paralegal_id", paralegalId);
	}
	
	public List<Case> findByStatus(CaseStatus caseStatus) {
		String sql = """
					 SELECT %s
					 FROM cases
					 WHERE case_status = ?
					 ORDER BY opened_at DESC
					 """.formatted(SELECT_COLUMNS);
		
		List<Case> cases = new ArrayList<>();
		
		Connection connection = DataSourceUtils.getConnection(dataSource);
		
		try (PreparedStatement statement = connection.prepareStatement(sql)) {
			statement.setString(1, caseStatus.name());
			
			try (ResultSet resultSet = statement.executeQuery()) {
				
				while (resultSet.next()) {
					cases.add(mapRow(resultSet));
				}
				
			}
			
		} catch (SQLException e) {
			throw new PersistenceException("finding cases", e);
		} finally {
			DataSourceUtils.releaseConnection(connection, dataSource);
		}
		
		return cases;
	}
	
	public List<Case> findByClientIdAndStatus(Long clientId, CaseStatus caseStatus) {
		return findByForeignKeyAndStatus("client_id", clientId, caseStatus);
	}
	
	public List<Case> findByAssociateIdAndStatus(Long associateId, CaseStatus caseStatus) {
		return findByForeignKeyAndStatus("associate_id", associateId, caseStatus);
	}
	
	public List<Case> findByParalegalIdAndStatus(Long paralegalId, CaseStatus caseStatus) {
		return findByForeignKeyAndStatus("paralegal_id", paralegalId, caseStatus);
	}
	
	// Update
	
	public Case update(Case case_) {
		String sql = """
					 UPDATE cases
					 SET associate_id           = ?,
					     paralegal_id           = ?,
					     case_status            = ?,
					     case_resolution_status = ?,
					     closed_at              = ?
					 WHERE case_id = ?
					 """;
		
		Connection connection = DataSourceUtils.getConnection(dataSource);
		
		try (PreparedStatement statement = connection.prepareStatement(sql)) {
			statement.setLong(1, case_.getAssociate().getUserId());
			statement.setLong(2, case_.getParalegal().getUserId());
			statement.setString(3, case_.getCaseStatus().name());
			
			if (case_.getCaseResolutionStatus() != null) {
				statement.setString(4, case_.getCaseResolutionStatus().name());
			} else {
				statement.setNull(4, Types.VARCHAR);
			}
			
			if (case_.getClosedAt() != null) {
				statement.setTimestamp(5, Timestamp.valueOf(case_.getClosedAt()));
			} else {
				statement.setNull(5, Types.TIMESTAMP);
			}
			
			statement.setLong(6, case_.getCaseId());
			
			statement.executeUpdate();
		} catch (SQLException e) {
			throw new PersistenceException("updating case", e);
		} finally {
			DataSourceUtils.releaseConnection(connection, dataSource);
		}
		
		return case_;
	}
	
	// Private Helpers
	
	private Case mapRow(ResultSet resultSet) throws SQLException {
		Long caseId = resultSet.getLong("case_id");
		String caseCode = resultSet.getString("case_code");
		Consultation consultation = consultationDao.findById(resultSet.getLong("consultation_id"));
		Client client = (Client) userDao.findByIdAndRole(resultSet.getLong("client_id"), Role.CLIENT);
		Associate associate = (Associate) userDao.findByIdAndRole(resultSet.getLong("associate_id"), Role.ASSOCIATE);
		Paralegal paralegal = (Paralegal) userDao.findByIdAndRole(resultSet.getLong("paralegal_id"), Role.PARALEGAL);
		CaseStatus caseStatus = CaseStatus.valueOf(resultSet.getString("case_status"));
		LocalDateTime openedAt = resultSet.getTimestamp("opened_at").toLocalDateTime();
		
		CaseResolutionStatus caseResolutionStatus = null;
		String caseResolutionStatusName = resultSet.getString("case_resolution_status");
		
		if (caseResolutionStatusName != null) {
			caseResolutionStatus = CaseResolutionStatus.valueOf(caseResolutionStatusName);
		}
		
		LocalDateTime closedAt = null;
		Timestamp closedAtSql = resultSet.getTimestamp("closed_at");
		
		if (closedAtSql != null) {
			closedAt = closedAtSql.toLocalDateTime();
		}
		
		return new Case(caseId, caseCode, consultation, client, associate, paralegal, caseStatus, openedAt, caseResolutionStatus, closedAt);
	}
	
	private List<Case> findByForeignKey(String column, Long userId) {
		String sql = """
					 SELECT %s
					 FROM cases
					 WHERE %s = ?
					 ORDER BY opened_at DESC
					 """.formatted(SELECT_COLUMNS, column);
		
		List<Case> cases = new ArrayList<>();
		
		Connection connection = DataSourceUtils.getConnection(dataSource);
		
		try (PreparedStatement statement = connection.prepareStatement(sql)) {
			statement.setLong(1, userId);
			
			try (ResultSet resultSet = statement.executeQuery()) {
				
				while (resultSet.next()) {
					cases.add(mapRow(resultSet));
				}
				
			}
			
		} catch (SQLException e) {
			throw new PersistenceException("finding cases", e);
		} finally {
			DataSourceUtils.releaseConnection(connection, dataSource);
		}
		
		return cases;
	}
	
	private List<Case> findByForeignKeyAndStatus(String column, Long userId, CaseStatus caseStatus) {
		String sql = """
					 SELECT %s
					 FROM cases
					 WHERE %s = ?
					   AND case_status = ?
					 ORDER BY opened_at DESC
					 """.formatted(SELECT_COLUMNS, column);
		
		List<Case> cases = new ArrayList<>();
		
		Connection connection = DataSourceUtils.getConnection(dataSource);
		
		try (PreparedStatement statement = connection.prepareStatement(sql)) {
			statement.setLong(1, userId);
			statement.setString(2, caseStatus.name());
			
			try (ResultSet resultSet = statement.executeQuery()) {
				
				while (resultSet.next()) {
					cases.add(mapRow(resultSet));
				}
				
			}
			
		} catch (SQLException e) {
			throw new PersistenceException("finding cases", e);
		} finally {
			DataSourceUtils.releaseConnection(connection, dataSource);
		}
		
		return cases;
	}
	
}
