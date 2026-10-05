package lexflow.service;

import lexflow.dao.ConflictCheckDao;
import lexflow.dto.consultation.ConflictCheckResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ConflictCheckService {
	
	private final ConflictCheckDao conflictCheckDao;
	
	public List<ConflictCheckResult> check(String keyword) {
		
		if (keyword == null || keyword.isBlank()) {
			return List.of();
		}
		
		return conflictCheckDao.findByKeyword(keyword);
	}
	
}
