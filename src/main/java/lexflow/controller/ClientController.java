package lexflow.controller;

import jakarta.validation.Valid;
import lexflow.dto.consultation.EditConsultationRequest;
import lexflow.dto.consultation.SubmitOnlineConsultationRequest;
import lexflow.dto.document.UploadCaseDocumentsRequest;
import lexflow.dto.finance.TopUpByCardRequest;
import lexflow.dto.user.ChangePasswordRequest;
import lexflow.entity.case_.Case;
import lexflow.entity.consultation.CaseType;
import lexflow.entity.consultation.Consultation;
import lexflow.entity.user.Client;
import lexflow.entity.user.User;
import lexflow.exception.BusinessRuleViolationException;
import lexflow.exception.PersistenceException;
import lexflow.exception.case_.InvalidCaseStateException;
import lexflow.exception.consultation.InvalidConsultationStateException;
import lexflow.security.CustomUserPrincipal;
import lexflow.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class ClientController {
	
	private final ConsultationService consultationService;
	private final UserService userService;
	private final DocumentService documentService;
	private final CaseService caseService;
	private final EventService eventService;
	private final TransactionService transactionService;
	private final TaskService taskService;
	
	// Overview
	
	@GetMapping("/client/dashboard")
	public String dashboard(Authentication authentication, Model model) {
		model.addAttribute("user", currentClient(authentication));
		
		return "client/dashboard";
	}
	
	@GetMapping("/client/profile")
	public String profile(Authentication authentication, Model model) {
		model.addAttribute("user", currentClient(authentication));
		
		return "client/profile";
	}
	
	@PostMapping("/client/profile/change-password")
	public String changePassword(Authentication authentication, @Valid @ModelAttribute ChangePasswordRequest form, BindingResult bindingResult, Model model) {
		User user = currentClient(authentication);
		
		if (bindingResult.hasErrors()) {
			model.addAttribute("error", bindingResult.getAllErrors().getFirst().getDefaultMessage());
			model.addAttribute("user", user);
			
			return "client/profile";
		}
		
		if (!userService.verifyPassword(user, form.getCurrentPassword())) {
			model.addAttribute("user", user);
			model.addAttribute("error", "Current password is incorrect.");
			
			return "client/profile";
		}
		
		userService.changePassword(user, form.getNewPassword());
		
		model.addAttribute("user", user);
		model.addAttribute("success", "Password changed successfully.");
		
		return "client/profile";
	}
	
	// Consultations
	
	@GetMapping("/client/consultations/new")
	public String showConsultationForm(Model model) {
		model.addAttribute("caseTypes", CaseType.values());
		
		return "client/consultation-form";
	}
	
	@PostMapping("/client/consultations")
	public String submitConsultation(@Valid @ModelAttribute SubmitOnlineConsultationRequest form, BindingResult bindingResult, Authentication authentication, Model model, RedirectAttributes redirectAttributes) {
		
		if (bindingResult.hasErrors()) {
			model.addAttribute("error", bindingResult.getAllErrors().getFirst().getDefaultMessage());
			model.addAttribute("caseTypes", CaseType.values());
			
			return "client/consultation-form";
		}
		
		List<MultipartFile> consultationFiles = form.getConsultationFiles() != null ? form.getConsultationFiles() : List.of();
		
		try {
			consultationService.submit(currentClient(authentication), form.getCaseType(), form.getOpposingParties(), form.getDescription(), consultationFiles);
		} catch (BusinessRuleViolationException e) {
			model.addAttribute("caseTypes", CaseType.values());
			model.addAttribute("error", e.getMessage());
			
			return "client/consultation-form";
		} catch (PersistenceException e) {
			model.addAttribute("caseTypes", CaseType.values());
			model.addAttribute("error", "Could not submit your consultation request. Please try again.");
			
			return "client/consultation-form";
		}
		
		redirectAttributes.addFlashAttribute("success", "Consultation request submitted successfully.");
		
		return "redirect:/client/consultations";
	}
	
	@GetMapping("/client/consultations")
	public String consultations(Authentication authentication, Model model) {
		model.addAttribute("consultations", consultationService.findAllForClient(currentClient(authentication)));
		
		return "client/consultation-list";
	}
	
	@GetMapping("/client/consultations/{id}")
	public String consultationDetails(@PathVariable("id") Long consultationId, Authentication authentication, Model model) {
		populateConsultationDetailModel(consultationService.findOwnedByClient(consultationId, currentClient(authentication)), model);
		
		return "client/consultation-details";
	}
	
	@PostMapping("/client/consultations/{id}/delete")
	public String deleteConsultation(@PathVariable("id") Long consultationId, Authentication authentication, RedirectAttributes redirectAttributes) {
		Consultation consultation = consultationService.findOwnedByClient(consultationId, currentClient(authentication));
		
		try {
			consultationService.delete(consultation);
		} catch (InvalidConsultationStateException e) {
			redirectAttributes.addFlashAttribute("error", e.getMessage());
			
			return "redirect:/client/consultations/" + consultationId;
		}
		
		redirectAttributes.addFlashAttribute("success", "Consultation request deleted.");
		
		return "redirect:/client/consultations";
	}
	
	@PostMapping("/client/consultations/{id}/edit")
	public String editConsultation(@PathVariable("id") Long consultationId, @Valid @ModelAttribute EditConsultationRequest form, BindingResult bindingResult, Authentication authentication, Model model) {
		Consultation consultation = consultationService.findOwnedByClient(consultationId, currentClient(authentication));
		
		if (bindingResult.hasErrors()) {
			model.addAttribute("error", bindingResult.getAllErrors().getFirst().getDefaultMessage());
			populateConsultationDetailModel(consultation, model);
			
			return "client/consultation-details";
		}
		
		List<MultipartFile> consultationFiles = form.getConsultationFiles() != null ? form.getConsultationFiles() : List.of();
		
		try {
			consultation = consultationService.updateDetails(consultation, form.getCaseType(), form.getOpposingParties(), form.getDescription(), consultationFiles, form.getDeleteDocumentIds(), form.getDocumentIds(), form.getOriginalFilenames());
			populateConsultationDetailModel(consultation, model);
			model.addAttribute("success", "Consultation request updated successfully.");
		} catch (InvalidConsultationStateException | BusinessRuleViolationException e) {
			populateConsultationDetailModel(consultation, model);
			model.addAttribute("error", e.getMessage());
		}
		
		return "client/consultation-details";
	}
	
	// Cases
	
	@GetMapping("/client/cases/open")
	public String openCases(Authentication authentication, Model model) {
		model.addAttribute("cases", caseService.findOpenForClient(currentClient(authentication)));
		model.addAttribute("title", "Open Cases");
		model.addAttribute("active", "cases-open");
		
		return "client/case-list";
	}
	
	@GetMapping("/client/cases/closed")
	public String closedCases(Authentication authentication, Model model) {
		model.addAttribute("cases", caseService.findClosedForClient(currentClient(authentication)));
		model.addAttribute("title", "Closed Cases");
		model.addAttribute("active", "cases-closed");
		
		return "client/case-list";
	}
	
	@GetMapping("/client/cases/{id}")
	public String caseDetails(@PathVariable("id") Long caseId, @RequestParam(required = false) String documentKeyword, Authentication authentication, Model model) {
		Case case_ = caseService.findOwnedByClient(caseId, currentClient(authentication));
		populateCaseDetailModel(case_, model);
		
		model.addAttribute("caseDocuments", documentService.findByCaseAndKeyword(case_, documentKeyword));
		model.addAttribute("documentKeyword", documentKeyword);
		
		return "client/case-details";
	}
	
	@PostMapping("/client/cases/{id}/documents/stage")
	public String stageCaseDocument(@PathVariable("id") Long caseId, @Valid @ModelAttribute UploadCaseDocumentsRequest form, Authentication authentication, Model model) {
		Case case_ = caseService.findOwnedByClient(caseId, currentClient(authentication));
		List<MultipartFile> caseFiles = form.getCaseFiles() != null ? form.getCaseFiles() : List.of();
		
		try {
			documentService.stage(case_, caseFiles, LocalDateTime.now());
			populateCaseDetailModel(case_, model);
			model.addAttribute("success", "Document uploaded.");
		} catch (InvalidCaseStateException e) {
			populateCaseDetailModel(case_, model);
			model.addAttribute("error", e.getMessage());
		}
		
		return "client/case-details";
	}
	
	// Schedule
	
	@GetMapping("/client/events/upcoming")
	public String upcomingEvents(Authentication authentication, Model model) {
		model.addAttribute("events", eventService.upcoming(eventService.findAllForClient(currentClient(authentication))));
		model.addAttribute("title", "Upcoming Events");
		model.addAttribute("active", "events-upcoming");

		return "client/event-list";
	}

	@GetMapping("/client/events/past")
	public String pastEvents(Authentication authentication, Model model) {
		model.addAttribute("events", eventService.past(eventService.findAllForClient(currentClient(authentication))));
		model.addAttribute("title", "Past Events");
		model.addAttribute("active", "events-past");

		return "client/event-list";
	}
	
	@GetMapping("/client/events/{id}")
	public String eventDetails(@PathVariable("id") Long eventId, Authentication authentication, Model model) {
		model.addAttribute("event", eventService.findOwnedByClient(eventId, currentClient(authentication)));
		
		return "client/event-details";
	}
	
	// Payments
	
	@GetMapping("/client/transactions")
	public String transactions(Authentication authentication, Model model) {
		Client client = currentClient(authentication);
		model.addAttribute("client", client);
		model.addAttribute("transactions", transactionService.findAllForClient(client));
		
		return "client/transactions";
	}
	
	@PostMapping("/client/transactions/top-up")
	public String topUpWalletByCard(@Valid @ModelAttribute TopUpByCardRequest form, BindingResult bindingResult, Authentication authentication, Model model) {
		
		if (bindingResult.hasErrors()) {
			model.addAttribute("error", bindingResult.getAllErrors().getFirst().getDefaultMessage());
			Client client = currentClient(authentication);
			model.addAttribute("client", client);
			model.addAttribute("transactions", transactionService.findAllForClient(client));
			
			return "client/transactions";
		}
		
		return "redirect:/payment/portal?amount=" + form.getAmount();
	}
	
	// Documents
	
	@GetMapping("/client/documents/user/{id}")
	public ResponseEntity<Resource> viewUserDocument(@PathVariable("id") Long documentId, Authentication authentication) {
		return documentService.streamUserDocument(documentService.findById(documentId), currentClient(authentication));
	}
	
	@GetMapping("/client/documents/consultation/{id}")
	public ResponseEntity<Resource> viewConsultationDocument(@PathVariable("id") Long documentId, Authentication authentication) {
		return documentService.streamConsultationDocument(documentService.findById(documentId), currentClient(authentication));
	}
	
	@GetMapping("/client/documents/case/{id}")
	public ResponseEntity<Resource> viewCaseDocument(@PathVariable("id") Long documentId, Authentication authentication) {
		return documentService.streamCaseDocument(documentService.findById(documentId), currentClient(authentication));
	}
	
	// Private Helpers
	
	private Client currentClient(Authentication authentication) {
		CustomUserPrincipal principal = (CustomUserPrincipal) authentication.getPrincipal();
		
		return (Client) principal.user();
	}
	
	private void populateConsultationDetailModel(Consultation consultation, Model model) {
		model.addAttribute("consultation", consultation);
		model.addAttribute("consultationDocuments", documentService.findByConsultation(consultation));
		model.addAttribute("caseTypes", CaseType.values());
		model.addAttribute("existingCase", caseService.findByConsultation(consultation));
	}
	
	private void populateCaseDetailModel(Case case_, Model model) {
		model.addAttribute("case", case_);
		model.addAttribute("stagedDocuments", documentService.findStagedByCase(case_));
		model.addAttribute("caseDocuments", documentService.findByCase(case_));
		model.addAttribute("tasks", taskService.findByCase(case_));
	}
	
}
