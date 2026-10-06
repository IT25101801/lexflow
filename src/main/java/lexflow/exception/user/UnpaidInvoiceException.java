package lexflow.exception.user;

import lexflow.exception.BusinessRuleViolationException;
import lombok.Getter;

import java.util.List;

@Getter
public class UnpaidInvoiceException extends BusinessRuleViolationException {
	
	private final Long clientId;
	private final List<String> invoiceCodes;
	
	public UnpaidInvoiceException(Long clientId, List<String> invoiceCodes) {
		super(String.format("This client cannot be deleted: %s %s %s still unpaid", invoiceCodes.size() == 1 ? "invoice" : "invoices", String.join(", ", invoiceCodes), invoiceCodes.size() == 1 ? "is" : "are"));
		this.clientId = clientId;
		this.invoiceCodes = invoiceCodes;
	}
	
}
