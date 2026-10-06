package lexflow.entity.finance;

import lexflow.entity.user.Client;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
public class Transaction {
	
	private Long transactionId;
	private String transactionCode;
	private final Client client;
	private final BigDecimal amount;
	private final TransactionType transactionType;
	private final Long invoiceId;
	private final LocalDateTime occurredAt;
	private String invoiceCode;
	
	public Transaction(Client client, BigDecimal amount, TransactionType transactionType, Long invoiceId, LocalDateTime occurredAt) {
		this.client = client;
		this.amount = amount;
		this.transactionType = transactionType;
		this.invoiceId = invoiceId;
		this.occurredAt = occurredAt;
	}
	
	public Transaction(Long transactionId, String transactionCode, Client client, BigDecimal amount, TransactionType transactionType, Long invoiceId, LocalDateTime occurredAt) {
		this.transactionId = transactionId;
		this.transactionCode = transactionCode;
		this.client = client;
		this.amount = amount;
		this.transactionType = transactionType;
		this.invoiceId = invoiceId;
		this.occurredAt = occurredAt;
	}
	
}
