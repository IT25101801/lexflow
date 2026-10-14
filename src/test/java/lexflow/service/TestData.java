package lexflow.service;

import lexflow.entity.case_.Case;
import lexflow.entity.case_.CaseResolutionStatus;
import lexflow.entity.case_.CaseStatus;
import lexflow.entity.consultation.CaseType;
import lexflow.entity.consultation.Consultation;
import lexflow.entity.consultation.ConsultationStatus;
import lexflow.entity.finance.Invoice;
import lexflow.entity.finance.InvoiceStatus;
import lexflow.entity.scheduling.ConsultationEvent;
import lexflow.entity.scheduling.EventStatus;
import lexflow.entity.user.AccountStatus;
import lexflow.entity.user.Associate;
import lexflow.entity.user.Client;
import lexflow.entity.user.Gender;
import lexflow.entity.user.Paralegal;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

final class TestData {

	private TestData() {
	}

	static Client client(long userId) {
		return new Client(userId, "CL-" + userId, "client" + userId + "@test.com", "x", "Client " + userId, LocalDate.of(1990, 1, 1), Gender.OTHER, List.of("Colombo"), List.of("0771234567"), "900000000V", null, AccountStatus.ACTIVE, LocalDateTime.now(), BigDecimal.ZERO);
	}

	static Associate associate(long userId) {
		return new Associate(userId, "AS-" + userId, "associate" + userId + "@test.com", "x", "Associate " + userId, LocalDate.of(1985, 1, 1), Gender.OTHER, List.of("Colombo"), List.of("0771234567"), "850000000V", null, AccountStatus.ACTIVE, LocalDateTime.now(), "BAR" + userId, null);
	}

	static Paralegal paralegal(long userId) {
		return new Paralegal(userId, "PL-" + userId, "paralegal" + userId + "@test.com", "x", "Paralegal " + userId, LocalDate.of(1992, 1, 1), Gender.OTHER, List.of("Colombo"), List.of("0771234567"), "920000000V", null, AccountStatus.ACTIVE, LocalDateTime.now());
	}

	static Consultation consultation(long consultationId, ConsultationStatus consultationStatus, Associate associate, Paralegal paralegal) {
		return new Consultation(consultationId, "CN-" + consultationId, client(1L), CaseType.CIVIL, List.of("Opponent"), "Description", consultationStatus, LocalDateTime.now(), associate, paralegal, null);
	}

	static Case case_(long caseId, Consultation consultation, CaseStatus caseStatus) {
		CaseResolutionStatus caseResolutionStatus = caseStatus == CaseStatus.CLOSED ? CaseResolutionStatus.WON : null;
		LocalDateTime closedAt = caseStatus == CaseStatus.CLOSED ? LocalDateTime.now() : null;

		return new Case(caseId, "CS-" + caseId, consultation, consultation.getClient(), consultation.getAssociate(), consultation.getParalegal(), caseStatus, LocalDateTime.now(), caseResolutionStatus, closedAt);
	}

	static ConsultationEvent meeting(long eventId, Consultation consultation, EventStatus eventStatus) {
		return new ConsultationEvent(eventId, "Meeting", LocalDate.now(), LocalTime.of(10, 0), LocalTime.of(11, 0), "Office", null, eventStatus, LocalDateTime.now(), null, null, consultation);
	}

	static Invoice invoice(long invoiceId, long caseId, InvoiceStatus invoiceStatus) {
		return new Invoice(invoiceId, "IN-" + invoiceId, caseId, new BigDecimal("100.00"), invoiceStatus, LocalDateTime.now(), null, null);
	}

}
