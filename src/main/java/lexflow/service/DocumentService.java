package lexflow.service;

import lexflow.dao.CaseDao;
import lexflow.dao.ConsultationDao;
import lexflow.dao.DocumentDao;
import lexflow.dao.InvoiceDao;
import lexflow.entity.case_.Case;
import lexflow.entity.case_.Task;
import lexflow.entity.consultation.Consultation;
import lexflow.entity.document.*;
import lexflow.entity.finance.Invoice;
import lexflow.entity.user.*;
import lexflow.exception.PersistenceException;
import lexflow.exception.document.MissingRequiredDocumentException;
import lexflow.exception.document.NonPdfDocumentException;
import lexflow.strategy.*;
import lexflow.util.FileUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DocumentService {
	
	private final DocumentDao documentDao;
	private final CaseService caseService;
	private final ConsultationDao consultationDao;
	private final CaseDao caseDao;
	private final InvoiceDao invoiceDao;
	
	// Create
	
	public void attachNic(User user, MultipartFile nicFile, LocalDateTime uploadedAt) {
		
		if (nicFile == null || nicFile.isEmpty()) {
			throw new MissingRequiredDocumentException("NIC");
		}
		
		requirePdf(nicFile, "NIC");
		
		String filePath = FileUtil.store(nicFile, user.getUserId(), DocumentType.NIC_DOCUMENT);
		NicDocument nicDocument = new NicDocument(filePath, nicFile.getOriginalFilename(), uploadedAt, user.getUserId());
		
		documentDao.insert(nicDocument);
		user.setNicDocument(nicDocument);
	}
	
	public void attachBar(User user, MultipartFile barFile, LocalDateTime uploadedAt) {
		
		if (barFile == null || barFile.isEmpty()) {
			throw new MissingRequiredDocumentException("Bar");
		}
		
		requirePdf(barFile, "Bar");
		
		String filePath = FileUtil.store(barFile, user.getUserId(), DocumentType.BAR_DOCUMENT);
		BarDocument barDocument = new BarDocument(filePath, barFile.getOriginalFilename(), uploadedAt, user.getUserId());
		
		documentDao.insert(barDocument);
		setBarDocument(user, barDocument);
	}
	
	@Transactional
	public List<ConsultationDocument> attach(Consultation consultation, List<MultipartFile> consultationFiles, LocalDateTime uploadedAt) {
		List<ConsultationDocument> consultationDocuments = new ArrayList<>();
		
		for (MultipartFile consultationFile : consultationFiles) {
			ConsultationDocument consultationDocument = storeConsultationDocument(consultation, consultationFile, uploadedAt);
			
			if (consultationDocument != null) {
				consultationDocuments.add(consultationDocument);
			}
			
		}
		
		return consultationDocuments;
	}
	
	@Transactional
	public List<CaseDocument> attach(Case case_, Long taskId, List<MultipartFile> caseFiles, LocalDateTime uploadedAt) {
		caseService.requireOpen(case_);
		
		List<CaseDocument> caseDocuments = new ArrayList<>();
		
		for (MultipartFile caseFile : caseFiles) {
			CaseDocument caseDocument = storeCaseDocument(case_, caseFile, false, taskId, uploadedAt);
			
			if (caseDocument != null) {
				caseDocuments.add(caseDocument);
			}
			
		}
		
		return caseDocuments;
	}
	
	public InvoiceDocument attach(Case case_, Invoice invoice, byte[] pdfBytes, LocalDateTime uploadedAt) {
		String originalFilename = invoice.getInvoiceCode() + ".pdf";
		String filePath = FileUtil.store(pdfBytes, case_.getCaseId(), DocumentType.INVOICE_DOCUMENT, originalFilename);
		InvoiceDocument invoiceDocument = new InvoiceDocument(filePath, originalFilename, uploadedAt, invoice.getInvoiceId());
		
		return (InvoiceDocument) documentDao.insert(invoiceDocument);
	}
	
	@Transactional
	public List<CaseDocument> stage(Case case_, List<MultipartFile> caseFiles, LocalDateTime uploadedAt) {
		caseService.requireOpen(case_);
		
		List<CaseDocument> caseDocuments = new ArrayList<>();
		
		for (MultipartFile caseFile : caseFiles) {
			CaseDocument caseDocument = storeCaseDocument(case_, caseFile, true, null, uploadedAt);
			
			if (caseDocument != null) {
				caseDocuments.add(caseDocument);
			}
			
		}
		
		if (caseDocuments.isEmpty()) {
			throw new MissingRequiredDocumentException("Case");
		}
		
		return caseDocuments;
	}
	
	// Read
	
	public Document findById(Long documentId) {
		return documentDao.findById(documentId);
	}
	
	public List<ConsultationDocument> findByConsultation(Consultation consultation) {
		return documentDao.findByConsultationId(consultation.getConsultationId());
	}
	
	public List<CaseDocument> findStagedByCase(Case case_) {
		return documentDao.findByCaseIdAndInStaging(case_.getCaseId(), true);
	}
	
	public List<CaseDocument> findByCase(Case case_) {
		return documentDao.findByCaseIdAndInStaging(case_.getCaseId(), false);
	}
	
	public List<CaseDocument> findByTask(Task task) {
		return documentDao.findByTaskId(task.getTaskId());
	}
	
	public Map<Long, List<CaseDocument>> findByTasks(List<Task> tasks) {
		Map<Long, List<CaseDocument>> taskDocuments = new HashMap<>();
		
		for (Task task : tasks) {
			
			if (task.isDocumentingTask()) {
				taskDocuments.put(task.getTaskId(), findByTask(task));
			}
			
		}
		
		return taskDocuments;
	}
	
	public List<CaseDocument> findByCaseAndKeyword(Case case_, String keyword) {
		
		if (keyword == null || keyword.isBlank()) {
			return findByCase(case_);
		}
		
		return documentDao.findByCaseIdAndFilenameKeyword(case_.getCaseId(), keyword);
	}
	
	public void requirePdf(MultipartFile file, String documentLabel) {
		
		if (file == null || file.isEmpty()) {
			return;
		}
		
		String filename = file.getOriginalFilename();
		
		if (filename == null || !filename.toLowerCase().endsWith(".pdf")) {
			throw new NonPdfDocumentException(documentLabel, filename);
		}
		
	}
	
	public ResponseEntity<Resource> streamUserDocument(Document document, User requester) {
		
		if (!(document instanceof UserDocument userDocument)) {
			throw new AccessDeniedException("Not a user document.");
		}
		
		if (!canAccessUserDocument(userDocument, requester)) {
			throw new AccessDeniedException("Not authorized to view this document.");
		}
		
		return buildResponse(document);
	}
	
	public ResponseEntity<Resource> streamConsultationDocument(Document document, User requester) {
		
		if (!(document instanceof ConsultationDocument consultationDocument)) {
			throw new AccessDeniedException("Not a consultation document.");
		}
		
		Consultation consultation = consultationDao.findById(consultationDocument.getConsultationId());
		
		if (!canAccessConsultationDocument(consultation, requester)) {
			throw new AccessDeniedException("Not authorized to view this document.");
		}
		
		return buildResponse(document);
	}
	
	public ResponseEntity<Resource> streamCaseDocument(Document document, User requester) {
		
		if (!(document instanceof CaseDocument caseDocument)) {
			throw new AccessDeniedException("Not a case document.");
		}
		
		Case case_ = caseDao.findById(caseDocument.getCaseId());
		
		if (!canAccessCaseDocument(case_, requester)) {
			throw new AccessDeniedException("Not authorized to view this document.");
		}
		
		return buildResponse(document);
	}
	
	public ResponseEntity<Resource> streamInvoiceDocument(Document document, User requester) {
		
		if (!(document instanceof InvoiceDocument invoiceDocument)) {
			throw new AccessDeniedException("Not an invoice document.");
		}
		
		Invoice invoice = invoiceDao.findById(invoiceDocument.getInvoiceId());
		Case case_ = caseDao.findById(invoice.getCaseId());
		
		if (!canAccessInvoiceDocument(case_, requester)) {
			throw new AccessDeniedException("Not authorized to view this document.");
		}
		
		return buildResponse(document);
	}
	
	// Update
	
	public void replaceNic(User user, MultipartFile nicFile) {
		
		if (nicFile == null || nicFile.isEmpty()) {
			return;
		}
		
		requirePdf(nicFile, "NIC");
		
		String filePath = FileUtil.store(nicFile, user.getUserId(), DocumentType.NIC_DOCUMENT);
		NicDocument currentNicDocument = user.getNicDocument();
		LocalDateTime uploadedAt = LocalDateTime.now();
		
		if (currentNicDocument != null) {
			String oldFilePath = currentNicDocument.getFilePath();
			documentDao.replace(currentNicDocument.getDocumentId(), filePath, nicFile.getOriginalFilename(), uploadedAt);
			
			currentNicDocument.setFilePath(filePath);
			currentNicDocument.setOriginalFilename(nicFile.getOriginalFilename());
			currentNicDocument.setUploadedAt(uploadedAt);
			
			FileUtil.delete(oldFilePath);
		} else {
			NicDocument newNicDocument = new NicDocument(filePath, nicFile.getOriginalFilename(), uploadedAt, user.getUserId());
			user.setNicDocument((NicDocument) documentDao.insert(newNicDocument));
		}
		
	}
	
	public void replaceBar(User user, MultipartFile barFile) {
		
		if (barFile == null || barFile.isEmpty()) {
			return;
		}
		
		requirePdf(barFile, "Bar");
		
		String filePath = FileUtil.store(barFile, user.getUserId(), DocumentType.BAR_DOCUMENT);
		BarDocument currentBarDocument = getBarDocument(user);
		LocalDateTime uploadedAt = LocalDateTime.now();
		
		if (currentBarDocument != null) {
			String oldFilePath = currentBarDocument.getFilePath();
			documentDao.replace(currentBarDocument.getDocumentId(), filePath, barFile.getOriginalFilename(), uploadedAt);
			
			currentBarDocument.setFilePath(filePath);
			currentBarDocument.setOriginalFilename(barFile.getOriginalFilename());
			currentBarDocument.setUploadedAt(uploadedAt);
			
			FileUtil.delete(oldFilePath);
		} else {
			BarDocument newBarDocument = new BarDocument(filePath, barFile.getOriginalFilename(), uploadedAt, user.getUserId());
			setBarDocument(user, (BarDocument) documentDao.insert(newBarDocument));
		}
		
	}
	
	public void replaceInvoice(Invoice invoice, Case case_, byte[] pdfBytes) {
		InvoiceDocument currentInvoiceDocument = invoice.getInvoiceDocument();
		String filePath = FileUtil.store(pdfBytes, case_.getCaseId(), DocumentType.INVOICE_DOCUMENT, currentInvoiceDocument.getOriginalFilename());
		String oldFilePath = currentInvoiceDocument.getFilePath();
		LocalDateTime uploadedAt = LocalDateTime.now();
		documentDao.replace(currentInvoiceDocument.getDocumentId(), filePath, currentInvoiceDocument.getOriginalFilename(), uploadedAt);
		
		currentInvoiceDocument.setFilePath(filePath);
		currentInvoiceDocument.setUploadedAt(uploadedAt);
		
		FileUtil.delete(oldFilePath);
	}
	
	public void rename(Consultation consultation, Long documentId, String newFilename) {
		ConsultationDocument consultationDocument = documentDao.findConsultationDocumentById(documentId);
		
		if (!consultationDocument.getConsultationId().equals(consultation.getConsultationId())) {
			throw new AccessDeniedException("Document does not belong to this consultation.");
		}
		
		documentDao.updateFilename(documentId, FileUtil.rename(consultationDocument.getFilePath(), newFilename), newFilename);
	}
	
	public void rename(Case case_, Long documentId, String newFilename) {
		CaseDocument caseDocument = documentDao.findCaseDocumentById(documentId);
		
		if (!caseDocument.getCaseId().equals(case_.getCaseId())) {
			throw new AccessDeniedException("Document does not belong to this case.");
		}
		
		String originalFilename = caseDocument.getOriginalFilename();
		int dot = originalFilename.lastIndexOf('.');
		String extension = dot > 0 ? originalFilename.substring(dot) : "";
		String renamedFilename = newFilename.trim();
		
		if (!renamedFilename.toLowerCase().endsWith(extension.toLowerCase())) {
			renamedFilename = renamedFilename + extension;
		}
		
		documentDao.updateFilename(documentId, FileUtil.rename(caseDocument.getFilePath(), renamedFilename), renamedFilename);
	}
	
	@Transactional
	public void addStagedToCase(Case case_, List<Long> documentIds) {
		
		if (documentIds == null) {
			return;
		}
		
		for (Long documentId : documentIds) {
			CaseDocument caseDocument = documentDao.findCaseDocumentById(documentId);
			
			if (!caseDocument.getCaseId().equals(case_.getCaseId())) {
				throw new AccessDeniedException("Document does not belong to this case.");
			}
			
			documentDao.updateInStaging(documentId, false);
		}
		
	}
	
	public void unlinkFromTask(Task task) {
		documentDao.unlinkFromTask(task.getTaskId());
	}
	
	// Delete
	
	public void delete(Consultation consultation, List<Long> documentIds) {
		
		if (documentIds == null) {
			return;
		}
		
		List<String> filePaths = new ArrayList<>();
		
		for (Long documentId : documentIds) {
			ConsultationDocument consultationDocument = documentDao.findConsultationDocumentById(documentId);
			
			if (!consultationDocument.getConsultationId().equals(consultation.getConsultationId())) {
				throw new AccessDeniedException("Document does not belong to this consultation.");
			}
			
			filePaths.add(consultationDocument.getFilePath());
		}
		
		documentDao.deleteAllById(documentIds);
		
		for (String filePath : filePaths) {
			FileUtil.delete(filePath);
		}
		
	}
	
	public void deleteAll(Consultation consultation) {
		List<ConsultationDocument> consultationDocuments = findByConsultation(consultation);
		List<Long> documentIds = new ArrayList<>();
		
		for (ConsultationDocument consultationDocument : consultationDocuments) {
			documentIds.add(consultationDocument.getDocumentId());
		}
		
		delete(consultation, documentIds);
	}
	
	public void deleteStaged(Case case_, List<Long> documentIds) {
		
		if (documentIds == null) {
			return;
		}
		
		List<String> filePaths = new ArrayList<>();
		
		for (Long documentId : documentIds) {
			CaseDocument caseDocument = documentDao.findCaseDocumentById(documentId);
			
			if (!caseDocument.getCaseId().equals(case_.getCaseId())) {
				throw new AccessDeniedException("Document does not belong to this case.");
			}
			
			filePaths.add(caseDocument.getFilePath());
		}
		
		documentDao.deleteAllById(documentIds);
		
		for (String filePath : filePaths) {
			FileUtil.delete(filePath);
		}
		
	}
	
	// Strategy Pattern
	
	private boolean canAccessUserDocument(UserDocument userDocument, User requester) {
		return requester instanceof Partner || requester instanceof ClientRelationsOfficer || userDocument.getUserId().equals(requester.getUserId());
	}
	
	private boolean canAccessConsultationDocument(Consultation consultation, User requester) {
		return resolveDocumentAccessStrategy(requester, true, false).canAccess(requester, consultation.getClient(), consultation.getAssociate(), consultation.getParalegal());
	}
	
	private boolean canAccessCaseDocument(Case case_, User requester) {
		return resolveDocumentAccessStrategy(requester, false, false).canAccess(requester, case_.getClient(), case_.getAssociate(), case_.getParalegal());
	}
	
	private boolean canAccessInvoiceDocument(Case case_, User requester) {
		return resolveDocumentAccessStrategy(requester, false, true).canAccess(requester, case_.getClient(), case_.getAssociate(), case_.getParalegal());
	}
	
	private DocumentAccessStrategy resolveDocumentAccessStrategy(User requester, boolean allowClientRelationsOfficer, boolean allowFinanceOfficer) {
		
		if (requester instanceof Partner) {
			return new AlwaysAllowAccessStrategy();
		}
		
		if (allowClientRelationsOfficer && requester instanceof ClientRelationsOfficer) {
			return new AlwaysAllowAccessStrategy();
		}
		
		if (allowFinanceOfficer && requester instanceof FinanceOfficer) {
			return new AlwaysAllowAccessStrategy();
		}
		
		if (requester instanceof Associate) {
			return new AssociateOwnershipAccessStrategy();
		}
		
		if (requester instanceof Paralegal) {
			return new ParalegalOwnershipAccessStrategy();
		}
		
		if (requester instanceof Client) {
			return new ClientOwnershipAccessStrategy();
		}
		
		return new DenyAccessStrategy();
	}
	
	// Private Helpers
	
	private void setBarDocument(User user, BarDocument barDocument) {
		
		if (user instanceof Partner partner) {
			partner.setBarDocument(barDocument);
		} else if (user instanceof Associate associate) {
			associate.setBarDocument(barDocument);
		}
		
	}
	
	private ConsultationDocument storeConsultationDocument(Consultation consultation, MultipartFile consultationFile, LocalDateTime uploadedAt) {
		String filePath = FileUtil.store(consultationFile, consultation.getConsultationId(), DocumentType.CONSULTATION_DOCUMENT);
		
		if (filePath == null) {
			return null;
		}
		
		ConsultationDocument consultationDocument = new ConsultationDocument(filePath, consultationFile.getOriginalFilename(), uploadedAt, consultation.getConsultationId());
		
		return (ConsultationDocument) documentDao.insert(consultationDocument);
	}
	
	private CaseDocument storeCaseDocument(Case case_, MultipartFile caseFile, boolean inStaging, Long taskId, LocalDateTime uploadedAt) {
		String filePath = FileUtil.store(caseFile, case_.getCaseId(), DocumentType.CASE_DOCUMENT);
		
		if (filePath == null) {
			return null;
		}
		
		CaseDocument caseDocument = new CaseDocument(filePath, caseFile.getOriginalFilename(), uploadedAt, case_.getCaseId(), inStaging, taskId);
		
		return (CaseDocument) documentDao.insert(caseDocument);
	}
	
	private ResponseEntity<Resource> buildResponse(Document document) {
		Resource resource;
		
		try {
			resource = FileUtil.loadAsResource(document.getFilePath());
		} catch (PersistenceException e) {
			return ResponseEntity.notFound().build();
		}
		
		String contentType;
		
		try {
			contentType = Files.probeContentType(Paths.get(document.getFilePath()));
		} catch (IOException e) {
			throw new PersistenceException("reading file type", e);
		}
		
		if (contentType == null) {
			contentType = "application/octet-stream";
		}
		
		return ResponseEntity.ok().contentType(MediaType.parseMediaType(contentType)).header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline().filename(document.getStoredFilename()).build().toString()).body(resource);
	}
	
	private BarDocument getBarDocument(User user) {
		
		if (user instanceof Partner partner) {
			return partner.getBarDocument();
		}
		
		if (user instanceof Associate associate) {
			return associate.getBarDocument();
		}
		
		return null;
	}
	
}
