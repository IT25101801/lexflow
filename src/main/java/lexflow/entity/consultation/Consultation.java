package lexflow.entity.consultation;

import lexflow.entity.user.Associate;
import lexflow.entity.user.Client;
import lexflow.entity.user.Paralegal;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class Consultation {
	
	private Long consultationId;
	private String consultationCode;
	private final Client client;
	private CaseType caseType;
	private List<String> opposingParties;
	private String description;
	private ConsultationStatus consultationStatus;
	private final LocalDateTime requestedAt;
	private Associate associate;
	private Paralegal paralegal;
	private String rejectionReason;
	
	public Consultation(Client client, CaseType caseType, List<String> opposingParties, String description, LocalDateTime requestedAt) {
		this.client = client;
		this.caseType = caseType;
		this.opposingParties = opposingParties;
		this.description = description;
		this.consultationStatus = ConsultationStatus.PENDING;
		this.requestedAt = requestedAt;
	}
	
	public Consultation(Long consultationId, String consultationCode, Client client, CaseType caseType, List<String> opposingParties, String description, ConsultationStatus consultationStatus, LocalDateTime requestedAt, Associate associate, Paralegal paralegal, String rejectionReason) {
		this.consultationId = consultationId;
		this.consultationCode = consultationCode;
		this.client = client;
		this.caseType = caseType;
		this.opposingParties = opposingParties;
		this.description = description;
		this.consultationStatus = consultationStatus;
		this.requestedAt = requestedAt;
		this.associate = associate;
		this.paralegal = paralegal;
		this.rejectionReason = rejectionReason;
	}
	
}
