package lexflow.state.finance;

import lexflow.entity.finance.Invoice;
import lexflow.entity.finance.InvoiceStatus;
import lexflow.entity.user.Client;
import lexflow.exception.finance.InvalidInvoiceStateException;
import lexflow.service.InvoiceService;

public class VoidedInvoiceState implements InvoiceState {
	
	@Override
	public Invoice charge(Invoice invoice, InvoiceService invoiceService, Client client) {
		throw new InvalidInvoiceStateException(invoice.getInvoiceId(), invoice.getInvoiceStatus(), InvoiceStatus.UNPAID);
	}
	
	@Override
	public Invoice void_(Invoice invoice, InvoiceService invoiceService) {
		throw new InvalidInvoiceStateException(invoice.getInvoiceId(), invoice.getInvoiceStatus(), InvoiceStatus.UNPAID);
	}
	
	@Override
	public Invoice voidAndRefund(Invoice invoice, InvoiceService invoiceService, Client client) {
		throw new InvalidInvoiceStateException(invoice.getInvoiceId(), invoice.getInvoiceStatus(), InvoiceStatus.PAID);
	}
	
}
