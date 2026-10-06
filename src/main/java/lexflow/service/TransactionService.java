package lexflow.service;

import lexflow.dao.TransactionDao;
import lexflow.entity.finance.Transaction;
import lexflow.entity.finance.TransactionType;
import lexflow.entity.user.Client;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TransactionService {
	
	private final TransactionDao transactionDao;
	
	// Create
	
	public Transaction record(Client client, BigDecimal amount, TransactionType transactionType, Long invoiceId) {
		Transaction transaction = new Transaction(client, amount, transactionType, invoiceId, LocalDateTime.now());
		
		return transactionDao.insert(transaction);
	}
	
	public Transaction topUpByCard(Client client, BigDecimal amount) {
		return record(client, amount, TransactionType.CARD_TOP_UP, null);
	}
	
	public Transaction topUpByCash(Client client, BigDecimal amount) {
		return record(client, amount, TransactionType.CASH_TOP_UP, null);
	}
	
	public Transaction withdraw(Client client, BigDecimal amount) {
		return record(client, amount, TransactionType.WITHDRAWAL, null);
	}
	
	// Read
	
	public List<Transaction> findAll() {
		return transactionDao.findAll();
	}
	
	public List<Transaction> findAllForClient(Client client) {
		return transactionDao.findByClientId(client.getUserId());
	}
	
	// Firm Revenue
	
	public BigDecimal totalCharges() {
		return transactionDao.sumAmountByType(TransactionType.CHARGE);
	}
	
	public BigDecimal totalRefunds() {
		return transactionDao.sumAmountByType(TransactionType.REFUND);
	}
	
	public BigDecimal netRevenue() {
		return totalCharges().subtract(totalRefunds());
	}
	
}
