package lexflow.dao;

import lexflow.entity.case_.CaseResolutionStatus;
import lexflow.entity.consultation.ConsultationStatus;
import lexflow.entity.document.DocumentType;
import lexflow.entity.scheduling.EventStatus;
import lexflow.entity.user.Role;
import lexflow.exception.PersistenceException;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

@Repository
@RequiredArgsConstructor
public class AnalyticsDao {
	
	private final DataSource dataSource;
	
	// User
	
	public BigDecimal sumClientWalletBalance() {
		String sql = """
					 SELECT SUM(wallet_balance) AS total_wallet_balance
					 FROM users
					 WHERE role = 'CLIENT'
					 """;
		
		Connection connection = DataSourceUtils.getConnection(dataSource);
		
		try (PreparedStatement statement = connection.prepareStatement(sql)) {
			
			try (ResultSet resultSet = statement.executeQuery()) {
				
				if (resultSet.next()) {
					BigDecimal total = resultSet.getBigDecimal("total_wallet_balance");
					
					return total != null ? total : BigDecimal.ZERO;
				}
				
				return BigDecimal.ZERO;
			}
			
		} catch (SQLException e) {
			throw new PersistenceException("summing client wallet balance", e);
		} finally {
			DataSourceUtils.releaseConnection(connection, dataSource);
		}
		
	}
	
	public Map<Role, Integer> countActiveUsersByRole() {
		String sql = """
					 SELECT role,
					        COUNT(*) AS record_count
					 FROM users
					 WHERE account_status = 'ACTIVE'
					 GROUP BY role
					 """;
		
		return countByEnum(sql, "role", Role.class);
	}
	
	// Consultation
	
	public Map<ConsultationStatus, Integer> countConsultationsByStatus() {
		String sql = """
					 SELECT consultation_status,
					        COUNT(*) AS record_count
					 FROM consultations
					 GROUP BY consultation_status
					 """;
		
		return countByEnum(sql, "consultation_status", ConsultationStatus.class);
	}
	
	// Case
	
	public Map<CaseResolutionStatus, Integer> countCasesByResolutionStatus() {
		String sql = """
					 SELECT case_resolution_status,
					        COUNT(*) AS record_count
					 FROM cases
					 WHERE case_resolution_status IS NOT NULL
					 GROUP BY case_resolution_status
					 """;
		
		return countByEnum(sql, "case_resolution_status", CaseResolutionStatus.class);
	}
	
	public Double averageCaseDurationDays() {
		String sql = """
					 SELECT AVG(CAST(DATEDIFF(DAY, opened_at, closed_at) AS FLOAT)) AS average_days
					 FROM cases
					 WHERE case_status = 'CLOSED'
					 """;
		
		return averageDuration(sql, null);
	}
	
	// Document
	
	public Map<DocumentType, Integer> countDocumentsByType() {
		String sql = """
					 SELECT document_type,
					        COUNT(*) AS record_count
					 FROM documents
					 GROUP BY document_type
					 """;
		
		return countByEnum(sql, "document_type", DocumentType.class);
	}
	
	public int countStagedDocuments() {
		String sql = """
					 SELECT COUNT(*) AS record_count
					 FROM documents
					 WHERE in_staging = 1
					 """;
		
		Connection connection = DataSourceUtils.getConnection(dataSource);
		
		try (PreparedStatement statement = connection.prepareStatement(sql)) {
			
			try (ResultSet resultSet = statement.executeQuery()) {
				resultSet.next();
				
				return resultSet.getInt("record_count");
			}
			
		} catch (SQLException e) {
			throw new PersistenceException("counting staged documents", e);
		} finally {
			DataSourceUtils.releaseConnection(connection, dataSource);
		}
		
	}
	
	// Invoice
	
