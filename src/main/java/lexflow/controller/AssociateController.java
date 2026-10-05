package lexflow.controller;

import jakarta.validation.Valid;
import lexflow.dto.consultation.RejectConsultationRequest;
import lexflow.dto.scheduling.ScheduleMeetingRequest;
import lexflow.dto.user.ChangePasswordRequest;
import lexflow.entity.case_.Case;
import lexflow.entity.consultation.Consultation;
import lexflow.entity.scheduling.ConsultationEvent;
import lexflow.entity.user.Associate;
import lexflow.exception.BusinessRuleViolationException;
import lexflow.exception.InvalidStateException;
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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class AssociateController {
	
	private final CaseService caseService;
	
	private final ConsultationService consultationService;
	
	private final DocumentService documentService;
	
	private final EventService eventService;
	
	private final UserService userService;
	
	// ==================== Overview (Dashboard, Profile) ====================
	
	@GetMapping("/associate/dashboard")
	public String dashboard(Authentication authentication, Model model) {
		model.addAttribute("user", currentAssociate(authentication));
		return "associate/dashboard";
	}
	
	// ==================== Helpers (shared across sections) ====================
	
	private Associate currentAssociate(Authentication authentication) {
		CustomUserPrincipal principal = (CustomUserPrincipal) authentication.getPrincipal();
		return (Associate) principal.user();
	}
	
	@GetMapping("/associate/profile")
	public String profile(Authentication authentication, Model model) {
		model.addAttribute("user", currentAssociate(authentication));
		return "associate/profile";
	}
	
	@PostMapping("/associate/profile/change-password")
	public String changePassword(@Valid @ModelAttribute ChangePasswordRequest form, BindingResult bindingResult, Authentication authentication, Model model) {
		Associate associate = currentAssociate(authentication);
		
		if (bindingResult.hasErrors()) {
			model.addAttribute("error", bindingResult.getAllErrors().getFirst().getDefaultMessage());
			model.addAttribute("user", associate);
			return "associate/profile";
		}
		
		if (!userService.verifyPassword(associate, form.getCurrentPassword())) {
			model.addAttribute("user", associate);
			model.addAttribute("error", "Current password is incorrect.");
			return "associate/profile";
		}
		
		userService.changePassword(associate, form.getNewPassword());
		
		model.addAttribute("user", associate);
		model.addAttribute("success", "Password changed successfully.");
		return "associate/profile";
	}
	
	@GetMapping("/associate/documents/user/{id}")
	public ResponseEntity<Resource> viewUserDocument(@PathVariable("id") Long documentId, Authentication authentication) {
		return documentService.streamUserDocument(documentService.findById(documentId), currentAssociate(authentication));
	}
	
	// ==================== Consultations (Pending, All) ====================
	
	@GetMapping("/associate/consultations/pending")
	public String pendingConsultations(Authentication authentication, Model model) {
		model.addAttribute("consultations", consultationService.findAwaitingReviewForAssociate(currentAssociate(authentication)));
		return "associate/consultation-pending-list";
	}
	
	@GetMapping("/associate/consultations")
	public String consultations(Authentication authentication, Model model) {
		model.addAttribute("consultations", consultationService.findAllForAssociate(currentAssociate(authentication)));
		return "associate/consultation-list";
	}
	
	@GetMapping("/associate/consultations/{id}")
	public String consultationDetails(@PathVariable("id") Long consultationId, Authentication authentication, Model model) {
		Consultation consultation = consultationService.findOwnedByAssociate(consultationId, currentAssociate(authentication));
		populateConsultationDetailModel(consultation, model);
		return "associate/consultation-details";
	}
	
	private void populateConsultationDetailModel(Consultation consultation, Model model) {
		model.addAttribute("consultation", consultation);
		model.addAttribute("consultationDocuments", documentService.findConsultationDocuments(consultation));
		model.addAttribute("meetings", eventService.findByConsultation(consultation));
		model.addAttribute("meetingCompleted", consultationService.hasCompletedMeeting(consultation));
		model.addAttribute("existingCase", caseService.findByConsultation(consultation));
	}
	
	@PostMapping("/associate/consultations/{id}/accept")
	public String acceptConsultation(@PathVariable("id") Long consultationId, Authentication authentication, Model model) {
		Consultation consultation = consultationService.findOwnedByAssociate(consultationId, currentAssociate(authentication));
		
		try {
			consultation = consultationService.associateAccept(consultation);
			populateConsultationDetailModel(consultation, model);
			model.addAttribute("success", "Consultation request accepted.");
		} catch (InvalidStateException | BusinessRuleViolationException e) {
			populateConsultationDetailModel(consultation, model);
			model.addAttribute("error", e.getMessage());
		}
		
		return "associate/consultation-details";
	}
	
	@PostMapping("/associate/consultations/{id}/reject")
	public String rejectConsultation(@PathVariable("id") Long consultationId, @Valid @ModelAttribute RejectConsultationRequest form, BindingResult bindingResult, Authentication authentication, Model model) {
		Consultation consultation = consultationService.findOwnedByAssociate(consultationId, currentAssociate(authentication));
		
		if (bindingResult.hasErrors()) {
			model.addAttribute("error", bindingResult.getAllErrors().getFirst().getDefaultMessage());
			populateConsultationDetailModel(consultation, model);
			return "associate/consultation-details";
		}
		
		try {
			consultation = consultationService.associateReject(consultation, form.getRejectionReason());
			populateConsultationDetailModel(consultation, model);
			model.addAttribute("success", "Consultation request rejected.");
		} catch (InvalidStateException | BusinessRuleViolationException e) {
			populateConsultationDetailModel(consultation, model);
			model.addAttribute("error", e.getMessage());
		}
		
		return "associate/consultation-details";
	}
	
	@PostMapping("/associate/consultations/{id}/schedule")
	public String scheduleMeeting(@PathVariable("id") Long consultationId, @Valid @ModelAttribute ScheduleMeetingRequest form, BindingResult bindingResult, Authentication authentication, Model model) {
		Consultation consultation = consultationService.findOwnedByAssociate(consultationId, currentAssociate(authentication));
		
		if (bindingResult.hasErrors()) {
			model.addAttribute("error", bindingResult.getAllErrors().getFirst().getDefaultMessage());
			populateConsultationDetailModel(consultation, model);
			return "associate/consultation-details";
		}
		
		try {
			eventService.schedule(consultation, form.getSubject(), form.getDate(), form.getStartTime(), form.getEndTime(), form.getLocation(), form.getNotes());
			populateConsultationDetailModel(consultation, model);
			model.addAttribute("success", "Meeting scheduled successfully.");
		} catch (InvalidStateException | BusinessRuleViolationException e) {
			populateConsultationDetailModel(consultation, model);
			model.addAttribute("error", e.getMessage());
		}
		
		return "associate/consultation-details";
	}
	
	@PostMapping("/associate/consultations/{id}/events/{eventId}/reschedule")
	public String rescheduleMeeting(@PathVariable("id") Long consultationId, @PathVariable Long eventId, @Valid @ModelAttribute ScheduleMeetingRequest form, BindingResult bindingResult, Authentication authentication, Model model) {
		Consultation consultation = consultationService.findOwnedByAssociate(consultationId, currentAssociate(authentication));
		
		if (bindingResult.hasErrors()) {
			model.addAttribute("error", bindingResult.getAllErrors().getFirst().getDefaultMessage());
			populateConsultationDetailModel(consultation, model);
			return "associate/consultation-details";
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
		
		return "associate/consultation-details";
	}
	
	@PostMapping("/associate/consultations/{id}/events/{eventId}/complete")
	public String completeMeeting(@PathVariable("id") Long consultationId, @PathVariable Long eventId, Authentication authentication, Model model) {
		Consultation consultation = consultationService.findOwnedByAssociate(consultationId, currentAssociate(authentication));
		
		try {
			eventService.completeEvent(eventService.findOwnedConsultationEvent(eventId, consultation));
			populateConsultationDetailModel(consultation, model);
			model.addAttribute("success", "Meeting marked as completed.");
		} catch (InvalidStateException | BusinessRuleViolationException e) {
			populateConsultationDetailModel(consultation, model);
			model.addAttribute("error", e.getMessage());
		}
		
		return "associate/consultation-details";
	}
	
	@PostMapping("/associate/consultations/{id}/events/{eventId}/cancel")
	public String cancelMeeting(@PathVariable("id") Long consultationId, @PathVariable Long eventId, Authentication authentication, Model model) {
		Consultation consultation = consultationService.findOwnedByAssociate(consultationId, currentAssociate(authentication));
		
		try {
			eventService.cancelEvent(eventService.findOwnedConsultationEvent(eventId, consultation));
			populateConsultationDetailModel(consultation, model);
			model.addAttribute("success", "Meeting canceled.");
		} catch (InvalidStateException e) {
			populateConsultationDetailModel(consultation, model);
			model.addAttribute("error", e.getMessage());
		}
		
		return "associate/consultation-details";
	}
	
	@PostMapping("/associate/consultations/{id}/open-case")
	public String openCase(@PathVariable("id") Long consultationId, Authentication authentication, RedirectAttributes redirectAttributes) {
		Consultation consultation = consultationService.findOwnedByAssociate(consultationId, currentAssociate(authentication));
		Case case_ = caseService.open(consultation);
		
		redirectAttributes.addFlashAttribute("success", "Case opened successfully.");
		return "redirect:/associate/cases/" + case_.getCaseId();
	}
	
	// ==================== Documents (file downloads, no nav item of their own) ====================
	
	@GetMapping("/associate/documents/consultation/{id}")
	public ResponseEntity<Resource> viewConsultationDocument(@PathVariable("id") Long documentId, Authentication authentication) {
		return documentService.streamConsultationDocument(documentService.findById(documentId), currentAssociate(authentication));
	}
	
	// ==================== Cases (Open, Closed) ====================
	
	@GetMapping("/associate/cases/open")
	public String openCases(Authentication authentication, Model model) {
		model.addAttribute("cases", caseService.findOpenForAssociate(currentAssociate(authentication)));
		model.addAttribute("title", "Open Cases");
		model.addAttribute("active", "cases-open");
		return "associate/case-list";
	}
	
	@GetMapping("/associate/cases/closed")
	public String closedCases(Authentication authentication, Model model) {
		model.addAttribute("cases", caseService.findClosedForAssociate(currentAssociate(authentication)));
		model.addAttribute("title", "Closed Cases");
		model.addAttribute("active", "cases-closed");
		return "associate/case-list";
	}
	
}
