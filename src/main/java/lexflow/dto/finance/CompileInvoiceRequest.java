package lexflow.dto.finance;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class CompileInvoiceRequest {
	
	private List<Long> taskIds;
	
}
