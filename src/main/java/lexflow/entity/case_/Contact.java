package lexflow.entity.case_;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class Contact {
	
	private Long contactId;
	private Long caseId;
	private String name;
	private List<String> phoneNumbers;
	private List<String> emails;
	private List<String> addresses;
	private String notes;
	private LocalDateTime addedAt;
	
	public Contact(Long caseId, String name, List<String> phoneNumbers, List<String> emails, List<String> addresses, String notes, LocalDateTime addedAt) {
		this.caseId = caseId;
		this.name = name;
		this.phoneNumbers = phoneNumbers;
		this.emails = emails;
		this.addresses = addresses;
		this.notes = notes;
		this.addedAt = addedAt;
	}
	
	public Contact(Long contactId, Long caseId, String name, List<String> phoneNumbers, List<String> emails, List<String> addresses, String notes, LocalDateTime addedAt) {
		this.contactId = contactId;
		this.caseId = caseId;
		this.name = name;
		this.phoneNumbers = phoneNumbers;
		this.emails = emails;
		this.addresses = addresses;
		this.notes = notes;
		this.addedAt = addedAt;
	}
	
}
