package lexflow.entity.case_;

import lexflow.entity.consultation.Consultation;
import lexflow.entity.user.Associate;
import lexflow.entity.user.Client;
import lexflow.entity.user.Paralegal;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class Case {
	
	private Long caseId;
	private String caseCode;
	private final Consultation consultation;
	private final Client client;
	private Associate associate;
	private Paralegal paralegal;
	private CaseStatus caseStatus;
	private final LocalDateTime openedAt;
	private CaseResolutionStatus caseResolutionStatus;
	private LocalDateTime closedAt;
	
	public Case(Consultation consultation, Client client, Associate associate, Paralegal paralegal, LocalDateTime openedAt) {
		this.consultation = consultation;
		this.client = client;
		this.associate = associate;
		this.paralegal = paralegal;
		this.caseStatus = CaseStatus.OPEN;
		this.openedAt = openedAt;
	}
	
	public Case(Long caseId, String caseCode, Consultation consultation, Client client, Associate associate, Paralegal paralegal, CaseStatus caseStatus, LocalDateTime openedAt, CaseResolutionStatus caseResolutionStatus, LocalDateTime closedAt) {
		this.caseId = caseId;
		this.caseCode = caseCode;
		this.consultation = consultation;
		this.client = client;
		this.associate = associate;
		this.paralegal = paralegal;
		this.caseStatus = caseStatus;
		this.openedAt = openedAt;
		this.caseResolutionStatus = caseResolutionStatus;
		this.closedAt = closedAt;
	}
	
}
