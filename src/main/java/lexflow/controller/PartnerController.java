package lexflow.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lexflow.dto.case_.AddTaskRequest;
import lexflow.dto.case_.CloseCaseRequest;
import lexflow.dto.case_.RenameTaskRequest;
import lexflow.dto.case_.SaveContactRequest;
import lexflow.dto.consultation.AssignStaffRequest;
import lexflow.dto.consultation.RejectConsultationRequest;
import lexflow.dto.document.RenameDocumentRequest;
import lexflow.dto.finance.CompileInvoiceRequest;
import lexflow.dto.finance.EditBillableAmountRequest;
import lexflow.dto.scheduling.ScheduleMeetingRequest;
import lexflow.dto.user.*;
import lexflow.entity.case_.Case;
import lexflow.entity.case_.CaseResolutionStatus;
import lexflow.entity.case_.Contact;
import lexflow.entity.case_.Task;
import lexflow.entity.consultation.CaseType;
import lexflow.entity.consultation.Consultation;
import lexflow.entity.finance.InvoiceStatus;
import lexflow.entity.scheduling.ConsultationEvent;
import lexflow.entity.user.Role;
import lexflow.entity.user.*;
import lexflow.exception.BusinessRuleViolationException;
import lexflow.exception.EntityAlreadyExistsException;
import lexflow.exception.InvalidStateException;
import lexflow.exception.PersistenceException;
import lexflow.exception.document.NonPdfDocumentException;
import lexflow.exception.finance.InsufficientFundsException;
import lexflow.exception.finance.InvalidInvoiceStateException;
import lexflow.security.CustomUserPrincipal;
import lexflow.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class PartnerController {
	
	private final AnalyticsService analyticsService;
	
	private final CaseService caseService;
	
	private final ConsultationService consultationService;
	
	private final ConflictCheckService conflictCheckService;
	
	private final ContactService contactService;
	
	private final DocumentService documentService;
	
	private final EventService eventService;
	
	private final InvoiceService invoiceService;
	
	private final TaskService taskService;
	
	private final TransactionService transactionService;
	
	private final UserService userService;
	
	// ==================== Overview (Dashboard, Profile) ====================
	
	@GetMapping("/partner/dashboard")
	public String dashboard(Authentication authentication, Model model) {
		model.addAttribute("user", currentPartner(authentication));
		return "partner/dashboard";
	}
	
	// ==================== Helpers (shared across sections) ====================
	
	private Partner currentPartner(Authentication authentication) {
		CustomUserPrincipal principal = (CustomUserPrincipal) authentication.getPrincipal();
		return (Partner) principal.user();
	}
	
	@GetMapping("/partner/profile")
	public String profile(Authentication authentication, Model model) {
		model.addAttribute("user", currentPartner(authentication));
		model.addAttribute("genders", Gender.values());
		return "partner/profile";
	}
	
	@PostMapping("/partner/profile/edit")
	public String editProfile(@Valid @ModelAttribute EditProfileRequest form, BindingResult bindingResult,
							  Authentication authentication, Model model) {
		Partner partner = currentPartner(authentication);
		
		if (bindingResult.hasErrors()) {
			model.addAttribute("error", bindingResult.getAllErrors().getFirst().getDefaultMessage());
			model.addAttribute("user", partner);
			model.addAttribute("genders", Gender.values());
			return "partner/profile";
		}
		
		try {
			User user = userService.updateDetails(partner, form.getName(), form.getDateOfBirth(), form.getGender(),
												  form.getAddresses(), form.getPhoneNumbers(), form.getNicFile(), form.getBarFile());
			model.addAttribute("user", user);
			model.addAttribute("success", "Profile updated successfully.");
		} catch (NonPdfDocumentException e) {
			model.addAttribute("user", partner);
			model.addAttribute("error", e.getMessage());
		}
		
		model.addAttribute("genders", Gender.values());
		return "partner/profile";
	}
	
	@PostMapping("/partner/profile/change-password")
	public String changePassword(@Valid @ModelAttribute ChangePasswordRequest form, BindingResult bindingResult,
								 Authentication authentication, Model model) {
		Partner partner = currentPartner(authentication);
		model.addAttribute("genders", Gender.values());
		
		if (bindingResult.hasErrors()) {
			model.addAttribute("error", bindingResult.getAllErrors().getFirst().getDefaultMessage());
			model.addAttribute("user", partner);
			return "partner/profile";
		}
		
		if (!userService.verifyPassword(partner, form.getCurrentPassword())) {
			model.addAttribute("user", partner);
			model.addAttribute("error", "Current password is incorrect.");
			return "partner/profile";
		}
		
		userService.changePassword(partner, form.getNewPassword());
		
		model.addAttribute("user", partner);
		model.addAttribute("success", "Password changed successfully.");
		return "partner/profile";
	}
	
	@GetMapping("/partner/documents/user/{id}")
	public ResponseEntity<Resource> viewUserDocument(@PathVariable("id") Long documentId, Authentication authentication) {
		return documentService.streamUserDocument(documentService.findById(documentId), currentPartner(authentication));
	}
	
	// ==================== Users (Register Staff, Role Lists, User Detail) ====================
	
	@GetMapping("/partner/register-staff")
	public String showRegisterStaffForm(Model model) {
		model.addAttribute("genders", Gender.values());
		model.addAttribute("roles", List.of(Role.PARTNER, Role.ASSOCIATE, Role.PARALEGAL, Role.CLIENT_RELATIONS_OFFICER, Role.FINANCE_OFFICER));
		return "partner/register-staff";
	}
	
	@PostMapping("/partner/register-staff")
	public String registerStaff(@Valid @ModelAttribute RegisterStaffRequest form, BindingResult bindingResult,
								Model model, RedirectAttributes redirectAttributes) {
		
		if (bindingResult.hasErrors()) {
			List<String> validationErrors = new ArrayList<>();
			
			for (ObjectError objectError : bindingResult.getAllErrors()) {
				validationErrors.add(objectError.getDefaultMessage());
			}
			
			model.addAttribute("validationErrors", validationErrors);
			model.addAttribute("genders", Gender.values());
			model.addAttribute("roles", List.of(Role.PARTNER, Role.ASSOCIATE, Role.PARALEGAL, Role.CLIENT_RELATIONS_OFFICER, Role.FINANCE_OFFICER));
			return "partner/register-staff";
		}
		
		try {
			userService.register(form.getRole(), form.getEmail(), form.getPassword(), form.getName(), form.getDateOfBirth(),
								 form.getGender(), form.getAddresses(), form.getPhoneNumbers(), form.getNicNumber(),
								 form.getBarNumber(), form.getNicFile(), form.getBarFile());
		} catch (EntityAlreadyExistsException | BusinessRuleViolationException e) {
			model.addAttribute("genders", Gender.values());
			model.addAttribute("roles", List.of(Role.PARTNER, Role.ASSOCIATE, Role.PARALEGAL, Role.CLIENT_RELATIONS_OFFICER, Role.FINANCE_OFFICER));
			model.addAttribute("error", e.getMessage());
			return "partner/register-staff";
		} catch (PersistenceException e) {
			model.addAttribute("genders", Gender.values());
			model.addAttribute("roles", List.of(Role.PARTNER, Role.ASSOCIATE, Role.PARALEGAL, Role.CLIENT_RELATIONS_OFFICER, Role.FINANCE_OFFICER));
			model.addAttribute("error", "Could not register the staff member. Please try again.");
			return "partner/register-staff";
		}
		
		String listUrl = switch (form.getRole()) {
			case PARTNER -> "/partner/users/partners";
			case ASSOCIATE -> "/partner/users/associates";
			case PARALEGAL -> "/partner/users/paralegals";
			case CLIENT_RELATIONS_OFFICER -> "/partner/users/client-relations-officers";
			case FINANCE_OFFICER -> "/partner/users/finance-officers";
			default -> "/partner/dashboard";
		};
		
		redirectAttributes.addFlashAttribute("success", "Staff member registered successfully.");
		return "redirect:" + listUrl;
	}
	
	@GetMapping("/partner/users/associates")
	public String associates(@RequestParam(required = false) String keyword, Model model) {
		model.addAttribute("users", userService.findByRoleAndKeyword(Role.ASSOCIATE, keyword));
		model.addAttribute("keyword", keyword);
		model.addAttribute("title", "Associates");
		model.addAttribute("active", "users-associates");
		model.addAttribute("listUrl", "/partner/users/associates");
		return "partner/user-list";
	}
	
	@GetMapping("/partner/users/paralegals")
	public String paralegals(@RequestParam(required = false) String keyword, Model model) {
		model.addAttribute("users", userService.findByRoleAndKeyword(Role.PARALEGAL, keyword));
		model.addAttribute("keyword", keyword);
		model.addAttribute("title", "Paralegals");
		model.addAttribute("active", "users-paralegals");
		model.addAttribute("listUrl", "/partner/users/paralegals");
		return "partner/user-list";
	}
	
	@GetMapping("/partner/users/client-relations-officers")
	public String clientRelationsOfficers(@RequestParam(required = false) String keyword, Model model) {
		model.addAttribute("users", userService.findByRoleAndKeyword(Role.CLIENT_RELATIONS_OFFICER, keyword));
		model.addAttribute("keyword", keyword);
		model.addAttribute("title", "Client Relations Officers");
		model.addAttribute("active", "users-client-relations-officers");
		model.addAttribute("listUrl", "/partner/users/client-relations-officers");
		return "partner/user-list";
	}
	
	@GetMapping("/partner/users/finance-officers")
	public String financeOfficers(@RequestParam(required = false) String keyword, Model model) {
		model.addAttribute("users", userService.findByRoleAndKeyword(Role.FINANCE_OFFICER, keyword));
		model.addAttribute("keyword", keyword);
		model.addAttribute("title", "Finance Officers");
		model.addAttribute("active", "users-finance-officers");
		model.addAttribute("listUrl", "/partner/users/finance-officers");
		return "partner/user-list";
	}
	
	@GetMapping("/partner/users/clients")
	public String clients(@RequestParam(required = false) String keyword, Model model) {
		model.addAttribute("users", userService.findByRoleAndKeyword(Role.CLIENT, keyword));
		model.addAttribute("keyword", keyword);
		model.addAttribute("title", "Clients");
		model.addAttribute("active", "users-clients");
		model.addAttribute("listUrl", "/partner/users/clients");
		return "partner/user-list";
	}
	
	@GetMapping("/partner/users/partners")
	public String partners(@RequestParam(required = false) String keyword, Authentication authentication, Model model) {
		model.addAttribute("users", userService.findByRoleAndKeyword(Role.PARTNER, keyword));
		model.addAttribute("keyword", keyword);
		model.addAttribute("title", "Partners");
		model.addAttribute("currentUserEmail", authentication.getName());
		return "partner/partner-list";
	}
	
	@GetMapping("/partner/conflict-check")
	public String conflictCheck(@RequestParam(required = false) String keyword, Model model) {
		
		if (keyword != null && !keyword.isBlank()) {
			model.addAttribute("results", conflictCheckService.check(keyword));
			model.addAttribute("keyword", keyword);
		}
		
		return "partner/conflict-check";
	}
	
	// ==================== Consultations (Pending, All, Conflict Check) ====================
	
	@GetMapping("/partner/consultations/pending")
	public String pendingConsultations(Model model) {
		model.addAttribute("consultations", consultationService.findAwaitingReviewForPartner());
		return "partner/consultation-pending-list";
	}
	
	@GetMapping("/partner/consultations")
	public String consultations(Model model) {
		model.addAttribute("consultations", consultationService.findAll());
		return "partner/consultation-list";
	}
	
	@GetMapping("/partner/consultations/{id}")
	public String consultationDetails(@PathVariable("id") Long consultationId, Model model) {
		populateConsultationDetailModel(consultationService.findById(consultationId), model);
		return "partner/consultation-details";
	}
	
	private void populateConsultationDetailModel(Consultation consultation, Model model) {
		model.addAttribute("consultation", consultation);
		model.addAttribute("consultationDocuments", documentService.findByConsultation(consultation));
		model.addAttribute("associates", userService.findActiveAssociates());
		model.addAttribute("paralegals", userService.findActiveParalegals());
		model.addAttribute("meetings", eventService.findByConsultation(consultation));
		
		Case existingCase = caseService.findByConsultation(consultation);
		model.addAttribute("existingCase", existingCase);
		model.addAttribute("hasCase", existingCase != null);
	}
	
	@PostMapping("/partner/consultations/{id}/accept")
	public String acceptConsultation(@PathVariable("id") Long consultationId,
									 @Valid @ModelAttribute AssignStaffRequest form, BindingResult bindingResult,
									 Model model) {
		Consultation consultation = consultationService.findById(consultationId);
		
		if (bindingResult.hasErrors()) {
			model.addAttribute("error", bindingResult.getAllErrors().getFirst().getDefaultMessage());
			populateConsultationDetailModel(consultation, model);
			return "partner/consultation-details";
		}
		
		Associate associate = (Associate) userService.findById(form.getAssociateId());
		Paralegal paralegal = (Paralegal) userService.findById(form.getParalegalId());
		
		try {
			consultation = consultationService.partnerAccept(consultation, associate, paralegal);
			populateConsultationDetailModel(consultation, model);
			model.addAttribute("success", "Consultation request accepted and staff assigned.");
		} catch (InvalidStateException e) {
			populateConsultationDetailModel(consultation, model);
			model.addAttribute("error", e.getMessage());
		}
		
		return "partner/consultation-details";
	}
	
	@PostMapping("/partner/consultations/{id}/reject")
	public String rejectConsultation(@PathVariable("id") Long consultationId,
									 @Valid @ModelAttribute RejectConsultationRequest form, BindingResult bindingResult,
									 Model model) {
		Consultation consultation = consultationService.findById(consultationId);
		
		if (bindingResult.hasErrors()) {
			model.addAttribute("error", bindingResult.getAllErrors().getFirst().getDefaultMessage());
			populateConsultationDetailModel(consultation, model);
			return "partner/consultation-details";
		}
		
		try {
			consultation = consultationService.partnerReject(consultation, form.getRejectionReason());
			populateConsultationDetailModel(consultation, model);
			model.addAttribute("success", "Consultation request rejected.");
		} catch (InvalidStateException e) {
			populateConsultationDetailModel(consultation, model);
			model.addAttribute("error", e.getMessage());
		}
		
		return "partner/consultation-details";
	}
	
	@PostMapping("/partner/consultations/{id}/reassign")
	public String reassignConsultation(@PathVariable("id") Long consultationId,
									   @Valid @ModelAttribute AssignStaffRequest form, BindingResult bindingResult,
									   Model model) {
		Consultation consultation = consultationService.findById(consultationId);
		
		if (bindingResult.hasErrors()) {
			model.addAttribute("error", bindingResult.getAllErrors().getFirst().getDefaultMessage());
			populateConsultationDetailModel(consultation, model);
			return "partner/consultation-details";
		}
		
		Associate associate = (Associate) userService.findById(form.getAssociateId());
		Paralegal paralegal = (Paralegal) userService.findById(form.getParalegalId());
		consultation = consultationService.reassignStaff(consultation, associate, paralegal);
		
		populateConsultationDetailModel(consultation, model);
		model.addAttribute("success", "Staff reassigned.");
		return "partner/consultation-details";
	}
	
	@PostMapping("/partner/consultations/{id}/events/{eventId}/reschedule")
	public String rescheduleMeeting(@PathVariable("id") Long consultationId, @PathVariable Long eventId,
									@Valid @ModelAttribute ScheduleMeetingRequest form, BindingResult bindingResult,
									Model model) {
		Consultation consultation = consultationService.findById(consultationId);
		
		if (bindingResult.hasErrors()) {
			model.addAttribute("error", bindingResult.getAllErrors().getFirst().getDefaultMessage());
			populateConsultationDetailModel(consultation, model);
			return "partner/consultation-details";
		}
		
		ConsultationEvent event = eventService.findOwnedByConsultation(eventId, consultation);
		
		try {
			eventService.reschedule(event, form.getSubject(), form.getDate(), form.getStartTime(), form.getEndTime(), form.getLocation(), form.getNotes());
			populateConsultationDetailModel(consultation, model);
			model.addAttribute("success", "Meeting rescheduled successfully.");
		} catch (InvalidStateException | BusinessRuleViolationException e) {
			populateConsultationDetailModel(consultation, model);
			model.addAttribute("error", e.getMessage());
		}
		
		return "partner/consultation-details";
	}
	
	@PostMapping("/partner/consultations/{id}/events/{eventId}/complete")
	public String completeMeeting(@PathVariable("id") Long consultationId, @PathVariable Long eventId, Model model) {
		Consultation consultation = consultationService.findById(consultationId);
		
		try {
			eventService.complete(eventService.findOwnedByConsultation(eventId, consultation));
			populateConsultationDetailModel(consultation, model);
			model.addAttribute("success", "Meeting marked as completed.");
		} catch (InvalidStateException | BusinessRuleViolationException e) {
			populateConsultationDetailModel(consultation, model);
			model.addAttribute("error", e.getMessage());
		}
		
		return "partner/consultation-details";
	}
	
	@PostMapping("/partner/consultations/{id}/events/{eventId}/cancel")
	public String cancelMeeting(@PathVariable("id") Long consultationId, @PathVariable Long eventId, Model model) {
		Consultation consultation = consultationService.findById(consultationId);
		
		try {
			eventService.cancel(eventService.findOwnedByConsultation(eventId, consultation));
			populateConsultationDetailModel(consultation, model);
			model.addAttribute("success", "Meeting canceled.");
		} catch (InvalidStateException e) {
			populateConsultationDetailModel(consultation, model);
			model.addAttribute("error", e.getMessage());
		}
		
		return "partner/consultation-details";
	}
	
	// ==================== Documents (file downloads, no nav item of their own) ====================
	
	@GetMapping("/partner/documents/consultation/{id}")
	public ResponseEntity<Resource> viewConsultationDocument(@PathVariable("id") Long documentId, Authentication authentication) {
		return documentService.streamConsultationDocument(documentService.findById(documentId), currentPartner(authentication));
	}
	
	// ==================== Cases (Open, Closed) ====================
	
	@GetMapping("/partner/cases/open")
	public String openCases(Model model) {
		model.addAttribute("cases", caseService.findAllOpen());
		model.addAttribute("title", "Open Cases");
		model.addAttribute("active", "cases-open");
		return "partner/case-list";
	}
	
	@GetMapping("/partner/cases/closed")
	public String closedCases(Model model) {
		model.addAttribute("cases", caseService.findAllClosed());
		model.addAttribute("title", "Closed Cases");
		model.addAttribute("active", "cases-closed");
		return "partner/case-list";
	}
	
	@GetMapping("/partner/cases/{id}")
	public String caseDetails(@PathVariable("id") Long caseId, @RequestParam(required = false) String documentKeyword, Model model) {
		Case case_ = caseService.findById(caseId);
		populateCaseDetailModel(case_, model);
		
		model.addAttribute("caseDocuments", documentService.findByCaseAndKeyword(case_, documentKeyword));
		model.addAttribute("documentKeyword", documentKeyword);
		
		return "partner/case-details";
	}
	
	private void populateCaseDetailModel(Case case_, Model model) {
		model.addAttribute("case", case_);
		model.addAttribute("associates", userService.findActiveAssociates());
		model.addAttribute("paralegals", userService.findActiveParalegals());
		model.addAttribute("stagedDocuments", documentService.findStagedByCase(case_));
		model.addAttribute("caseDocuments", documentService.findByCase(case_));
		model.addAttribute("contacts", contactService.findByCase(case_));
		
		List<Task> tasks = taskService.findByCase(case_);
		model.addAttribute("tasks", tasks);
		model.addAttribute("taskDocuments", documentService.findByTasks(tasks));
		model.addAttribute("unbilledCharges", taskService.findUnbilledByCase(case_));
		model.addAttribute("unpaidInvoices", invoiceService.findByCaseAndStatus(case_, InvoiceStatus.UNPAID));
		model.addAttribute("paidInvoices", invoiceService.findByCaseAndStatus(case_, InvoiceStatus.PAID));
		model.addAttribute("voidedInvoices", invoiceService.findByCaseAndStatus(case_, InvoiceStatus.VOIDED));
	}
	
	@PostMapping("/partner/cases/{id}/close")
	public String closeCase(@PathVariable("id") Long caseId,
							@Valid @ModelAttribute CloseCaseRequest form, BindingResult bindingResult,
							Model model) {
		Case case_ = caseService.findById(caseId);
		
		if (bindingResult.hasErrors()) {
			model.addAttribute("error", bindingResult.getAllErrors().getFirst().getDefaultMessage());
			populateCaseDetailModel(case_, model);
			return "partner/case-details";
		}
		
		try {
			case_ = caseService.close(case_, form.getCaseResolutionStatus());
			populateCaseDetailModel(case_, model);
			model.addAttribute("success", "Case closed successfully.");
		} catch (InvalidStateException e) {
			populateCaseDetailModel(case_, model);
			model.addAttribute("error", e.getMessage());
		}
		
		return "partner/case-details";
	}
	
	@PostMapping("/partner/cases/{id}/documents/{documentId}/accept")
	public String acceptStagedDocument(@PathVariable("id") Long caseId, @PathVariable Long documentId, Model model) {
		Case case_ = caseService.findById(caseId);
		documentService.addStagedToCase(case_, List.of(documentId));
		
		populateCaseDetailModel(case_, model);
		model.addAttribute("success", "Document added to case.");
		return "partner/case-details";
	}
	
	@PostMapping("/partner/cases/{id}/documents/{documentId}/reject")
	public String rejectStagedDocument(@PathVariable("id") Long caseId, @PathVariable Long documentId, Model model) {
		Case case_ = caseService.findById(caseId);
		documentService.deleteStaged(case_, List.of(documentId));
		
		populateCaseDetailModel(case_, model);
		model.addAttribute("success", "Document deleted.");
		return "partner/case-details";
	}
	
	@PostMapping("/partner/cases/{id}/documents/{documentId}/rename")
	public String renameCaseDocument(@PathVariable("id") Long caseId, @PathVariable Long documentId,
									 @Valid @ModelAttribute RenameDocumentRequest form, BindingResult bindingResult,
									 Model model) {
		Case case_ = caseService.findById(caseId);
		
		if (bindingResult.hasErrors()) {
			model.addAttribute("error", bindingResult.getAllErrors().getFirst().getDefaultMessage());
			populateCaseDetailModel(case_, model);
			return "partner/case-details";
		}
		
		documentService.rename(case_, documentId, form.getOriginalFilename());
		
		populateCaseDetailModel(case_, model);
		model.addAttribute("success", "Document renamed.");
		return "partner/case-details";
	}
	
	@PostMapping("/partner/cases/{id}/documents/{documentId}/delete")
	public String deleteCaseDocument(@PathVariable("id") Long caseId, @PathVariable Long documentId, Model model) {
		Case case_ = caseService.findById(caseId);
		documentService.deleteStaged(case_, List.of(documentId));
		
		populateCaseDetailModel(case_, model);
		model.addAttribute("success", "Document deleted.");
		return "partner/case-details";
	}
	
	@PostMapping("/partner/cases/{id}/contacts")
	public String addContact(@PathVariable("id") Long caseId,
							 @Valid @ModelAttribute SaveContactRequest form, BindingResult bindingResult,
							 Model model) {
		Case case_ = caseService.findById(caseId);
		
		if (bindingResult.hasErrors()) {
			model.addAttribute("error", bindingResult.getAllErrors().getFirst().getDefaultMessage());
			populateCaseDetailModel(case_, model);
			return "partner/case-details";
		}
		
		try {
			contactService.add(case_, form.getName(), form.getPhoneNumbers(), form.getEmails(), form.getAddresses(), form.getNotes());
			populateCaseDetailModel(case_, model);
			model.addAttribute("success", "Contact added.");
		} catch (InvalidStateException e) {
			populateCaseDetailModel(case_, model);
			model.addAttribute("error", e.getMessage());
		}
		
		return "partner/case-details";
	}
	
	@PostMapping("/partner/cases/{id}/contacts/{contactId}/edit")
	public String editContact(@PathVariable("id") Long caseId, @PathVariable Long contactId,
							  @Valid @ModelAttribute SaveContactRequest form, BindingResult bindingResult,
							  Model model) {
		Case case_ = caseService.findById(caseId);
		
		if (bindingResult.hasErrors()) {
			model.addAttribute("error", bindingResult.getAllErrors().getFirst().getDefaultMessage());
			populateCaseDetailModel(case_, model);
			return "partner/case-details";
		}
		
		Contact contact = contactService.findById(contactId);
		
		try {
			contactService.update(case_, contact, form.getName(), form.getPhoneNumbers(), form.getEmails(), form.getAddresses(), form.getNotes());
			populateCaseDetailModel(case_, model);
			model.addAttribute("success", "Contact updated.");
		} catch (InvalidStateException e) {
			populateCaseDetailModel(case_, model);
			model.addAttribute("error", e.getMessage());
		}
		
		return "partner/case-details";
	}
	
	@PostMapping("/partner/cases/{id}/tasks")
	public String addTask(@PathVariable("id") Long caseId,
						  @Valid @ModelAttribute AddTaskRequest form, BindingResult bindingResult,
						  Model model) {
		Case case_ = caseService.findById(caseId);
		
		if (bindingResult.hasErrors()) {
			model.addAttribute("error", bindingResult.getAllErrors().getFirst().getDefaultMessage());
			populateCaseDetailModel(case_, model);
			return "partner/case-details";
		}
		
		try {
			taskService.add(case_, form.getTitle(), form.getDocumentingTask(), form.getSchedulingTask(), form.getBillingTask(),
							form.getCaseFiles(),
							form.getDate(), form.getStartTime(), form.getEndTime(), form.getLocation(), form.getNotes(),
							form.getBillableAmount());
			populateCaseDetailModel(case_, model);
			model.addAttribute("success", "Task added.");
		} catch (InvalidStateException | BusinessRuleViolationException e) {
			populateCaseDetailModel(case_, model);
			model.addAttribute("error", e.getMessage());
		}
		
		return "partner/case-details";
	}
	
	@PostMapping("/partner/cases/{id}/tasks/{taskId}/edit-title")
	public String editTaskTitle(@PathVariable("id") Long caseId, @PathVariable Long taskId,
								@Valid @ModelAttribute RenameTaskRequest form, BindingResult bindingResult,
								Model model) {
		Case case_ = caseService.findById(caseId);
		
		if (bindingResult.hasErrors()) {
			model.addAttribute("error", bindingResult.getAllErrors().getFirst().getDefaultMessage());
			populateCaseDetailModel(case_, model);
			return "partner/case-details";
		}
		
		taskService.updateTitle(taskService.findById(taskId), form.getTitle());
		
		populateCaseDetailModel(case_, model);
		model.addAttribute("success", "Task renamed.");
		return "partner/case-details";
	}
	
	@PostMapping("/partner/cases/{id}/tasks/{taskId}/reschedule")
	public String rescheduleTaskEvent(@PathVariable("id") Long caseId, @PathVariable Long taskId,
									  @Valid @ModelAttribute ScheduleMeetingRequest form, BindingResult bindingResult,
									  Model model) {
		Case case_ = caseService.findById(caseId);
		
		if (bindingResult.hasErrors()) {
			model.addAttribute("error", bindingResult.getAllErrors().getFirst().getDefaultMessage());
			populateCaseDetailModel(case_, model);
			return "partner/case-details";
		}
		
		try {
			taskService.rescheduleEvent(taskService.findById(taskId), form.getSubject(), form.getDate(), form.getStartTime(), form.getEndTime(), form.getLocation(), form.getNotes());
			populateCaseDetailModel(case_, model);
			model.addAttribute("success", "Event rescheduled successfully.");
		} catch (InvalidStateException | BusinessRuleViolationException e) {
			populateCaseDetailModel(case_, model);
			model.addAttribute("error", e.getMessage());
		}
		
		return "partner/case-details";
	}
	
	@PostMapping("/partner/cases/{id}/reassign")
	public String reassignCase(@PathVariable("id") Long caseId,
							   @Valid @ModelAttribute AssignStaffRequest form, BindingResult bindingResult,
							   Model model) {
		Case case_ = caseService.findById(caseId);
		
		if (bindingResult.hasErrors()) {
			model.addAttribute("error", bindingResult.getAllErrors().getFirst().getDefaultMessage());
			populateCaseDetailModel(case_, model);
			return "partner/case-details";
		}
		
		Associate associate = (Associate) userService.findById(form.getAssociateId());
		Paralegal paralegal = (Paralegal) userService.findById(form.getParalegalId());
		
		try {
			case_ = caseService.reassignStaff(case_, associate, paralegal);
			populateCaseDetailModel(case_, model);
			model.addAttribute("success", "Case reassigned successfully.");
		} catch (InvalidStateException e) {
			populateCaseDetailModel(case_, model);
			model.addAttribute("error", e.getMessage());
		}
		
		return "partner/case-details";
	}
	
	@PostMapping("/partner/cases/{id}/invoices/compile")
	public String compileInvoice(@PathVariable("id") Long caseId,
								 @Valid @ModelAttribute CompileInvoiceRequest form,
								 Model model) {
		Case case_ = caseService.findById(caseId);
		
		try {
			invoiceService.compile(case_, form.getTaskIds());
			populateCaseDetailModel(case_, model);
			model.addAttribute("success", "Invoice compiled successfully.");
		} catch (BusinessRuleViolationException e) {
			populateCaseDetailModel(case_, model);
			model.addAttribute("error", e.getMessage());
		}
		
		return "partner/case-details";
	}
	
	@PostMapping("/partner/cases/{id}/invoices/{invoiceId}/charge")
	public String chargeInvoice(@PathVariable("id") Long caseId, @PathVariable Long invoiceId, Model model) {
		Case case_ = caseService.findById(caseId);
		
		try {
			invoiceService.chargeInvoice(case_, invoiceService.findById(invoiceId));
			populateCaseDetailModel(case_, model);
			model.addAttribute("success", "Invoice charged successfully.");
		} catch (InvalidInvoiceStateException | InsufficientFundsException e) {
			populateCaseDetailModel(case_, model);
			model.addAttribute("error", e.getMessage());
		}
		
		return "partner/case-details";
	}
	
	@PostMapping("/partner/cases/{id}/tasks/{taskId}/edit-billable-amount")
	public String editBillableAmount(@PathVariable("id") Long caseId, @PathVariable Long taskId,
									 @Valid @ModelAttribute EditBillableAmountRequest form, BindingResult bindingResult,
									 Model model) {
		Case case_ = caseService.findById(caseId);
		
		if (bindingResult.hasErrors()) {
			model.addAttribute("error", bindingResult.getAllErrors().getFirst().getDefaultMessage());
			populateCaseDetailModel(case_, model);
			return "partner/case-details";
		}
		
		try {
			taskService.updateBillableAmount(taskService.findById(taskId), form.getAmount());
			populateCaseDetailModel(case_, model);
			model.addAttribute("success", "Charge amount updated.");
		} catch (BusinessRuleViolationException e) {
			populateCaseDetailModel(case_, model);
			model.addAttribute("error", e.getMessage());
		}
		
		return "partner/case-details";
	}
	
	@PostMapping("/partner/cases/{id}/invoices/{invoiceId}/void")
	public String voidInvoice(@PathVariable("id") Long caseId, @PathVariable Long invoiceId, Model model) {
		Case case_ = caseService.findById(caseId);
		
		try {
			invoiceService.voidInvoice(invoiceService.findById(invoiceId));
			populateCaseDetailModel(case_, model);
			model.addAttribute("success", "Invoice voided. Its charges are unbilled again.");
		} catch (InvalidInvoiceStateException e) {
			populateCaseDetailModel(case_, model);
			model.addAttribute("error", e.getMessage());
		}
		
		return "partner/case-details";
	}
	
	@PostMapping("/partner/cases/{id}/invoices/{invoiceId}/void-refund")
	public String voidAndRefund(@PathVariable("id") Long caseId, @PathVariable Long invoiceId, Model model) {
		Case case_ = caseService.findById(caseId);
		
		try {
			invoiceService.voidAndRefundInvoice(case_, invoiceService.findById(invoiceId));
			populateCaseDetailModel(case_, model);
			model.addAttribute("success", "Invoice voided and refunded. Its charges are unbilled again.");
		} catch (InvalidInvoiceStateException e) {
			populateCaseDetailModel(case_, model);
			model.addAttribute("error", e.getMessage());
		}
		
		return "partner/case-details";
	}
	
	@GetMapping("/partner/documents/case/{id}")
	public ResponseEntity<Resource> viewCaseDocument(@PathVariable("id") Long documentId, Authentication authentication) {
		return documentService.streamCaseDocument(documentService.findById(documentId), currentPartner(authentication));
	}
	
	@GetMapping("/partner/documents/invoice/{id}")
	public ResponseEntity<Resource> viewInvoiceDocument(@PathVariable("id") Long documentId, Authentication authentication) {
		return documentService.streamInvoiceDocument(documentService.findById(documentId), currentPartner(authentication));
	}
	
	// ==================== Finance (Firm Transactions) ====================
	
	@GetMapping("/partner/transactions")
	public String transactions(Model model) {
		model.addAttribute("transactions", transactionService.findAll());
		model.addAttribute("totalCharges", transactionService.totalCharges());
		model.addAttribute("totalRefunds", transactionService.totalRefunds());
		model.addAttribute("netRevenue", transactionService.netRevenue());
		return "partner/transactions";
	}
	
	@GetMapping("/partner/users/{id}")
	public String userDetails(@PathVariable("id") Long userId, Authentication authentication, Model model) {
		User target = userService.findById(userId);
		populateUserDetailModel(target, authentication, model);
		return userDetailsView(target);
	}
	
	private void populateUserDetailModel(User target, Authentication authentication, Model model) {
		model.addAttribute("user", target);
		model.addAttribute("isSelf", target.getEmail().equals(authentication.getName()));
		model.addAttribute("isAssociate", target instanceof Associate);
		model.addAttribute("isClient", target instanceof Client);
		model.addAttribute("genders", Gender.values());
		
		switch (target) {
			case Client client -> {
				model.addAttribute("relatedConsultations", consultationService.findAllForClient(client));
				model.addAttribute("relatedCases", caseService.findAllForClient(client));
				model.addAttribute("relatedEvents", eventService.findAllForClient(client));
				model.addAttribute("relatedTransactions", transactionService.findAllForClient(client));
			}
			case Associate associate -> {
				model.addAttribute("relatedConsultations", consultationService.findAllForAssociate(associate));
				model.addAttribute("relatedCases", caseService.findAllForAssociate(associate));
				model.addAttribute("relatedEvents", eventService.findAllForAssociate(associate));
			}
			case Paralegal paralegal -> {
				model.addAttribute("relatedConsultations", consultationService.findAllForParalegal(paralegal));
				model.addAttribute("relatedCases", caseService.findAllForParalegal(paralegal));
				model.addAttribute("relatedEvents", eventService.findAllForParalegal(paralegal));
			}
			default -> {
			}
		}
		
		model.addAttribute("active", switch (target) {
			case Partner ignored -> "users-partners";
			case Associate ignored -> "users-associates";
			case Paralegal ignored -> "users-paralegals";
			case Client ignored -> "users-clients";
			case ClientRelationsOfficer ignored -> "users-client-relations-officers";
			case FinanceOfficer ignored -> "users-finance-officers";
			default -> "";
		});
	}
	
	private String userDetailsView(User target) {
		return target instanceof Partner ? "partner/partner-details" : "partner/user-details";
	}
	
	@PostMapping("/partner/users/{id}/edit")
	public String editUser(@PathVariable("id") Long userId,
						   @Valid @ModelAttribute EditProfileRequest form, BindingResult bindingResult,
						   Authentication authentication, Model model) {
		User target = userService.findById(userId);
		
		if (bindingResult.hasErrors()) {
			model.addAttribute("error", bindingResult.getAllErrors().getFirst().getDefaultMessage());
			populateUserDetailModel(target, authentication, model);
			return userDetailsView(target);
		}
		
		try {
			target = userService.updateDetails(target, form.getName(), form.getDateOfBirth(), form.getGender(),
											   form.getAddresses(), form.getPhoneNumbers(), form.getNicFile(), form.getBarFile());
			model.addAttribute("success", "User updated successfully.");
		} catch (NonPdfDocumentException e) {
			model.addAttribute("error", e.getMessage());
		}
		
		populateUserDetailModel(target, authentication, model);
		return userDetailsView(target);
	}
	
	@PostMapping("/partner/users/{id}/reset-password")
	public String resetUserPassword(@PathVariable("id") Long userId,
									@Valid @ModelAttribute ResetPasswordRequest form, BindingResult bindingResult,
									Authentication authentication, HttpServletRequest request, HttpServletResponse response,
									Model model, RedirectAttributes redirectAttributes) {
		User target = userService.findById(userId);
		
		if (bindingResult.hasErrors()) {
			model.addAttribute("error", bindingResult.getAllErrors().getFirst().getDefaultMessage());
			populateUserDetailModel(target, authentication, model);
			return userDetailsView(target);
		}
		
		target = userService.changePassword(target, form.getTemporaryPassword());
		
		if (target.getEmail().equals(authentication.getName())) {
			new SecurityContextLogoutHandler().logout(request, response, authentication);
			redirectAttributes.addFlashAttribute("success", "Password reset successfully. Please log in again.");
			return "redirect:/auth/login";
		}
		
		populateUserDetailModel(target, authentication, model);
		model.addAttribute("success", "Password reset successfully.");
		return userDetailsView(target);
	}
	
	@PostMapping("/partner/users/{id}/delete")
	public String deleteUser(@PathVariable("id") Long userId,
							 Authentication authentication, HttpServletRequest request, HttpServletResponse response,
							 Model model, RedirectAttributes redirectAttributes) {
		User target = userService.findById(userId);
		
		try {
			userService.delete(target);
		} catch (BusinessRuleViolationException e) {
			populateUserDetailModel(target, authentication, model);
			model.addAttribute("error", e.getMessage());
			return userDetailsView(target);
		}
		
		if (target.getEmail().equals(authentication.getName())) {
			new SecurityContextLogoutHandler().logout(request, response, authentication);
			redirectAttributes.addFlashAttribute("success", "Account deleted.");
			return "redirect:/auth/login";
		}
		
		String listUrl = switch (target) {
			case Partner ignored -> "/partner/users/partners";
			case Associate ignored -> "/partner/users/associates";
			case Paralegal ignored -> "/partner/users/paralegals";
			case Client ignored -> "/partner/users/clients";
			case ClientRelationsOfficer ignored -> "/partner/users/client-relations-officers";
			case FinanceOfficer ignored -> "/partner/users/finance-officers";
			default -> "/partner/dashboard";
		};
		
		redirectAttributes.addFlashAttribute("success", "User deleted successfully.");
		return "redirect:" + listUrl;
	}
	
	@PostMapping("/partner/users/{id}/reactivate")
	public String reactivateUser(@PathVariable("id") Long userId,
								 @Valid @ModelAttribute ReactivateAccountRequest form, BindingResult bindingResult,
								 Authentication authentication, Model model) {
		User target = userService.findById(userId);
		
		if (bindingResult.hasErrors()) {
			model.addAttribute("error", bindingResult.getAllErrors().getFirst().getDefaultMessage());
			populateUserDetailModel(target, authentication, model);
			return userDetailsView(target);
		}
		
		try {
			target = userService.reactivate(target, form.getTemporaryPassword());
			populateUserDetailModel(target, authentication, model);
			model.addAttribute("success", "Account reactivated. Give the user the temporary password so they can log in and change it.");
		} catch (InvalidStateException | BusinessRuleViolationException e) {
			populateUserDetailModel(target, authentication, model);
			model.addAttribute("error", e.getMessage());
		}
		
		return userDetailsView(target);
	}
	
	// ==================== Schedule (Firm Events) ====================
	
	@GetMapping("/partner/events")
	public String events(Model model) {
		model.addAttribute("events", eventService.findAll());
		return "partner/event-list";
	}
	
	@GetMapping("/partner/events/{id}")
	public String eventDetails(@PathVariable("id") Long eventId, Model model) {
		model.addAttribute("event", eventService.findById(eventId));
		return "partner/event-details";
	}
	
	@PostMapping("/partner/events/{id}/complete")
	public String completeEvent(@PathVariable("id") Long eventId, RedirectAttributes redirectAttributes) {
		
		try {
			eventService.complete(eventService.findById(eventId));
			redirectAttributes.addFlashAttribute("success", "Event marked as completed.");
		} catch (InvalidStateException | BusinessRuleViolationException e) {
			redirectAttributes.addFlashAttribute("error", e.getMessage());
		}
		
		return "redirect:/partner/events/{id}";
	}
	
	@PostMapping("/partner/events/{id}/cancel")
	public String cancelEvent(@PathVariable("id") Long eventId, RedirectAttributes redirectAttributes) {
		
		try {
			eventService.cancel(eventService.findById(eventId));
			redirectAttributes.addFlashAttribute("success", "Event canceled.");
		} catch (InvalidStateException e) {
			redirectAttributes.addFlashAttribute("error", e.getMessage());
		}
		
		return "redirect:/partner/events/{id}";
	}
	
	@GetMapping("/partner/analytics")
	public String analytics(Authentication authentication, Model model) {
		model.addAttribute("user", currentPartner(authentication));
		
		Map<CaseResolutionStatus, Integer> caseResolutionBreakdown = analyticsService.caseResolutionBreakdown();
		model.addAttribute("caseResolutionLabels", buildLabels(caseResolutionBreakdown));
		model.addAttribute("caseResolutionValues", buildIntValues(caseResolutionBreakdown));
		
		model.addAttribute("avgCaseDurationDays", analyticsService.avgCaseDurationDays());
		
		Map<Integer, BigDecimal> revenueByMonth = analyticsService.revenueByMonth(LocalDate.now().getYear());
		model.addAttribute("revenueByMonthLabels", monthLabels());
		model.addAttribute("revenueByMonthValues", revenueByMonth.values());
		
		Map<CaseType, BigDecimal> revenueByCaseType = analyticsService.revenueByCaseType();
		model.addAttribute("revenueByCaseTypeLabels", buildLabels(revenueByCaseType));
		model.addAttribute("revenueByCaseTypeValues", buildDecimalValues(revenueByCaseType));
		
		Map<User, Integer> associateClosedCounts = analyticsService.closedCaseCountByAssociate();
		model.addAttribute("associateClosedLabels", buildStaffLabels(associateClosedCounts));
		model.addAttribute("associateClosedValues", buildIntValues(associateClosedCounts));
		
		Map<User, Integer> paralegalClosedCounts = analyticsService.closedCaseCountByParalegal();
		model.addAttribute("paralegalClosedLabels", buildStaffLabels(paralegalClosedCounts));
		model.addAttribute("paralegalClosedValues", buildIntValues(paralegalClosedCounts));
		
		return "partner/analytics";
	}
	
	private List<String> buildLabels(Map<?, ?> counts) {
		List<String> labels = new ArrayList<>();
		
		for (Object key : counts.keySet()) {
			labels.add(key.toString());
		}
		
		return labels;
	}
	
	private List<String> buildStaffLabels(Map<User, Integer> workload) {
		List<String> labels = new ArrayList<>();
		
		for (User staff : workload.keySet()) {
			labels.add(staff.getName());
		}
		
		return labels;
	}
	
	private List<Integer> buildIntValues(Map<?, Integer> counts) {
		List<Integer> values = new ArrayList<>();
		
		for (Integer value : counts.values()) {
			values.add(value);
		}
		
		return values;
	}
	
	private List<BigDecimal> buildDecimalValues(Map<?, BigDecimal> sums) {
		List<BigDecimal> values = new ArrayList<>();
		
		for (BigDecimal value : sums.values()) {
			values.add(value == null ? BigDecimal.ZERO : value);
		}
		
		return values;
	}
	
	private List<String> monthLabels() {
		return List.of("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec");
	}
	
}