	public Map<Integer, BigDecimal> sumPaidRevenueByMonth(int year) {
		String sql = """
					 SELECT MONTH(paid_at)    AS month_number,
					        SUM(total_amount) AS total_revenue
					 FROM invoices
					 WHERE invoice_status = 'PAID'
					   AND YEAR(paid_at) = ?
					 GROUP BY MONTH(paid_at)
					 """;
		
		Map<Integer, BigDecimal> sums = new HashMap<>();
		Connection connection = DataSourceUtils.getConnection(dataSource);
		
		try (PreparedStatement statement = connection.prepareStatement(sql)) {
			statement.setInt(1, year);
			
			try (ResultSet resultSet = statement.executeQuery()) {
				
				while (resultSet.next()) {
					sums.put(resultSet.getInt("month_number"), resultSet.getBigDecimal("total_revenue"));
				}
				
			}
			
		} catch (SQLException e) {
			throw new PersistenceException("summing paid revenue by month", e);
		} finally {
			DataSourceUtils.releaseConnection(connection, dataSource);
		}
		
		return sums;
	}
	
	public BigDecimal sumUnpaidInvoiceTotal() {
		String sql = """
					 SELECT SUM(total_amount) AS total_unpaid_amount
					 FROM invoices
					 WHERE invoice_status = 'UNPAID'
					 """;
		
		Connection connection = DataSourceUtils.getConnection(dataSource);
		
		try (PreparedStatement statement = connection.prepareStatement(sql)) {
			
			try (ResultSet resultSet = statement.executeQuery()) {
				
				if (resultSet.next()) {
					BigDecimal total = resultSet.getBigDecimal("total_unpaid_amount");
					
					return total != null ? total : BigDecimal.ZERO;
				}
				
				return BigDecimal.ZERO;
			}
			
		} catch (SQLException e) {
			throw new PersistenceException("summing unpaid invoice total", e);
		} finally {
			DataSourceUtils.releaseConnection(connection, dataSource);
		}
		
	}
	
	// Event
	
	public Map<EventStatus, Integer> countEventsByStatus() {
		String sql = """
					 SELECT event_status,
					        COUNT(*) AS record_count
					 FROM events
					 GROUP BY event_status
					 """;
		
		return countByEnum(sql, "event_status", EventStatus.class);
	}
	
	public int countScheduledEventsBetween(LocalDate startDate, LocalDate endDate) {
		String sql = """
					 SELECT COUNT(*) AS record_count
					 FROM events
					 WHERE event_status = 'SCHEDULED'
					   AND date BETWEEN ? AND ?
					 """;
		
		Connection connection = DataSourceUtils.getConnection(dataSource);
		
		try (PreparedStatement statement = connection.prepareStatement(sql)) {
			statement.setDate(1, Date.valueOf(startDate));
			statement.setDate(2, Date.valueOf(endDate));
			
			try (ResultSet resultSet = statement.executeQuery()) {
				resultSet.next();
				
				return resultSet.getInt("record_count");
			}
			
		} catch (SQLException e) {
			throw new PersistenceException("counting scheduled events", e);
		} finally {
			DataSourceUtils.releaseConnection(connection, dataSource);
		}
		
	}
	
	// Private Helpers
	
	private <E extends Enum<E>> Map<E, Integer> countByEnum(String sql, String column, Class<E> enumType) {
		Map<E, Integer> counts = new EnumMap<>(enumType);
		Connection connection = DataSourceUtils.getConnection(dataSource);
		
		try (PreparedStatement statement = connection.prepareStatement(sql)) {
			
			try (ResultSet resultSet = statement.executeQuery()) {
				
				while (resultSet.next()) {
					counts.put(Enum.valueOf(enumType, resultSet.getString(column)), resultSet.getInt("record_count"));
				}
				
			}
			
		} catch (SQLException e) {
			throw new PersistenceException("counting by " + column, e);
		} finally {
			DataSourceUtils.releaseConnection(connection, dataSource);
		}
		
		return counts;
	}
	
	private Double averageDuration(String sql, Long userId) {
		Connection connection = DataSourceUtils.getConnection(dataSource);
		
		try (PreparedStatement statement = connection.prepareStatement(sql)) {
			
			if (userId != null) {
				statement.setLong(1, userId);
			}
			
			try (ResultSet resultSet = statement.executeQuery()) {
				
				if (resultSet.next()) {
					double avgDays = resultSet.getDouble("average_days");
					
					return resultSet.wasNull() ? null : avgDays;
				}
				
				return null;
			}
			
		} catch (SQLException e) {
			throw new PersistenceException("averaging case duration", e);
		} finally {
			DataSourceUtils.releaseConnection(connection, dataSource);
		}
		
	}
	
}
