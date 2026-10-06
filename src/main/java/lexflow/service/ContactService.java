package lexflow.service;

import lexflow.dao.ContactDao;
import lexflow.entity.case_.Case;
import lexflow.entity.case_.CaseStatus;
import lexflow.entity.case_.Contact;
import lexflow.exception.case_.InvalidCaseStateException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ContactService {
	
	private final ContactDao contactDao;
	
	// Create
	@Transactional
	public Contact add(Case case_, String name, List<String> phoneNumbers, List<String> emails, List<String> addresses, String notes) {
		
		if (case_.getCaseStatus() != CaseStatus.OPEN) {
			throw new InvalidCaseStateException(case_.getCaseId(), case_.getCaseStatus(), CaseStatus.OPEN);
		}
		
		Contact contact = new Contact(case_.getCaseId(), name, phoneNumbers, emails, addresses, notes, LocalDateTime.now());
		
		return contactDao.insert(contact);
	}
	
	// Read
	public Contact findById(Long contactId) {
		return contactDao.findById(contactId);
	}
	
	public List<Contact> findByCase(Case case_) {
		return contactDao.findByCaseId(case_.getCaseId());
	}
	
	// Update
	@Transactional
	public Contact update(Case case_, Contact contact, String name, List<String> phoneNumbers, List<String> emails, List<String> addresses, String notes) {
		
		if (case_.getCaseStatus() != CaseStatus.OPEN) {
			throw new InvalidCaseStateException(case_.getCaseId(), case_.getCaseStatus(), CaseStatus.OPEN);
		}
		
		contact.setName(name);
		contact.setPhoneNumbers(phoneNumbers);
		contact.setEmails(emails);
		contact.setAddresses(addresses);
		contact.setNotes(notes);
		
		return contactDao.update(contact);
	}
	
}
