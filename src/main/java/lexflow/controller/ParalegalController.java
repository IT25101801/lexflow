package lexflow.controller;

import jakarta.validation.Valid;
import lexflow.dto.user.ChangePasswordRequest;
import lexflow.entity.consultation.Consultation;
import lexflow.entity.user.Paralegal;
import lexflow.entity.user.User;
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

@Controller
@RequiredArgsConstructor
public class ParalegalController {
	
	private final ConsultationService consultationService;
	
	private final UserService userService;
	
	private final DocumentService documentService;
	
	private final CaseService caseService;
	
	// ==================== Overview (Dashboard, Profile) ====================
	
	@GetMapping("/paralegal/dashboard")
	public String dashboard(Authentication authentication, Model model) {
		model.addAttribute("user", currentParalegal(authentication));
		return "paralegal/dashboard";
	}
	
	private Paralegal currentParalegal(Authentication authentication) {
		CustomUserPrincipal principal = (CustomUserPrincipal) authentication.getPrincipal();
		return (Paralegal) principal.user();
	}
	
	@GetMapping("/paralegal/profile")
	public String profile(Authentication authentication, Model model) {
		model.addAttribute("user", currentParalegal(authentication));
		return "paralegal/profile";
	}
	
	@PostMapping("/paralegal/profile/change-password")
	public String changePassword(Authentication authentication,
								 @Valid @ModelAttribute ChangePasswordRequest form, BindingResult bindingResult,
								 Model model) {
		User user = currentParalegal(authentication);
		
		if (bindingResult.hasErrors()) {
			model.addAttribute("error", bindingResult.getAllErrors().getFirst().getDefaultMessage());
			model.addAttribute("user", user);
			return "paralegal/profile";
		}
		
		if (!userService.verifyPassword(user, form.getCurrentPassword())) {
			model.addAttribute("user", user);
			model.addAttribute("error", "Current password is incorrect.");
			return "paralegal/profile";
		}
		
		userService.changePassword(user, form.getNewPassword());
		
		model.addAttribute("user", user);
		model.addAttribute("success", "Password changed successfully.");
		return "paralegal/profile";
	}
	
	@GetMapping("/paralegal/documents/user/{id}")
	public ResponseEntity<Resource> viewUserDocument(@PathVariable("id") Long documentId, Authentication authentication) {
		return documentService.streamUserDocument(documentService.findById(documentId), currentParalegal(authentication));
	}
	
	// ==================== Consultations (Unscheduled, All) ====================
	
	@GetMapping("/paralegal/consultations/pending")
	public String pendingConsultations(Authentication authentication, Model model) {
		model.addAttribute("consultations", consultationService.findAwaitingReviewForParalegal(currentParalegal(authentication)));
		return "paralegal/consultation-pending-list";
	}
	
	@GetMapping("/paralegal/consultations")
	public String consultations(Authentication authentication, Model model) {
		model.addAttribute("consultations", consultationService.findAllForParalegal(currentParalegal(authentication)));
		return "paralegal/consultation-list";
	}
	
	@GetMapping("/paralegal/consultations/{id}")
	public String consultationDetails(@PathVariable("id") Long consultationId, Authentication authentication, Model model) {
		Consultation consultation = consultationService.findOwnedByParalegal(consultationId, currentParalegal(authentication));
		model.addAttribute("consultation", consultation);
		model.addAttribute("consultationDocuments", documentService.findConsultationDocuments(consultation));
		model.addAttribute("existingCase", caseService.findByConsultation(consultation));
		return "paralegal/consultation-details";
	}
	
	// ==================== Documents (cross-page file streaming, no dedicated nav entry) ====================
	
	@GetMapping("/paralegal/documents/consultation/{id}")
	public ResponseEntity<Resource> viewConsultationDocument(@PathVariable("id") Long documentId, Authentication authentication) {
		return documentService.streamConsultationDocument(documentService.findById(documentId), currentParalegal(authentication));
	}
	
	// ==================== Cases (Open, Closed, Details & Actions) ====================
	
	@GetMapping("/paralegal/cases/open")
	public String openCases(Authentication authentication, Model model) {
		model.addAttribute("cases", caseService.findOpenForParalegal(currentParalegal(authentication)));
		model.addAttribute("title", "Open Cases");
		model.addAttribute("active", "cases-open");
		return "paralegal/case-list";
	}
	
	@GetMapping("/paralegal/cases/closed")
	public String closedCases(Authentication authentication, Model model) {
		model.addAttribute("cases", caseService.findClosedForParalegal(currentParalegal(authentication)));
		model.addAttribute("title", "Closed Cases");
		model.addAttribute("active", "cases-closed");
		return "paralegal/case-list";
	}
	
}
