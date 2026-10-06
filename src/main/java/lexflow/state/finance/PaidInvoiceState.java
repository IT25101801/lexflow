package lexflow.state.finance;

import lexflow.entity.finance.Invoice;
import lexflow.entity.finance.InvoiceStatus;
import lexflow.entity.user.Client;
import lexflow.exception.finance.InvalidInvoiceStateException;
import lexflow.service.InvoiceService;

public class PaidInvoiceState implements InvoiceState {
	
	@Override
	public Invoice charge(Invoice invoice, InvoiceService invoiceService, Client client) {
		throw new InvalidInvoiceStateException(invoice.getInvoiceId(), invoice.getInvoiceStatus(), InvoiceStatus.UNPAID);
	}
	
	@Override
	public Invoice voidInvoice(Invoice invoice, InvoiceService invoiceService) {
		throw new InvalidInvoiceStateException(invoice.getInvoiceId(), invoice.getInvoiceStatus(), InvoiceStatus.UNPAID);
	}
	
	@Override
	public Invoice voidAndRefund(Invoice invoice, InvoiceService invoiceService, Client client) {
		return invoiceService.applyVoidAndRefund(invoice, client);
	}
	
}
