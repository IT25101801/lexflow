package lexflow.dao;

import lexflow.dto.consultation.ConflictCheckResult;
import lexflow.dto.consultation.ConflictSourceType;
import lexflow.exception.PersistenceException;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class ConflictCheckDao {
	
	private final DataSource dataSource;
	
	// Read
	public List<ConflictCheckResult> findByKeyword(String keyword) {
		String sql = """
					 SELECT u.name      AS matched_name,
					        u.role      AS source_type,
					        u.user_code AS detail,
					        1           AS source_order
					 FROM users u
					 WHERE LOWER(u.name) LIKE ?
					 UNION ALL
					 SELECT cnop.opposing_party,
					        'OPPOSING_PARTY',
					        cn.consultation_code,
					        2
					 FROM consultation_opposing_parties cnop
					 INNER JOIN consultations cn
					     ON cn.consultation_id = cnop.consultation_id
					 WHERE LOWER(cnop.opposing_party) LIKE ?
					 UNION ALL
					 SELECT ct.name,
					        'CONTACT',
					        cs.case_code,
					        3
					 FROM contacts ct
					 INNER JOIN cases cs
					     ON cs.case_id = ct.case_id
					 WHERE LOWER(ct.name) LIKE ?
					 ORDER BY source_order
					 """;
		String pattern = "%" + keyword.toLowerCase() + "%";
		List<ConflictCheckResult> results = new ArrayList<>();
		Connection connection = DataSourceUtils.getConnection(dataSource);
		
		try (PreparedStatement statement = connection.prepareStatement(sql)) {
			statement.setString(1, pattern);
			statement.setString(2, pattern);
			statement.setString(3, pattern);
			
			try (ResultSet resultSet = statement.executeQuery()) {
				
				while (resultSet.next()) {
					ConflictCheckResult result = new ConflictCheckResult();
					
					result.setMatchedName(resultSet.getString("matched_name"));
					result.setSourceType(ConflictSourceType.valueOf(resultSet.getString("source_type")));
					result.setDetail(resultSet.getString("detail"));
					
					results.add(result);
				}
				
			}
			
		} catch (SQLException e) {
			throw new PersistenceException("running the conflict check", e);
		} finally {
			DataSourceUtils.releaseConnection(connection, dataSource);
		}
		
		return results;
	}
	
}
