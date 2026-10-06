package lexflow.service;

import lexflow.dao.AnalyticsDao;
import lexflow.dao.UserDao;
import lexflow.entity.case_.CaseResolutionStatus;
import lexflow.entity.consultation.CaseType;
import lexflow.entity.finance.TransactionType;
import lexflow.entity.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AnalyticsService {
	
	private final AnalyticsDao analyticsDao;
	private final UserDao userDao;
	
	// ---- Case ----
	
	public Map<CaseResolutionStatus, Integer> caseResolutionBreakdown() {
		return analyticsDao.countCasesByResolutionStatus();
	}
	
	public Double averageCaseDurationDays() {
		return analyticsDao.averageCaseDurationDays();
	}
	
	public Map<User, Integer> closedCaseCountByAssociate() {
		return resolveStaffWorkload(analyticsDao.countClosedCasesByAssociate());
	}
	
	public Map<User, Integer> closedCaseCountByParalegal() {
		return resolveStaffWorkload(analyticsDao.countClosedCasesByParalegal());
	}
	
	public double winRateForAssociate(Long associateId) {
		return winRate(analyticsDao.countCasesByResolutionStatusForAssociate(associateId));
	}
	
	public double winRateForParalegal(Long paralegalId) {
		return winRate(analyticsDao.countCasesByResolutionStatusForParalegal(paralegalId));
	}
	
	public Double averageCaseDurationDaysForAssociate(Long associateId) {
		return analyticsDao.averageCaseDurationDaysForAssociate(associateId);
	}
	
	public Double averageCaseDurationDaysForParalegal(Long paralegalId) {
		return analyticsDao.averageCaseDurationDaysForParalegal(paralegalId);
	}
	
	// ---- Invoice ----
	
	public Map<Integer, BigDecimal> revenueByMonth(int year) {
		Map<Integer, BigDecimal> sums = analyticsDao.sumPaidRevenueByMonth(year);
		Map<Integer, BigDecimal> filled = new LinkedHashMap<>();
		
		for (int month = 1; month <= 12; month++) {
			BigDecimal total = sums.get(month);
			filled.put(month, total != null ? total : BigDecimal.ZERO);
		}
		
		return filled;
	}
	
	public Map<CaseType, BigDecimal> revenueByCaseType() {
		return analyticsDao.sumPaidRevenueByCaseType();
	}
	
	public BigDecimal outstandingInvoiceTotal() {
		return analyticsDao.sumUnpaidInvoiceTotal();
	}
	
	public Double averageDaysToPayment() {
		return analyticsDao.averageDaysToPayment();
	}
	
	// ---- Transaction ----
	
	public Map<TransactionType, Integer> topUpCountComparison() {
		return analyticsDao.countTopUpsByType();
	}
	
	// ---- User ----
	
	public BigDecimal totalClientWalletFloat() {
		return analyticsDao.sumClientWalletBalance();
	}
	
	// ---- Task ----
	
	public BigDecimal billableAmountThisMonthForAssociate(Long associateId) {
		LocalDate now = LocalDate.now();
		return analyticsDao.sumBillableAmountForAssociateInMonth(associateId, now.getYear(), now.getMonthValue());
	}
	
	public BigDecimal billableAmountLastMonthForAssociate(Long associateId) {
		LocalDate lastMonth = LocalDate.now().minusMonths(1);
		return analyticsDao.sumBillableAmountForAssociateInMonth(associateId, lastMonth.getYear(), lastMonth.getMonthValue());
	}
	
	public BigDecimal billableAmountThisMonthForParalegal(Long paralegalId) {
		LocalDate now = LocalDate.now();
		return analyticsDao.sumBillableAmountForParalegalInMonth(paralegalId, now.getYear(), now.getMonthValue());
	}
	
	public BigDecimal billableAmountLastMonthForParalegal(Long paralegalId) {
		LocalDate lastMonth = LocalDate.now().minusMonths(1);
		return analyticsDao.sumBillableAmountForParalegalInMonth(paralegalId, lastMonth.getYear(), lastMonth.getMonthValue());
	}
	
	// ---- Private Helpers ----
	
	private double winRate(Map<CaseResolutionStatus, Integer> breakdown) {
		int total = 0;
		int won = 0;
		
		for (Map.Entry<CaseResolutionStatus, Integer> entry : breakdown.entrySet()) {
			total += entry.getValue();
			
			if (entry.getKey() == CaseResolutionStatus.WON) {
				won = entry.getValue();
			}
			
		}
		
		if (total == 0) {
			return 0.0;
		}
		
		return (won * 100.0) / total;
	}
	
	private Map<User, Integer> resolveStaffWorkload(Map<Long, Integer> countsByUserId) {
		Map<User, Integer> workload = new LinkedHashMap<>();
		
		for (Map.Entry<Long, Integer> entry : countsByUserId.entrySet()) {
			User staff = userDao.findById(entry.getKey());
			workload.put(staff, entry.getValue());
		}
		
		return workload;
	}
	
}
