package lexflow.exception.finance;

import lexflow.entity.finance.InvoiceStatus;
import lexflow.exception.InvalidStateException;
import lombok.Getter;

@Getter
public class InvalidInvoiceStateException extends InvalidStateException {
	
	private final Long invoiceId;
	private final InvoiceStatus currentStatus;
	private final InvoiceStatus requiredStatus;
	
	public InvalidInvoiceStateException(Long invoiceId, InvoiceStatus currentStatus, InvoiceStatus requiredStatus) {
		super(String.format("This invoice cannot be processed: status must be %s, but it is currently %s", requiredStatus, currentStatus));
		this.invoiceId = invoiceId;
		this.currentStatus = currentStatus;
		this.requiredStatus = requiredStatus;
	}
	
}
