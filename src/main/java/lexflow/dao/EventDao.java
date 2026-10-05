package lexflow.dao;

import lexflow.entity.scheduling.*;
import lexflow.exception.EntityNotFoundException;
import lexflow.exception.PersistenceException;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class EventDao {
	
	private static final String SELECT_COLUMNS = """
												 event_id,
												 event_type,
												 subject,
												 date,
												 start_time,
												 end_time,
												 location,
												 notes,
												 event_status,
												 scheduled_at,
												 completed_at,
												 canceled_at,
												 consultation_id,
												 case_id,
												 task_id
												 """;
	
	private final DataSource dataSource;
	private final ConsultationDao consultationDao;
	private final CaseDao caseDao;
	
	// ---- Create ----
	
	public Event insert(Event event) {
		String sql = """
					 INSERT INTO events (event_type, subject, date, start_time, end_time, location, notes, event_status, scheduled_at, consultation_id, case_id, task_id)
					 VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
					 """;
		
		Long consultationId = null;
		Long caseId = null;
		Long taskId = null;
		
		if (event instanceof ConsultationEvent consultationEvent) {
			consultationId = consultationEvent.getConsultation().getConsultationId();
		} else if (event instanceof CaseEvent caseEvent) {
			caseId = caseEvent.getCase_().getCaseId();
			taskId = caseEvent.getTaskId();
		}
		
		Connection connection = DataSourceUtils.getConnection(dataSource);
		
		try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
			statement.setString(1, event.getEventType().name());
			statement.setString(2, event.getSubject());
			statement.setDate(3, Date.valueOf(event.getDate()));
			statement.setTime(4, Time.valueOf(event.getStartTime()));
			statement.setTime(5, Time.valueOf(event.getEndTime()));
			statement.setString(6, event.getLocation());
			statement.setString(7, event.getNotes());
			statement.setString(8, event.getEventStatus().name());
			statement.setTimestamp(9, Timestamp.valueOf(event.getScheduledAt()));
			
			if (consultationId != null) {
				statement.setLong(10, consultationId);
			} else {
				statement.setNull(10, Types.BIGINT);
			}
			
			if (caseId != null) {
				statement.setLong(11, caseId);
			} else {
				statement.setNull(11, Types.BIGINT);
			}
			
			if (taskId != null) {
				statement.setLong(12, taskId);
			} else {
				statement.setNull(12, Types.BIGINT);
			}
			
			statement.executeUpdate();
			
			try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
				
				if (generatedKeys.next()) {
					event.setEventId(generatedKeys.getLong(1));
					
					return event;
				}
				
				throw new PersistenceException("generating event ID");
			}
			
		} catch (SQLException e) {
			throw new PersistenceException("inserting event", e);
		} finally {
			DataSourceUtils.releaseConnection(connection, dataSource);
		}
		
	}
	
	// ---- Read ----
	
	public Event findById(Long eventId) {
		String sql = """
					 SELECT %s
					 FROM events
					 WHERE event_id = ?
					 """.formatted(SELECT_COLUMNS);
		
		Connection connection = DataSourceUtils.getConnection(dataSource);
		
		try (PreparedStatement statement = connection.prepareStatement(sql)) {
			statement.setLong(1, eventId);
			
			try (ResultSet resultSet = statement.executeQuery()) {
				
				if (resultSet.next()) {
					return mapRow(resultSet);
				}
				
				throw new EntityNotFoundException("Event", eventId.toString());
			}
			
		} catch (SQLException e) {
			throw new PersistenceException("finding event by ID", e);
		} finally {
			DataSourceUtils.releaseConnection(connection, dataSource);
		}
		
	}
	
	public List<Event> findAll() {
		String sql = """
					 SELECT %s
					 FROM events
					 ORDER BY date DESC,
					          start_time DESC
					 """.formatted(SELECT_COLUMNS);
		
		List<Event> events = new ArrayList<>();
		
		Connection connection = DataSourceUtils.getConnection(dataSource);
		
		try (PreparedStatement statement = connection.prepareStatement(sql)) {
			
			try (ResultSet resultSet = statement.executeQuery()) {
				
				while (resultSet.next()) {
					events.add(mapRow(resultSet));
				}
				
			}
			
		} catch (SQLException e) {
			throw new PersistenceException("finding events", e);
		} finally {
			DataSourceUtils.releaseConnection(connection, dataSource);
		}
		
		return events;
	}
	
	public List<ConsultationEvent> findByConsultationId(Long consultationId) {
		String sql = """
					 SELECT %s
					 FROM events
					 WHERE consultation_id = ?
					 ORDER BY scheduled_at DESC
					 """.formatted(SELECT_COLUMNS);
		
		List<ConsultationEvent> events = new ArrayList<>();
		
		Connection connection = DataSourceUtils.getConnection(dataSource);
		
		try (PreparedStatement statement = connection.prepareStatement(sql)) {
			statement.setLong(1, consultationId);
			
			try (ResultSet resultSet = statement.executeQuery()) {
				
				while (resultSet.next()) {
					events.add((ConsultationEvent) mapRow(resultSet));
				}
				
			}
			
		} catch (SQLException e) {
			throw new PersistenceException("finding events by consultation", e);
		} finally {
			DataSourceUtils.releaseConnection(connection, dataSource);
		}
		
		return events;
	}
	
	public List<CaseEvent> findByCaseId(Long caseId) {
		String sql = """
					 SELECT %s
					 FROM events
					 WHERE case_id = ?
					 ORDER BY scheduled_at DESC
					 """.formatted(SELECT_COLUMNS);
		
		List<CaseEvent> events = new ArrayList<>();
		
		Connection connection = DataSourceUtils.getConnection(dataSource);
		
		try (PreparedStatement statement = connection.prepareStatement(sql)) {
			statement.setLong(1, caseId);
			
			try (ResultSet resultSet = statement.executeQuery()) {
				
				while (resultSet.next()) {
					events.add((CaseEvent) mapRow(resultSet));
				}
				
			}
			
		} catch (SQLException e) {
			throw new PersistenceException("finding events by case", e);
		} finally {
			DataSourceUtils.releaseConnection(connection, dataSource);
		}
		
		return events;
	}
	
	public CaseEvent findByTaskId(Long taskId) {
		String sql = """
					 SELECT %s
					 FROM events
					 WHERE task_id = ?
					 """.formatted(SELECT_COLUMNS);
		
		Connection connection = DataSourceUtils.getConnection(dataSource);
		
		try (PreparedStatement statement = connection.prepareStatement(sql)) {
			statement.setLong(1, taskId);
			
			try (ResultSet resultSet = statement.executeQuery()) {
				
				if (resultSet.next()) {
					return (CaseEvent) mapRow(resultSet);
				}
				
				throw new EntityNotFoundException("Event for task", taskId.toString());
			}
			
		} catch (SQLException e) {
			throw new PersistenceException("finding event by task", e);
		} finally {
			DataSourceUtils.releaseConnection(connection, dataSource);
		}
		
	}
	
	// ---- Update ----
	
	public Event update(Event event) {
		String sql = """
					 UPDATE events
					 SET subject      = ?,
					     date         = ?,
					     start_time   = ?,
					     end_time     = ?,
					     location     = ?,
					     notes        = ?,
					     event_status = ?,
					     completed_at = ?,
					     canceled_at  = ?
					 WHERE event_id = ?
					 """;
		
		Connection connection = DataSourceUtils.getConnection(dataSource);
		
		try (PreparedStatement statement = connection.prepareStatement(sql)) {
			statement.setString(1, event.getSubject());
			statement.setDate(2, Date.valueOf(event.getDate()));
			statement.setTime(3, Time.valueOf(event.getStartTime()));
			statement.setTime(4, Time.valueOf(event.getEndTime()));
			statement.setString(5, event.getLocation());
			statement.setString(6, event.getNotes());
			statement.setString(7, event.getEventStatus().name());
			
			if (event.getCompletedAt() != null) {
				statement.setTimestamp(8, Timestamp.valueOf(event.getCompletedAt()));
			} else {
				statement.setNull(8, Types.TIMESTAMP);
			}
			
			if (event.getCanceledAt() != null) {
				statement.setTimestamp(9, Timestamp.valueOf(event.getCanceledAt()));
			} else {
				statement.setNull(9, Types.TIMESTAMP);
			}
			
			statement.setLong(10, event.getEventId());
			
			statement.executeUpdate();
			
			return event;
		} catch (SQLException e) {
			throw new PersistenceException("updating event", e);
		} finally {
			DataSourceUtils.releaseConnection(connection, dataSource);
		}
		
	}
	
	// ---- Private Helpers ----
	
	private Event mapRow(ResultSet resultSet) throws SQLException {
		Long eventId = resultSet.getLong("event_id");
		EventType eventType = EventType.valueOf(resultSet.getString("event_type"));
		String subject = resultSet.getString("subject");
		LocalDate date = resultSet.getDate("date").toLocalDate();
		LocalTime startTime = resultSet.getTime("start_time").toLocalTime();
		LocalTime endTime = resultSet.getTime("end_time").toLocalTime();
		String location = resultSet.getString("location");
		String notes = resultSet.getString("notes");
		EventStatus eventStatus = EventStatus.valueOf(resultSet.getString("event_status"));
		LocalDateTime scheduledAt = resultSet.getTimestamp("scheduled_at").toLocalDateTime();
		
		LocalDateTime completedAt = null;
		Timestamp completedAtSql = resultSet.getTimestamp("completed_at");
		
		if (completedAtSql != null) {
			completedAt = completedAtSql.toLocalDateTime();
		}
		
		LocalDateTime canceledAt = null;
		Timestamp canceledAtSql = resultSet.getTimestamp("canceled_at");
		
		if (canceledAtSql != null) {
			canceledAt = canceledAtSql.toLocalDateTime();
		}
		
		if (eventType == EventType.CONSULTATION_EVENT) {
			return new ConsultationEvent(eventId, subject, date, startTime, endTime, location, notes, eventStatus, scheduledAt, completedAt, canceledAt, consultationDao.findById(resultSet.getLong("consultation_id")));
		}
		
		return new CaseEvent(eventId, subject, date, startTime, endTime, location, notes, eventStatus, scheduledAt, completedAt, canceledAt, caseDao.findById(resultSet.getLong("case_id")), resultSet.getLong("task_id"));
		
	}
	
}
