package lexflow.dao;

import lexflow.entity.case_.CaseResolutionStatus;
import lexflow.entity.consultation.CaseType;
import lexflow.entity.finance.TransactionType;
import lexflow.exception.PersistenceException;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
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
	
	// Case
	
	public Map<CaseResolutionStatus, Integer> countCasesByResolutionStatus() {
		String sql = """
					 SELECT case_resolution_status,
					        COUNT(*) AS case_count
					 FROM cases
					 WHERE case_resolution_status IS NOT NULL
					 GROUP BY case_resolution_status
					 """;
		
		return countByResolutionStatus(sql, null);
	}
	
	public Map<CaseResolutionStatus, Integer> countCasesByResolutionStatusForAssociate(Long associateId) {
		String sql = """
					 SELECT case_resolution_status,
					        COUNT(*) AS case_count
					 FROM cases
					 WHERE case_resolution_status IS NOT NULL
					   AND associate_id = ?
					 GROUP BY case_resolution_status
					 """;
		
		return countByResolutionStatus(sql, associateId);
	}
	
	public Map<CaseResolutionStatus, Integer> countCasesByResolutionStatusForParalegal(Long paralegalId) {
		String sql = """
					 SELECT case_resolution_status,
					        COUNT(*) AS case_count
					 FROM cases
					 WHERE case_resolution_status IS NOT NULL
					   AND paralegal_id = ?
					 GROUP BY case_resolution_status
					 """;
		
		return countByResolutionStatus(sql, paralegalId);
	}
	
	public Double averageCaseDurationDays() {
		String sql = """
					 SELECT AVG(CAST(DATEDIFF(DAY, opened_at, closed_at) AS FLOAT)) AS average_days
					 FROM cases
					 WHERE case_status = 'CLOSED'
					 """;
		
		return averageDuration(sql, null);
	}
	
	public Double averageCaseDurationDaysForAssociate(Long associateId) {
		String sql = """
					 SELECT AVG(CAST(DATEDIFF(DAY, opened_at, closed_at) AS FLOAT)) AS average_days
					 FROM cases
					 WHERE case_status = 'CLOSED'
					   AND associate_id = ?
					 """;
		
		return averageDuration(sql, associateId);
	}
	
	public Double averageCaseDurationDaysForParalegal(Long paralegalId) {
		String sql = """
					 SELECT AVG(CAST(DATEDIFF(DAY, opened_at, closed_at) AS FLOAT)) AS average_days
					 FROM cases
					 WHERE case_status = 'CLOSED'
					   AND paralegal_id = ?
					 """;
		
		return averageDuration(sql, paralegalId);
	}
	
	public Map<Long, Integer> countClosedCasesByAssociate() {
		String sql = """
					 SELECT associate_id,
					        COUNT(*) AS case_count
					 FROM cases
					 WHERE case_status = 'CLOSED'
					 GROUP BY associate_id
					 """;
		
		return countByUserId(sql, "associate_id", "counting closed cases by associate");
	}
	
	public Map<Long, Integer> countClosedCasesByParalegal() {
		String sql = """
					 SELECT paralegal_id,
					        COUNT(*) AS case_count
					 FROM cases
					 WHERE case_status = 'CLOSED'
					 GROUP BY paralegal_id
					 """;
		
		return countByUserId(sql, "paralegal_id", "counting closed cases by paralegal");
	}
	
	// Task
	
	public BigDecimal sumBillableAmountForAssociateInMonth(Long associateId, int year, int month) {
		String sql = """
					 SELECT SUM(tk.billable_amount) AS total_billable_amount
					 FROM tasks tk
					 INNER JOIN cases cs
					     ON cs.case_id = tk.case_id
					 WHERE tk.billing_task = 1
					   AND cs.associate_id = ?
					   AND YEAR(tk.logged_at) = ?
					   AND MONTH(tk.logged_at) = ?
					 """;
		
		return sumBillableAmountForStaffInMonth(sql, associateId, year, month);
	}
	
	public BigDecimal sumBillableAmountForParalegalInMonth(Long paralegalId, int year, int month) {
		String sql = """
					 SELECT SUM(tk.billable_amount) AS total_billable_amount
					 FROM tasks tk
					 INNER JOIN cases cs
					     ON cs.case_id = tk.case_id
					 WHERE tk.billing_task = 1
					   AND cs.paralegal_id = ?
					   AND YEAR(tk.logged_at) = ?
					   AND MONTH(tk.logged_at) = ?
					 """;
		
		return sumBillableAmountForStaffInMonth(sql, paralegalId, year, month);
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
	
	public Map<CaseType, BigDecimal> sumPaidRevenueByCaseType() {
		String sql = """
					 SELECT cn.case_type,
					        SUM(i.total_amount) AS total_revenue
					 FROM invoices i
					 INNER JOIN cases cs
					     ON cs.case_id = i.case_id
					 INNER JOIN consultations cn
					     ON cn.consultation_id = cs.consultation_id
					 WHERE i.invoice_status = 'PAID'
					 GROUP BY cn.case_type
					 """;
		
		Map<CaseType, BigDecimal> sums = new EnumMap<>(CaseType.class);
		Connection connection = DataSourceUtils.getConnection(dataSource);
		
		try (PreparedStatement statement = connection.prepareStatement(sql)) {
			
			try (ResultSet resultSet = statement.executeQuery()) {
				
				while (resultSet.next()) {
					sums.put(CaseType.valueOf(resultSet.getString("case_type")), resultSet.getBigDecimal("total_revenue"));
				}
				
			}
			
		} catch (SQLException e) {
			throw new PersistenceException("summing paid revenue by case type", e);
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
	
	public Double averageDaysToPayment() {
		String sql = """
					 SELECT AVG(CAST(DATEDIFF(DAY, compiled_at, paid_at) AS FLOAT)) AS average_days
					 FROM invoices
					 WHERE invoice_status = 'PAID'
					 """;
		
		return averageDuration(sql, null);
	}
	
	// Transaction
	
	public Map<TransactionType, Integer> countTopUpsByType() {
		String sql = """
					 SELECT transaction_type,
					        COUNT(*) AS transaction_count
					 FROM transactions
					 WHERE transaction_type IN ('CARD_TOP_UP', 'CASH_TOP_UP')
					 GROUP BY transaction_type
					 """;
		
		Map<TransactionType, Integer> counts = new EnumMap<>(TransactionType.class);
		Connection connection = DataSourceUtils.getConnection(dataSource);
		
		try (PreparedStatement statement = connection.prepareStatement(sql)) {
			
			try (ResultSet resultSet = statement.executeQuery()) {
				
				while (resultSet.next()) {
					TransactionType transactionType = TransactionType.valueOf(resultSet.getString("transaction_type"));
					counts.put(transactionType, resultSet.getInt("transaction_count"));
				}
				
			}
			
		} catch (SQLException e) {
			throw new PersistenceException("counting top-ups by type", e);
		} finally {
			DataSourceUtils.releaseConnection(connection, dataSource);
		}
		
		return counts;
	}
	
	// Private Helpers
	
	private Map<CaseResolutionStatus, Integer> countByResolutionStatus(String sql, Long userId) {
		Map<CaseResolutionStatus, Integer> counts = new EnumMap<>(CaseResolutionStatus.class);
		Connection connection = DataSourceUtils.getConnection(dataSource);
		
		try (PreparedStatement statement = connection.prepareStatement(sql)) {
			
			if (userId != null) {
				statement.setLong(1, userId);
			}
			
			try (ResultSet resultSet = statement.executeQuery()) {
				
				while (resultSet.next()) {
					CaseResolutionStatus caseResolutionStatus = CaseResolutionStatus.valueOf(resultSet.getString("case_resolution_status"));
					counts.put(caseResolutionStatus, resultSet.getInt("case_count"));
				}
				
			}
			
		} catch (SQLException e) {
			throw new PersistenceException("counting cases by resolution status", e);
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
	
	private BigDecimal sumBillableAmountForStaffInMonth(String sql, Long userId, int year, int month) {
		Connection connection = DataSourceUtils.getConnection(dataSource);
		
		try (PreparedStatement statement = connection.prepareStatement(sql)) {
			statement.setLong(1, userId);
			statement.setInt(2, year);
			statement.setInt(3, month);
			
			try (ResultSet resultSet = statement.executeQuery()) {
				
				if (resultSet.next()) {
					BigDecimal total = resultSet.getBigDecimal("total_billable_amount");
					
					return total != null ? total : BigDecimal.ZERO;
				}
				
				return BigDecimal.ZERO;
			}
			
		} catch (SQLException e) {
			throw new PersistenceException("summing billable amount for staff in month", e);
		} finally {
			DataSourceUtils.releaseConnection(connection, dataSource);
		}
		
	}
	
	private Map<Long, Integer> countByUserId(String sql, String columnName, String errorContext) {
		Map<Long, Integer> counts = new HashMap<>();
		Connection connection = DataSourceUtils.getConnection(dataSource);
		
		try (PreparedStatement statement = connection.prepareStatement(sql)) {
			
			try (ResultSet resultSet = statement.executeQuery()) {
				
				while (resultSet.next()) {
					long userId = resultSet.getLong(columnName);
					
					if (!resultSet.wasNull()) {
						counts.put(userId, resultSet.getInt("case_count"));
					}
					
				}
				
			}
			
		} catch (SQLException e) {
			throw new PersistenceException(errorContext, e);
		} finally {
			DataSourceUtils.releaseConnection(connection, dataSource);
		}
		
		return counts;
	}
	
}
