package lexflow.dao;

import lexflow.entity.finance.Transaction;
import lexflow.entity.finance.TransactionType;
import lexflow.entity.user.Client;
import lexflow.entity.user.Role;
import lexflow.exception.EntityNotFoundException;
import lexflow.exception.PersistenceException;
import lexflow.exception.finance.InsufficientFundsException;
import lexflow.exception.finance.InvalidTransactionAmountException;
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
public class TransactionDao {
	
	private static final String SELECT_COLUMNS = """
												 transaction_id,
												 transaction_code,
												 client_id,
												 amount,
												 transaction_type,
												 invoice_id,
												 occurred_at
												 """;
	
	private final DataSource dataSource;
	
	private final UserDao userDao;
	
	// Create
	public Transaction insert(Transaction transaction) {
		String sql = "{call dbo.usp_record_wallet_transaction(?, ?, ?, ?, ?, ?, ?, ?, ?)}";
		Long clientId = transaction.getClient().getUserId();
		Connection connection = DataSourceUtils.getConnection(dataSource);
		
		try (CallableStatement statement = connection.prepareCall(sql)) {
			statement.setLong(1, clientId);
			statement.setBigDecimal(2, transaction.getAmount());
			statement.setString(3, transaction.getTransactionType().name());
			
			if (transaction.getInvoiceId() != null) {
				statement.setLong(4, transaction.getInvoiceId());
			} else {
				statement.setNull(4, Types.BIGINT);
			}
			
			statement.setTimestamp(5, Timestamp.valueOf(transaction.getOccurredAt()));
			
			statement.registerOutParameter(6, Types.BIGINT);
			statement.registerOutParameter(7, Types.VARCHAR);
			statement.registerOutParameter(8, Types.DECIMAL);
			statement.registerOutParameter(9, Types.INTEGER);
			
			statement.execute();
			
			int statusCode = statement.getInt(9);
			BigDecimal balance = statement.getBigDecimal(8);
			
			if (statusCode == 1) {
				throw new InvalidTransactionAmountException(transaction.getAmount());
			} else if (statusCode == 2) {
				throw new EntityNotFoundException("Client", clientId.toString());
			} else if (statusCode == 3) {
				throw new InsufficientFundsException(clientId, balance, transaction.getAmount());
			} else if (statusCode != 0) {
				throw new PersistenceException("inserting transaction: unhandled status code " + statusCode);
			}
			
			transaction.setTransactionId(statement.getLong(6));
			transaction.setTransactionCode(statement.getString(7));
			transaction.getClient().setWalletBalance(balance);
			
			return transaction;
		} catch (SQLException e) {
			throw new PersistenceException("inserting transaction", e);
		} finally {
			DataSourceUtils.releaseConnection(connection, dataSource);
		}
		
	}
	
	// Read
	public List<Transaction> findAll() {
		String sql = """
					 SELECT %s
					 FROM transactions
					 ORDER BY occurred_at DESC
					 """.formatted(SELECT_COLUMNS);
		List<Transaction> transactions = new ArrayList<>();
		Connection connection = DataSourceUtils.getConnection(dataSource);
		
		try (PreparedStatement statement = connection.prepareStatement(sql)) {
			
			try (ResultSet resultSet = statement.executeQuery()) {
				
				while (resultSet.next()) {
					transactions.add(mapRow(resultSet));
				}
				
			}
			
		} catch (SQLException e) {
			throw new PersistenceException("finding transactions", e);
		} finally {
			DataSourceUtils.releaseConnection(connection, dataSource);
		}
		
		return transactions;
	}
	
	public List<Transaction> findByClientId(Long clientId) {
		String sql = """
					 SELECT %s
					 FROM transactions
					 WHERE client_id = ?
					 ORDER BY occurred_at DESC
					 """.formatted(SELECT_COLUMNS);
		List<Transaction> transactions = new ArrayList<>();
		Connection connection = DataSourceUtils.getConnection(dataSource);
		
		try (PreparedStatement statement = connection.prepareStatement(sql)) {
			statement.setLong(1, clientId);
			
			try (ResultSet resultSet = statement.executeQuery()) {
				
				while (resultSet.next()) {
					transactions.add(mapRow(resultSet));
				}
				
			}
			
		} catch (SQLException e) {
			throw new PersistenceException("finding transactions by client", e);
		} finally {
			DataSourceUtils.releaseConnection(connection, dataSource);
		}
		
		return transactions;
	}
	
	public BigDecimal sumAmountByType(TransactionType transactionType) {
		String sql = """
					 SELECT COALESCE(SUM(amount), 0) AS total_amount
					 FROM transactions
					 WHERE transaction_type = ?
					 """;
		Connection connection = DataSourceUtils.getConnection(dataSource);
		
		try (PreparedStatement statement = connection.prepareStatement(sql)) {
			statement.setString(1, transactionType.name());
			
			try (ResultSet resultSet = statement.executeQuery()) {
				resultSet.next();
				return resultSet.getBigDecimal("total_amount");
			}
			
		} catch (SQLException e) {
			throw new PersistenceException("summing transactions by type", e);
		} finally {
			DataSourceUtils.releaseConnection(connection, dataSource);
		}
		
	}
	
	// Private Helpers
	private Transaction mapRow(ResultSet resultSet) throws SQLException {
		Long transactionId = resultSet.getLong("transaction_id");
		String transactionCode = resultSet.getString("transaction_code");
		Client client = (Client) userDao.findByIdAndRole(resultSet.getLong("client_id"), Role.CLIENT);
		BigDecimal amount = resultSet.getBigDecimal("amount");
		TransactionType transactionType = TransactionType.valueOf(resultSet.getString("transaction_type"));
		Long invoiceId = resultSet.getLong("invoice_id");
		
		if (resultSet.wasNull()) {
			invoiceId = null;
		}
		
		LocalDateTime occurredAt = resultSet.getTimestamp("occurred_at").toLocalDateTime();
		
		return new Transaction(transactionId, transactionCode, client, amount, transactionType, invoiceId, occurredAt);
	}
	
}
