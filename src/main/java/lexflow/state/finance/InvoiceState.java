package lexflow.state.finance;

import lexflow.entity.finance.Invoice;
import lexflow.entity.user.Client;
import lexflow.service.InvoiceService;

public interface InvoiceState {
	
	Invoice charge(Invoice invoice, InvoiceService invoiceService, Client client);
	
	Invoice void_(Invoice invoice, InvoiceService invoiceService);
	
	Invoice voidAndRefund(Invoice invoice, InvoiceService invoiceService, Client client);
	
}
