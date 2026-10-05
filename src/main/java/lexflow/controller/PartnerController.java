package lexflow.controller;

import jakarta.validation.Valid;
import lexflow.dto.consultation.AssignStaffRequest;
import lexflow.dto.consultation.RejectConsultationRequest;
import lexflow.dto.scheduling.ScheduleMeetingRequest;
import lexflow.dto.user.*;
import lexflow.entity.case_.Case;
import lexflow.entity.consultation.Consultation;
import lexflow.entity.scheduling.ConsultationEvent;
import lexflow.entity.user.Role;
import lexflow.entity.user.*;
import lexflow.exception.BusinessRuleViolationException;
import lexflow.exception.EntityAlreadyExistsException;
import lexflow.exception.InvalidStateException;
import lexflow.exception.PersistenceException;
import lexflow.exception.document.NonPdfDocumentException;
import lexflow.security.CustomUserPrincipal;
import lexflow.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class PartnerController {
	
	private final CaseService caseService;
	
	private final ConsultationService consultationService;
	
	private final ConflictCheckService conflictCheckService;
	
	private final DocumentService documentService;
	
	private final EventService eventService;
	
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
		model.addAttribute("users", userService.search(Role.ASSOCIATE, keyword));
		model.addAttribute("keyword", keyword);
		model.addAttribute("title", "Associates");
		model.addAttribute("active", "users-associates");
		model.addAttribute("listUrl", "/partner/users/associates");
		return "partner/user-list";
	}
	
	@GetMapping("/partner/users/paralegals")
	public String paralegals(@RequestParam(required = false) String keyword, Model model) {
		model.addAttribute("users", userService.search(Role.PARALEGAL, keyword));
		model.addAttribute("keyword", keyword);
		model.addAttribute("title", "Paralegals");
		model.addAttribute("active", "users-paralegals");
		model.addAttribute("listUrl", "/partner/users/paralegals");
		return "partner/user-list";
	}
	
	@GetMapping("/partner/users/client-relations-officers")
	public String clientRelationsOfficers(@RequestParam(required = false) String keyword, Model model) {
		model.addAttribute("users", userService.search(Role.CLIENT_RELATIONS_OFFICER, keyword));
		model.addAttribute("keyword", keyword);
		model.addAttribute("title", "Client Relations Officers");
		model.addAttribute("active", "users-client-relations-officers");
		model.addAttribute("listUrl", "/partner/users/client-relations-officers");
		return "partner/user-list";
	}
	
	@GetMapping("/partner/users/finance-officers")
	public String financeOfficers(@RequestParam(required = false) String keyword, Model model) {
		model.addAttribute("users", userService.search(Role.FINANCE_OFFICER, keyword));
		model.addAttribute("keyword", keyword);
		model.addAttribute("title", "Finance Officers");
		model.addAttribute("active", "users-finance-officers");
		model.addAttribute("listUrl", "/partner/users/finance-officers");
		return "partner/user-list";
	}
	
	@GetMapping("/partner/users/clients")
	public String clients(@RequestParam(required = false) String keyword, Model model) {
		model.addAttribute("users", userService.search(Role.CLIENT, keyword));
		model.addAttribute("keyword", keyword);
		model.addAttribute("title", "Clients");
		model.addAttribute("active", "users-clients");
		model.addAttribute("listUrl", "/partner/users/clients");
		return "partner/user-list";
	}
	
	@GetMapping("/partner/users/partners")
	public String partners(@RequestParam(required = false) String keyword, Authentication authentication, Model model) {
		model.addAttribute("users", userService.search(Role.PARTNER, keyword));
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
		model.addAttribute("consultationDocuments", documentService.findConsultationDocuments(consultation));
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
		
		ConsultationEvent event = eventService.findOwnedConsultationEvent(eventId, consultation);
		
		try {
			eventService.rescheduleConsultationEvent(event, form.getSubject(), form.getDate(), form.getStartTime(), form.getEndTime(), form.getLocation(), form.getNotes());
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
			eventService.completeEvent(eventService.findOwnedConsultationEvent(eventId, consultation));
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
			eventService.cancelEvent(eventService.findOwnedConsultationEvent(eventId, consultation));
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
	
}
