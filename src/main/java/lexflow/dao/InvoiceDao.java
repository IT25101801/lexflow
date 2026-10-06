package lexflow.dao;

import lexflow.entity.document.InvoiceDocument;
import lexflow.entity.finance.Invoice;
import lexflow.entity.finance.InvoiceStatus;
import lexflow.exception.EntityNotFoundException;
import lexflow.exception.PersistenceException;
import lexflow.util.CodeUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class InvoiceDao {
	
	private static final String SELECT_COLUMNS = """
												 invoice_id,
												 invoice_code,
												 case_id,
												 total_amount,
												 invoice_status,
												 compiled_at,
												 paid_at
												 """;
	
	private final DataSource dataSource;
	
	private final DocumentDao documentDao;
	
	// Create
	public Invoice insert(Invoice invoice) {
		String sql = """
					 INSERT INTO invoices (case_id, total_amount, invoice_status, compiled_at, paid_at)
					 VALUES (?, ?, ?, ?, ?)
					 """;
		String invoiceCode;
		Connection connection = DataSourceUtils.getConnection(dataSource);
		
		try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
			statement.setLong(1, invoice.getCaseId());
			statement.setBigDecimal(2, invoice.getTotalAmount());
			statement.setString(3, invoice.getInvoiceStatus().name());
			statement.setTimestamp(4, Timestamp.valueOf(invoice.getCompiledAt()));
			
			if (invoice.getPaidAt() != null) {
				statement.setTimestamp(5, Timestamp.valueOf(invoice.getPaidAt()));
			} else {
				statement.setNull(5, Types.TIMESTAMP);
			}
			
			statement.executeUpdate();
			
			try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
				
				if (!generatedKeys.next()) {
					throw new PersistenceException("generating invoice ID");
				}
				
				Long generatedId = generatedKeys.getLong(1);
				
				invoice.setInvoiceId(generatedId);
				invoiceCode = CodeUtil.forInvoice(generatedId);
			}
			
		} catch (SQLException e) {
			throw new PersistenceException("inserting invoice", e);
		} finally {
			DataSourceUtils.releaseConnection(connection, dataSource);
		}
		
		sql = """
			  UPDATE invoices
			  SET invoice_code = ?
			  WHERE invoice_id = ?
			  """;
		
		connection = DataSourceUtils.getConnection(dataSource);
		
		try (PreparedStatement statement = connection.prepareStatement(sql)) {
			statement.setString(1, invoiceCode);
			statement.setLong(2, invoice.getInvoiceId());
			
			statement.executeUpdate();
		} catch (SQLException e) {
			throw new PersistenceException("setting invoice code", e);
		} finally {
			DataSourceUtils.releaseConnection(connection, dataSource);
		}
		
		invoice.setInvoiceCode(invoiceCode);
		
		return invoice;
	}
	
	// Read
	public Invoice findById(Long invoiceId) {
		String sql = """
					 SELECT %s
					 FROM invoices
					 WHERE invoice_id = ?
					 """.formatted(SELECT_COLUMNS);
		Connection connection = DataSourceUtils.getConnection(dataSource);
		
		try (PreparedStatement statement = connection.prepareStatement(sql)) {
			statement.setLong(1, invoiceId);
			
			try (ResultSet resultSet = statement.executeQuery()) {
				
				if (resultSet.next()) {
					return mapRow(resultSet);
				}
				
				throw new EntityNotFoundException("Invoice", invoiceId.toString());
			}
			
		} catch (SQLException e) {
			throw new PersistenceException("finding invoice by ID", e);
		} finally {
			DataSourceUtils.releaseConnection(connection, dataSource);
		}
		
	}
	
	public List<Invoice> findByCaseIdAndStatus(Long caseId, InvoiceStatus invoiceStatus) {
		String sql = """
					 SELECT %s
					 FROM invoices
					 WHERE case_id = ?
					   AND invoice_status = ?
					 ORDER BY compiled_at DESC
					 """.formatted(SELECT_COLUMNS);
		List<Invoice> invoices = new ArrayList<>();
		Connection connection = DataSourceUtils.getConnection(dataSource);
		
		try (PreparedStatement statement = connection.prepareStatement(sql)) {
			statement.setLong(1, caseId);
			statement.setString(2, invoiceStatus.name());
			
			try (ResultSet resultSet = statement.executeQuery()) {
				
				while (resultSet.next()) {
					invoices.add(mapRow(resultSet));
				}
				
			}
			
		} catch (SQLException e) {
			throw new PersistenceException("finding invoices by case and status", e);
		} finally {
			DataSourceUtils.releaseConnection(connection, dataSource);
		}
		
		return invoices;
	}
	
	// Update
	public Invoice update(Invoice invoice) {
		String sql = """
					 UPDATE invoices
					 SET invoice_status = ?,
					     paid_at        = ?
					 WHERE invoice_id = ?
					 """;
		Connection connection = DataSourceUtils.getConnection(dataSource);
		
		try (PreparedStatement statement = connection.prepareStatement(sql)) {
			statement.setString(1, invoice.getInvoiceStatus().name());
			
			if (invoice.getPaidAt() != null) {
				statement.setTimestamp(2, Timestamp.valueOf(invoice.getPaidAt()));
			} else {
				statement.setNull(2, Types.TIMESTAMP);
			}
			
			statement.setLong(3, invoice.getInvoiceId());
			
			statement.executeUpdate();
		} catch (SQLException e) {
			throw new PersistenceException("updating invoice", e);
		} finally {
			DataSourceUtils.releaseConnection(connection, dataSource);
		}
		
		return invoice;
	}
	
	// Private Helpers
	private Invoice mapRow(ResultSet resultSet) throws SQLException {
		Long invoiceId = resultSet.getLong("invoice_id");
		String invoiceCode = resultSet.getString("invoice_code");
		Long caseId = resultSet.getLong("case_id");
		BigDecimal totalAmount = resultSet.getBigDecimal("total_amount");
		InvoiceStatus invoiceStatus = InvoiceStatus.valueOf(resultSet.getString("invoice_status"));
		LocalDateTime compiledAt = resultSet.getTimestamp("compiled_at").toLocalDateTime();
		InvoiceDocument invoiceDocument = documentDao.findByInvoiceId(invoiceId);
		LocalDateTime paidAt = null;
		Timestamp paidAtSql = resultSet.getTimestamp("paid_at");
		
		if (paidAtSql != null) {
			paidAt = paidAtSql.toLocalDateTime();
		}
		
		return new Invoice(invoiceId, invoiceCode, caseId, totalAmount, invoiceStatus, compiledAt, invoiceDocument, paidAt);
	}
	
}
