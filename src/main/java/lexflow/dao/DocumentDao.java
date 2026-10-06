package lexflow.dao;

import lexflow.entity.document.*;
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
public class DocumentDao {
	
	private static final String SELECT_COLUMNS = """
												 document_id,
												 document_type,
												 file_path,
												 original_filename,
												 uploaded_at,
												 user_id,
												 consultation_id,
												 case_id,
												 in_staging,
												 task_id,
												 invoice_id
												 """;
	
	private final DataSource dataSource;
	
	// Create
	
	public Document insert(Document document) {
		String sql = """
					 INSERT INTO documents (document_type, file_path, original_filename, uploaded_at, user_id, consultation_id, case_id, in_staging, task_id, invoice_id)
					 VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
					 """;
		
		Long userId = null;
		Long consultationId = null;
		Long caseId = null;
		Boolean inStaging = null;
		Long taskId = null;
		Long invoiceId = null;
		
		if (document instanceof UserDocument userDocument) {
			userId = userDocument.getUserId();
		} else if (document instanceof ConsultationDocument consultationDocument) {
			consultationId = consultationDocument.getConsultationId();
		} else if (document instanceof CaseDocument caseDocument) {
			caseId = caseDocument.getCaseId();
			inStaging = caseDocument.isInStaging();
			taskId = caseDocument.getTaskId();
		} else if (document instanceof InvoiceDocument invoiceDocument) {
			invoiceId = invoiceDocument.getInvoiceId();
		}
		
		Connection connection = DataSourceUtils.getConnection(dataSource);
		
		try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
			statement.setString(1, document.getDocumentType().name());
			statement.setString(2, document.getFilePath());
			statement.setString(3, document.getOriginalFilename());
			statement.setTimestamp(4, Timestamp.valueOf(document.getUploadedAt()));
			
			if (userId != null) {
				statement.setLong(5, userId);
			} else {
				statement.setNull(5, Types.BIGINT);
			}
			
			if (consultationId != null) {
				statement.setLong(6, consultationId);
			} else {
				statement.setNull(6, Types.BIGINT);
			}
			
			if (caseId != null) {
				statement.setLong(7, caseId);
			} else {
				statement.setNull(7, Types.BIGINT);
			}
			
			if (inStaging != null) {
				statement.setBoolean(8, inStaging);
			} else {
				statement.setNull(8, Types.BIT);
			}
			
			if (taskId != null) {
				statement.setLong(9, taskId);
			} else {
				statement.setNull(9, Types.BIGINT);
			}
			
			if (invoiceId != null) {
				statement.setLong(10, invoiceId);
			} else {
				statement.setNull(10, Types.BIGINT);
			}
			
			statement.executeUpdate();
			
			try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
				
				if (!generatedKeys.next()) {
					throw new PersistenceException("generating document ID");
				}
				
				document.setDocumentId(generatedKeys.getLong(1));
			}
			
		} catch (SQLException e) {
			throw new PersistenceException("inserting document", e);
		} finally {
			DataSourceUtils.releaseConnection(connection, dataSource);
		}
		
		return document;
	}
	
	// Read
	
	public Document findById(Long documentId) {
		String sql = """
					 SELECT %s
					 FROM documents
					 WHERE document_id = ?
					 """.formatted(SELECT_COLUMNS);
		
		Connection connection = DataSourceUtils.getConnection(dataSource);
		
		try (PreparedStatement statement = connection.prepareStatement(sql)) {
			statement.setLong(1, documentId);
			
			try (ResultSet resultSet = statement.executeQuery()) {
				
				if (resultSet.next()) {
					return mapRow(resultSet);
				}
				
				throw new EntityNotFoundException("Document", documentId.toString());
			}
			
		} catch (SQLException e) {
			throw new PersistenceException("finding document by ID", e);
		} finally {
			DataSourceUtils.releaseConnection(connection, dataSource);
		}
		
	}
	
	public NicDocument findNicByUserId(Long userId) {
		String sql = """
					 SELECT %s
					 FROM documents
					 WHERE user_id = ?
					   AND document_type = 'NIC_DOCUMENT'
					 """.formatted(SELECT_COLUMNS);
		
		Connection connection = DataSourceUtils.getConnection(dataSource);
		
		try (PreparedStatement statement = connection.prepareStatement(sql)) {
			statement.setLong(1, userId);
			
			try (ResultSet resultSet = statement.executeQuery()) {
				
				if (resultSet.next()) {
					return (NicDocument) mapRow(resultSet);
				}
				
				return null;
			}
			
		} catch (SQLException e) {
			throw new PersistenceException("finding NIC document by user", e);
		} finally {
			DataSourceUtils.releaseConnection(connection, dataSource);
		}
		
	}
	
	public BarDocument findBarByUserId(Long userId) {
		String sql = """
					 SELECT %s
					 FROM documents
					 WHERE user_id = ?
					   AND document_type = 'BAR_DOCUMENT'
					 """.formatted(SELECT_COLUMNS);
		
		Connection connection = DataSourceUtils.getConnection(dataSource);
		
		try (PreparedStatement statement = connection.prepareStatement(sql)) {
			statement.setLong(1, userId);
			
			try (ResultSet resultSet = statement.executeQuery()) {
				
				if (resultSet.next()) {
					return (BarDocument) mapRow(resultSet);
				}
				
				return null;
			}
			
		} catch (SQLException e) {
			throw new PersistenceException("finding bar document by user", e);
		} finally {
			DataSourceUtils.releaseConnection(connection, dataSource);
		}
		
	}
	
	public ConsultationDocument findConsultationDocumentById(Long documentId) {
		String sql = """
					 SELECT %s
					 FROM documents
					 WHERE document_id = ?
					   AND document_type = 'CONSULTATION_DOCUMENT'
					 """.formatted(SELECT_COLUMNS);
		
		Connection connection = DataSourceUtils.getConnection(dataSource);
		
		try (PreparedStatement statement = connection.prepareStatement(sql)) {
			statement.setLong(1, documentId);
			
			try (ResultSet resultSet = statement.executeQuery()) {
				
				if (resultSet.next()) {
					return (ConsultationDocument) mapRow(resultSet);
				}
				
				throw new EntityNotFoundException("ConsultationDocument", documentId.toString());
			}
			
		} catch (SQLException e) {
			throw new PersistenceException("finding consultation document by ID", e);
		} finally {
			DataSourceUtils.releaseConnection(connection, dataSource);
		}
		
	}
	
	public CaseDocument findCaseDocumentById(Long documentId) {
		String sql = """
					 SELECT %s
					 FROM documents
					 WHERE document_id = ?
					   AND document_type = 'CASE_DOCUMENT'
					 """.formatted(SELECT_COLUMNS);
		
		Connection connection = DataSourceUtils.getConnection(dataSource);
		
		try (PreparedStatement statement = connection.prepareStatement(sql)) {
			statement.setLong(1, documentId);
			
			try (ResultSet resultSet = statement.executeQuery()) {
				
				if (resultSet.next()) {
					return (CaseDocument) mapRow(resultSet);
				}
				
				throw new EntityNotFoundException("CaseDocument", documentId.toString());
			}
			
		} catch (SQLException e) {
			throw new PersistenceException("finding case document by ID", e);
		} finally {
			DataSourceUtils.releaseConnection(connection, dataSource);
		}
		
	}
	
	public InvoiceDocument findByInvoiceId(Long invoiceId) {
		String sql = """
					 SELECT %s
					 FROM documents
					 WHERE invoice_id = ?
					   AND document_type = 'INVOICE_DOCUMENT'
					 """.formatted(SELECT_COLUMNS);
		
		Connection connection = DataSourceUtils.getConnection(dataSource);
		
		try (PreparedStatement statement = connection.prepareStatement(sql)) {
			statement.setLong(1, invoiceId);
			
			try (ResultSet resultSet = statement.executeQuery()) {
				
				if (resultSet.next()) {
					return (InvoiceDocument) mapRow(resultSet);
				}
				
				return null;
			}
			
		} catch (SQLException e) {
			throw new PersistenceException("finding document by invoice", e);
		} finally {
			DataSourceUtils.releaseConnection(connection, dataSource);
		}
		
	}
	
	public List<ConsultationDocument> findByConsultationId(Long consultationId) {
		String sql = """
					 SELECT %s
					 FROM documents
					 WHERE consultation_id = ?
					   AND document_type = 'CONSULTATION_DOCUMENT'
					 """.formatted(SELECT_COLUMNS);
		
		List<ConsultationDocument> documents = new ArrayList<>();
		Connection connection = DataSourceUtils.getConnection(dataSource);
		
		try (PreparedStatement statement = connection.prepareStatement(sql)) {
			statement.setLong(1, consultationId);
			
			try (ResultSet resultSet = statement.executeQuery()) {
				
				while (resultSet.next()) {
					documents.add((ConsultationDocument) mapRow(resultSet));
				}
				
			}
			
		} catch (SQLException e) {
			throw new PersistenceException("finding documents by consultation", e);
		} finally {
			DataSourceUtils.releaseConnection(connection, dataSource);
		}
		
		return documents;
	}
	
	public List<CaseDocument> findByCaseIdAndInStaging(Long caseId, boolean inStaging) {
		String sql = """
					 SELECT %s
					 FROM documents
					 WHERE case_id = ?
					   AND in_staging = ?
					   AND document_type = 'CASE_DOCUMENT'
					 """.formatted(SELECT_COLUMNS);
		
		List<CaseDocument> documents = new ArrayList<>();
		Connection connection = DataSourceUtils.getConnection(dataSource);
		
		try (PreparedStatement statement = connection.prepareStatement(sql)) {
			statement.setLong(1, caseId);
			statement.setBoolean(2, inStaging);
			
			try (ResultSet resultSet = statement.executeQuery()) {
				
				while (resultSet.next()) {
					documents.add((CaseDocument) mapRow(resultSet));
				}
				
			}
			
		} catch (SQLException e) {
			throw new PersistenceException("finding documents by case and staging", e);
		} finally {
			DataSourceUtils.releaseConnection(connection, dataSource);
		}
		
		return documents;
	}
	
	public List<CaseDocument> findByTaskId(Long taskId) {
		String sql = """
					 SELECT %s
					 FROM documents
					 WHERE task_id = ?
					   AND document_type = 'CASE_DOCUMENT'
					 """.formatted(SELECT_COLUMNS);
		
		List<CaseDocument> documents = new ArrayList<>();
		Connection connection = DataSourceUtils.getConnection(dataSource);
		
		try (PreparedStatement statement = connection.prepareStatement(sql)) {
			statement.setLong(1, taskId);
			
			try (ResultSet resultSet = statement.executeQuery()) {
				
				while (resultSet.next()) {
					documents.add((CaseDocument) mapRow(resultSet));
				}
				
			}
			
		} catch (SQLException e) {
			throw new PersistenceException("finding documents by task", e);
		} finally {
			DataSourceUtils.releaseConnection(connection, dataSource);
		}
		
		return documents;
	}
	
	public List<CaseDocument> findByCaseIdAndFilenameKeyword(Long caseId, String keyword) {
		String sql = """
					 SELECT %s
					 FROM documents
					 WHERE case_id = ?
					   AND in_staging = 0
					   AND document_type = 'CASE_DOCUMENT'
					   AND LOWER(original_filename) LIKE ?
					 """.formatted(SELECT_COLUMNS);
		
		List<CaseDocument> documents = new ArrayList<>();
		Connection connection = DataSourceUtils.getConnection(dataSource);
		
		try (PreparedStatement statement = connection.prepareStatement(sql)) {
			statement.setLong(1, caseId);
			statement.setString(2, "%" + keyword.toLowerCase() + "%");
			
			try (ResultSet resultSet = statement.executeQuery()) {
				
				while (resultSet.next()) {
					documents.add((CaseDocument) mapRow(resultSet));
				}
				
			}
			
		} catch (SQLException e) {
			throw new PersistenceException("finding documents by case and filename keyword", e);
		} finally {
			DataSourceUtils.releaseConnection(connection, dataSource);
		}
		
		return documents;
	}
	
	// Update
	
	public void updateOriginalFilename(Long documentId, String originalFilename) {
		String sql = """
					 UPDATE documents
					 SET original_filename = ?
					 WHERE document_id = ?
					 """;
		
		Connection connection = DataSourceUtils.getConnection(dataSource);
		
		try (PreparedStatement statement = connection.prepareStatement(sql)) {
			statement.setString(1, originalFilename);
			statement.setLong(2, documentId);
			
			statement.executeUpdate();
		} catch (SQLException e) {
			throw new PersistenceException("updating document original filename", e);
		} finally {
			DataSourceUtils.releaseConnection(connection, dataSource);
		}
		
	}
	
	public void updateInStaging(Long documentId, boolean inStaging) {
		String sql = """
					 UPDATE documents
					 SET in_staging = ?
					 WHERE document_id = ?
					 """;
		
		Connection connection = DataSourceUtils.getConnection(dataSource);
		
		try (PreparedStatement statement = connection.prepareStatement(sql)) {
			statement.setBoolean(1, inStaging);
			statement.setLong(2, documentId);
			
			statement.executeUpdate();
		} catch (SQLException e) {
			throw new PersistenceException("updating document staging", e);
		} finally {
			DataSourceUtils.releaseConnection(connection, dataSource);
		}
		
	}
	
	public void replace(Long documentId, String filePath, String originalFilename, LocalDateTime uploadedAt) {
		String sql = """
					 UPDATE documents
					 SET file_path         = ?,
					     original_filename = ?,
					     uploaded_at       = ?
					 WHERE document_id = ?
					 """;
		
		Connection connection = DataSourceUtils.getConnection(dataSource);
		
		try (PreparedStatement statement = connection.prepareStatement(sql)) {
			statement.setString(1, filePath);
			statement.setString(2, originalFilename);
			statement.setTimestamp(3, Timestamp.valueOf(uploadedAt));
			statement.setLong(4, documentId);
			
			statement.executeUpdate();
		} catch (SQLException e) {
			throw new PersistenceException("replacing document", e);
		} finally {
			DataSourceUtils.releaseConnection(connection, dataSource);
		}
		
	}
	
	// Delete
	
	public void deleteAllById(List<Long> documentIds) {
		
		if (documentIds == null || documentIds.isEmpty()) {
			return;
		}
		
		String sql = """
					 DELETE
					 FROM documents
					 WHERE document_id = ?
					 """;
		
		Connection connection = DataSourceUtils.getConnection(dataSource);
		
		try (PreparedStatement statement = connection.prepareStatement(sql)) {
			
			for (Long documentId : documentIds) {
				statement.setLong(1, documentId);
				statement.executeUpdate();
			}
			
		} catch (SQLException e) {
			throw new PersistenceException("deleting documents", e);
		} finally {
			DataSourceUtils.releaseConnection(connection, dataSource);
		}
		
	}
	
	// Private Helpers
	
	private Document mapRow(ResultSet resultSet) throws SQLException {
		Long documentId = resultSet.getLong("document_id");
		DocumentType documentType = DocumentType.valueOf(resultSet.getString("document_type"));
		String filePath = resultSet.getString("file_path");
		String originalFilename = resultSet.getString("original_filename");
		LocalDateTime uploadedAt = resultSet.getTimestamp("uploaded_at").toLocalDateTime();
		
		if (documentType == DocumentType.NIC_DOCUMENT) {
			return new NicDocument(documentId, filePath, originalFilename, uploadedAt, resultSet.getLong("user_id"));
		}
		
		if (documentType == DocumentType.BAR_DOCUMENT) {
			return new BarDocument(documentId, filePath, originalFilename, uploadedAt, resultSet.getLong("user_id"));
		}
		
		if (documentType == DocumentType.CONSULTATION_DOCUMENT) {
			return new ConsultationDocument(documentId, filePath, originalFilename, uploadedAt, resultSet.getLong("consultation_id"));
		}
		
		if (documentType == DocumentType.CASE_DOCUMENT) {
			long caseId = resultSet.getLong("case_id");
			boolean inStaging = resultSet.getBoolean("in_staging");
			Long taskId = resultSet.getLong("task_id");
			
			if (resultSet.wasNull()) {
				taskId = null;
			}
			
			return new CaseDocument(documentId, filePath, originalFilename, uploadedAt, caseId, inStaging, taskId);
		}
		
		return new InvoiceDocument(documentId, filePath, originalFilename, uploadedAt, resultSet.getLong("invoice_id"));
	}
	
}
