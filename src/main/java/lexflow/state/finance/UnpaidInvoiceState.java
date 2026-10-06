package lexflow.state.finance;

import lexflow.entity.finance.Invoice;
import lexflow.entity.finance.InvoiceStatus;
import lexflow.entity.user.Client;
import lexflow.exception.finance.InvalidInvoiceStateException;
import lexflow.service.InvoiceService;

public class UnpaidInvoiceState implements InvoiceState {
	
	@Override
	public Invoice charge(Invoice invoice, InvoiceService invoiceService, Client client) {
		return invoiceService.applyPayment(invoice, client);
	}
	
	@Override
	public Invoice voidInvoice(Invoice invoice, InvoiceService invoiceService) {
		return invoiceService.applyVoid(invoice);
	}
	
	@Override
	public Invoice voidAndRefund(Invoice invoice, InvoiceService invoiceService, Client client) {
		throw new InvalidInvoiceStateException(invoice.getInvoiceId(), invoice.getInvoiceStatus(), InvoiceStatus.PAID);
	}
	
}
