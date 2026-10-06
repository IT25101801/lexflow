package lexflow.dto.document;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class SelectDocumentsRequest {
	
	@NotNull(message = "Select at least two documents")
	@Size(min = 2, message = "Select at least two documents")
	private List<Long> documentIds;
	
}
