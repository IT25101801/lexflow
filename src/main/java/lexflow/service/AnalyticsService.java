package lexflow.service;

import lexflow.dao.AnalyticsDao;
import lexflow.entity.case_.CaseResolutionStatus;
import lexflow.entity.consultation.ConsultationStatus;
import lexflow.entity.document.DocumentType;
import lexflow.entity.scheduling.EventStatus;
import lexflow.entity.user.Role;
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
	
	// User
	
	public Map<Role, Integer> activeUserCountByRole() {
		return analyticsDao.countActiveUsersByRole();
	}
	
	public BigDecimal totalClientWalletFloat() {
		return analyticsDao.sumClientWalletBalance();
	}
	
	// Consultation
	
	public Map<ConsultationStatus, Integer> consultationStatusBreakdown() {
		return analyticsDao.countConsultationsByStatus();
	}
	
	public Double consultationAcceptanceRate() {
		Map<ConsultationStatus, Integer> consultationStatusBreakdown = analyticsDao.countConsultationsByStatus();
		int accepted = consultationStatusBreakdown.getOrDefault(ConsultationStatus.ACCEPTED_BY_ASSOCIATE, 0);
		int rejected = consultationStatusBreakdown.getOrDefault(ConsultationStatus.REJECTED_BY_PARTNER, 0) + consultationStatusBreakdown.getOrDefault(ConsultationStatus.REJECTED_BY_ASSOCIATE, 0);
		
		if (accepted + rejected == 0) {
			return null;
		}
		
		return accepted * 100.0 / (accepted + rejected);
	}
	
	// Case
	
	public Map<CaseResolutionStatus, Integer> caseResolutionBreakdown() {
		return analyticsDao.countCasesByResolutionStatus();
	}
	
	public Double averageCaseDurationDays() {
		return analyticsDao.averageCaseDurationDays();
	}
	
	// Document
	
	public Map<DocumentType, Integer> documentCountByType() {
		return analyticsDao.countDocumentsByType();
	}
	
	public int stagedDocumentCount() {
		return analyticsDao.countStagedDocuments();
	}
	
	// Invoice
	
	public Map<Integer, BigDecimal> revenueByMonth(int year) {
		Map<Integer, BigDecimal> sums = analyticsDao.sumPaidRevenueByMonth(year);
		Map<Integer, BigDecimal> monthlyRevenue = new LinkedHashMap<>();
		
		for (int month = 1; month <= 12; month++) {
			BigDecimal total = sums.get(month);
			monthlyRevenue.put(month, total != null ? total : BigDecimal.ZERO);
		}
		
		return monthlyRevenue;
	}
	
	public BigDecimal outstandingInvoiceTotal() {
		return analyticsDao.sumUnpaidInvoiceTotal();
	}
	
	// Event
	
	public Map<EventStatus, Integer> eventStatusBreakdown() {
		return analyticsDao.countEventsByStatus();
	}
	
	public int upcomingEventCountThisWeek() {
		LocalDate today = LocalDate.now();
		
		return analyticsDao.countScheduledEventsBetween(today, today.plusDays(7));
	}
	
}
