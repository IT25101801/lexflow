package lexflow.dao;

import lexflow.entity.case_.Task;
import lexflow.entity.scheduling.CaseEvent;
import lexflow.exception.EntityNotFoundException;
import lexflow.exception.PersistenceException;
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
public class TaskDao {
	
	private static final String SELECT_COLUMNS = """
												 task_id,
												 case_id,
												 title,
												 documenting_task,
												 scheduling_task,
												 billing_task,
												 billable_amount,
												 billed_invoice_id,
												 logged_at
												 """;
	
	private final DataSource dataSource;
	
	private final EventDao eventDao;
	
	// Create
	
	public Task insert(Task task) {
		String sql = """
					 INSERT INTO tasks (case_id, title, documenting_task, scheduling_task, billing_task, billable_amount, billed_invoice_id, logged_at)
					 VALUES (?, ?, ?, ?, ?, ?, ?, ?)
					 """;
		
		Connection connection = DataSourceUtils.getConnection(dataSource);
		
		try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
			statement.setLong(1, task.getCaseId());
			statement.setString(2, task.getTitle());
			statement.setBoolean(3, task.isDocumentingTask());
			statement.setBoolean(4, task.isSchedulingTask());
			statement.setBoolean(5, task.isBillingTask());
			
			if (task.getBillableAmount() != null) {
				statement.setBigDecimal(6, task.getBillableAmount());
			} else {
				statement.setNull(6, Types.DECIMAL);
			}
			
			if (task.getBilledInvoiceId() != null) {
				statement.setLong(7, task.getBilledInvoiceId());
			} else {
				statement.setNull(7, Types.BIGINT);
			}
			
			statement.setTimestamp(8, Timestamp.valueOf(task.getLoggedAt()));
			
			statement.executeUpdate();
			
			try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
				
				if (!generatedKeys.next()) {
					throw new PersistenceException("generating task ID");
				}
				
				task.setTaskId(generatedKeys.getLong(1));
			}
			
		} catch (SQLException e) {
			throw new PersistenceException("inserting task", e);
		} finally {
			DataSourceUtils.releaseConnection(connection, dataSource);
		}
		
		return task;
	}
	
	// Read
	
	public Task findById(Long taskId) {
		String sql = """
					 SELECT %s
					 FROM tasks
					 WHERE task_id = ?
					 """.formatted(SELECT_COLUMNS);
		
		Connection connection = DataSourceUtils.getConnection(dataSource);
		
		try (PreparedStatement statement = connection.prepareStatement(sql)) {
			statement.setLong(1, taskId);
			
			try (ResultSet resultSet = statement.executeQuery()) {
				
				if (resultSet.next()) {
					return mapRow(resultSet);
				}
				
				throw new EntityNotFoundException("Task", taskId.toString());
			}
			
		} catch (SQLException e) {
			throw new PersistenceException("finding task by ID", e);
		} finally {
			DataSourceUtils.releaseConnection(connection, dataSource);
		}
		
	}
	
	public List<Task> findByCaseId(Long caseId) {
		String sql = """
					 SELECT %s
					 FROM tasks
					 WHERE case_id = ?
					 ORDER BY logged_at DESC
					 """.formatted(SELECT_COLUMNS);
		
		List<Task> tasks = new ArrayList<>();
		
		Connection connection = DataSourceUtils.getConnection(dataSource);
		
		try (PreparedStatement statement = connection.prepareStatement(sql)) {
			statement.setLong(1, caseId);
			
			try (ResultSet resultSet = statement.executeQuery()) {
				
				while (resultSet.next()) {
					tasks.add(mapRow(resultSet));
				}
				
			}
			
		} catch (SQLException e) {
			throw new PersistenceException("finding tasks by case", e);
		} finally {
			DataSourceUtils.releaseConnection(connection, dataSource);
		}
		
		return tasks;
	}
	
	public List<Task> findUnbilledByCaseId(Long caseId) {
		String sql = """
					 SELECT %s
					 FROM tasks
					 WHERE case_id = ?
					   AND billing_task = 1
					   AND billed_invoice_id IS NULL
					 ORDER BY logged_at DESC
					 """.formatted(SELECT_COLUMNS);
		
		List<Task> tasks = new ArrayList<>();
		
		Connection connection = DataSourceUtils.getConnection(dataSource);
		
		try (PreparedStatement statement = connection.prepareStatement(sql)) {
			statement.setLong(1, caseId);
			
			try (ResultSet resultSet = statement.executeQuery()) {
				
				while (resultSet.next()) {
					tasks.add(mapRow(resultSet));
				}
				
			}
			
		} catch (SQLException e) {
			throw new PersistenceException("finding unbilled tasks by case", e);
		} finally {
			DataSourceUtils.releaseConnection(connection, dataSource);
		}
		
		return tasks;
	}
	
	public List<Task> findByBilledInvoiceId(Long billedInvoiceId) {
		String sql = """
					 SELECT %s
					 FROM tasks
					 WHERE billed_invoice_id = ?
					 ORDER BY logged_at DESC
					 """.formatted(SELECT_COLUMNS);
		
		List<Task> tasks = new ArrayList<>();
		
		Connection connection = DataSourceUtils.getConnection(dataSource);
		
		try (PreparedStatement statement = connection.prepareStatement(sql)) {
			statement.setLong(1, billedInvoiceId);
			
			try (ResultSet resultSet = statement.executeQuery()) {
				
				while (resultSet.next()) {
					tasks.add(mapRow(resultSet));
				}
				
			}
			
		} catch (SQLException e) {
			throw new PersistenceException("finding tasks by billed invoice", e);
		} finally {
			DataSourceUtils.releaseConnection(connection, dataSource);
		}
		
		return tasks;
	}
	
	// Update
	
	public Task update(Task task) {
		String sql = """
					 UPDATE tasks
					 SET title             = ?,
					     billable_amount   = ?,
					     billed_invoice_id = ?
					 WHERE task_id = ?
					 """;
		
		Connection connection = DataSourceUtils.getConnection(dataSource);
		
		try (PreparedStatement statement = connection.prepareStatement(sql)) {
			statement.setString(1, task.getTitle());
			
			if (task.getBillableAmount() != null) {
				statement.setBigDecimal(2, task.getBillableAmount());
			} else {
				statement.setNull(2, Types.DECIMAL);
			}
			
			if (task.getBilledInvoiceId() != null) {
				statement.setLong(3, task.getBilledInvoiceId());
			} else {
				statement.setNull(3, Types.BIGINT);
			}
			
			statement.setLong(4, task.getTaskId());
			
			statement.executeUpdate();
			
			return task;
		} catch (SQLException e) {
			throw new PersistenceException("updating task", e);
		} finally {
			DataSourceUtils.releaseConnection(connection, dataSource);
		}
		
	}
	
	// Private Helpers
	
	private Task mapRow(ResultSet resultSet) throws SQLException {
		Long taskId = resultSet.getLong("task_id");
		Long caseId = resultSet.getLong("case_id");
		String title = resultSet.getString("title");
		boolean documentingTask = resultSet.getBoolean("documenting_task");
		boolean schedulingTask = resultSet.getBoolean("scheduling_task");
		boolean billingTask = resultSet.getBoolean("billing_task");
		
		CaseEvent scheduledEvent = null;
		
		if (schedulingTask) {
			scheduledEvent = eventDao.findByTaskId(taskId);
		}
		
		BigDecimal billableAmount = resultSet.getBigDecimal("billable_amount");
		
		Long billedInvoiceId = resultSet.getLong("billed_invoice_id");
		
		if (resultSet.wasNull()) {
			billedInvoiceId = null;
		}
		
		LocalDateTime loggedAt = resultSet.getTimestamp("logged_at").toLocalDateTime();
		
		return new Task(taskId, caseId, title, documentingTask, schedulingTask, billingTask, scheduledEvent, billableAmount, billedInvoiceId, loggedAt);
	}
	
}
