package lexflow.service;

import lexflow.dao.ContactDao;
import lexflow.entity.case_.Case;
import lexflow.entity.case_.Contact;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ContactService {
	
	private final ContactDao contactDao;
	private final CaseService caseService;
	
	// Create
	
	@Transactional
	public Contact save(Case case_, String name, List<String> phoneNumbers, List<String> emails, List<String> addresses, String notes) {
		caseService.requireOpen(case_);
		
		Contact contact = new Contact(case_.getCaseId(), name, phoneNumbers, emails, addresses, notes, LocalDateTime.now());
		
		return contactDao.insert(contact);
	}
	
	// Read
	
	public Contact findById(Long contactId) {
		return contactDao.findById(contactId);
	}
	
	public Contact findOwnedByCase(Long contactId, Case case_) {
		Contact contact = findById(contactId);
		
		if (!contact.getCaseId().equals(case_.getCaseId())) {
			throw new AccessDeniedException("Not a contact for this case.");
		}
		
		return contact;
	}
	
	public List<Contact> findByCase(Case case_) {
		return contactDao.findByCaseId(case_.getCaseId());
	}
	
	// Update
	
	@Transactional
	public Contact update(Case case_, Contact contact, String name, List<String> phoneNumbers, List<String> emails, List<String> addresses, String notes) {
		caseService.requireOpen(case_);
		
		contact.setName(name);
		contact.setPhoneNumbers(phoneNumbers);
		contact.setEmails(emails);
		contact.setAddresses(addresses);
		contact.setNotes(notes);
		
		return contactDao.update(contact);
	}
	
	// Delete
	
	public void delete(Case case_, Contact contact) {
		caseService.requireOpen(case_);
		contactDao.deleteById(contact.getContactId());
	}
	
}
