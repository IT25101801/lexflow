package lexflow.controller;

import jakarta.validation.Valid;
import lexflow.dto.consultation.EditConsultationRequest;
import lexflow.dto.consultation.SubmitOnlineConsultationRequest;
import lexflow.dto.user.ChangePasswordRequest;
import lexflow.entity.consultation.CaseType;
import lexflow.entity.consultation.Consultation;
import lexflow.entity.user.Client;
import lexflow.entity.user.User;
import lexflow.exception.PersistenceException;
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

import java.util.List;

@Controller
@RequiredArgsConstructor
public class ClientController {
	
	private final ConsultationService consultationService;
	
	private final UserService userService;
	
	private final DocumentService documentService;
	
	private final CaseService caseService;
	
	// ==================== Overview (Dashboard, Profile) ====================
	
	@GetMapping("/client/dashboard")
	public String dashboard(Authentication authentication, Model model) {
		model.addAttribute("user", currentClient(authentication));
		return "client/dashboard";
	}
	
	private Client currentClient(Authentication authentication) {
		CustomUserPrincipal principal = (CustomUserPrincipal) authentication.getPrincipal();
		return (Client) principal.user();
	}
	
	@GetMapping("/client/profile")
	public String profile(Authentication authentication, Model model) {
		model.addAttribute("user", currentClient(authentication));
		return "client/profile";
	}
	
	@PostMapping("/client/profile/change-password")
	public String changePassword(Authentication authentication,
								 @Valid @ModelAttribute ChangePasswordRequest form, BindingResult bindingResult,
								 Model model) {
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
	
	@GetMapping("/client/documents/user/{id}")
	public ResponseEntity<Resource> viewUserDocument(@PathVariable("id") Long documentId, Authentication authentication) {
		return documentService.streamUserDocument(documentService.findById(documentId), currentClient(authentication));
	}
	
	// ==================== Consultations (Submit, Requests) ====================
	
	@GetMapping("/client/consultations/new")
	public String showConsultationForm(Model model) {
		model.addAttribute("caseTypes", CaseType.values());
		return "client/consultation-form";
	}
	
	@PostMapping("/client/consultations")
	public String submitConsultation(@Valid @ModelAttribute SubmitOnlineConsultationRequest form, BindingResult bindingResult,
									 Authentication authentication, Model model, RedirectAttributes redirectAttributes) {
		if (bindingResult.hasErrors()) {
			model.addAttribute("error", bindingResult.getAllErrors().getFirst().getDefaultMessage());
			model.addAttribute("caseTypes", CaseType.values());
			return "client/consultation-form";
		}
		
		List<MultipartFile> consultationFiles = form.getConsultationFiles() != null ? form.getConsultationFiles() : List.of();
		
		try {
			consultationService.submit(currentClient(authentication), form.getCaseType(), form.getOpposingParties(), form.getDescription(), consultationFiles);
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
	
	// ==================== Helpers ====================
	
	private void populateConsultationDetailModel(Consultation consultation, Model model) {
		model.addAttribute("consultation", consultation);
		model.addAttribute("consultationDocuments", documentService.findConsultationDocuments(consultation));
		model.addAttribute("caseTypes", CaseType.values());
		model.addAttribute("existingCase", caseService.findByConsultation(consultation));
	}
	
	@PostMapping("/client/consultations/{id}/edit")
	public String editConsultation(@PathVariable("id") Long consultationId, @Valid @ModelAttribute EditConsultationRequest form, BindingResult bindingResult,
								   Authentication authentication, Model model) {
		Consultation consultation = consultationService.findOwnedByClient(consultationId, currentClient(authentication));
		
		if (bindingResult.hasErrors()) {
			model.addAttribute("error", bindingResult.getAllErrors().getFirst().getDefaultMessage());
			populateConsultationDetailModel(consultation, model);
			return "client/consultation-details";
		}
		
		List<MultipartFile> consultationFiles = form.getConsultationFiles() != null ? form.getConsultationFiles() : List.of();
		
		try {
			consultation = consultationService.updateDetails(consultation, form.getCaseType(), form.getOpposingParties(), form.getDescription(),
															 consultationFiles, form.getDeleteDocumentIds(), form.getDocumentIds(), form.getOriginalFilenames());
			populateConsultationDetailModel(consultation, model);
			model.addAttribute("success", "Consultation request updated successfully.");
		} catch (InvalidConsultationStateException e) {
			populateConsultationDetailModel(consultation, model);
			model.addAttribute("error", e.getMessage());
		}
		
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
	
	// ==================== Documents (cross-cutting, embedded in the pages above) ====================
	
	@GetMapping("/client/documents/consultation/{id}")
	public ResponseEntity<Resource> viewConsultationDocument(@PathVariable("id") Long documentId, Authentication authentication) {
		return documentService.streamConsultationDocument(documentService.findById(documentId), currentClient(authentication));
	}
	
}
