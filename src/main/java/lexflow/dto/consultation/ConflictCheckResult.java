package lexflow.dto.consultation;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ConflictCheckResult {
	
	private String matchedName;
	private ConflictSourceType sourceType;
	private String detail;
	
}
