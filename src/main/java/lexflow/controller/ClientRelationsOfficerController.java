package lexflow.controller;

import jakarta.validation.Valid;
import lexflow.dto.consultation.SubmitWalkInConsultationRequest;
import lexflow.dto.user.*;
import lexflow.entity.consultation.CaseType;
import lexflow.entity.consultation.Consultation;
import lexflow.entity.user.Client;
import lexflow.entity.user.ClientRelationsOfficer;
import lexflow.entity.user.Gender;
import lexflow.entity.user.User;
import lexflow.exception.*;
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
public class ClientRelationsOfficerController {
	
	private final UserService userService;
	
	private final ConsultationService consultationService;
	
	private final DocumentService documentService;
	
	// ==================== Overview (Dashboard, Profile) ====================
	
	@GetMapping("/client-relations-officer/dashboard")
	public String dashboard(Authentication authentication, Model model) {
		model.addAttribute("user", currentClientRelationsOfficer(authentication));
		return "client-relations-officer/dashboard";
	}
	
	private ClientRelationsOfficer currentClientRelationsOfficer(Authentication authentication) {
		CustomUserPrincipal principal = (CustomUserPrincipal) authentication.getPrincipal();
		return (ClientRelationsOfficer) principal.user();
	}
	
	@GetMapping("/client-relations-officer/profile")
	public String profile(Authentication authentication, Model model) {
		model.addAttribute("user", currentClientRelationsOfficer(authentication));
		return "client-relations-officer/profile";
	}
	
	@PostMapping("/client-relations-officer/profile/change-password")
	public String changePassword(Authentication authentication,
								 @Valid @ModelAttribute ChangePasswordRequest form, BindingResult bindingResult,
								 Model model) {
		User user = currentClientRelationsOfficer(authentication);
		
		if (bindingResult.hasErrors()) {
			model.addAttribute("error", bindingResult.getAllErrors().getFirst().getDefaultMessage());
			model.addAttribute("user", user);
			return "client-relations-officer/profile";
		}
		
		if (!userService.verifyPassword(user, form.getCurrentPassword())) {
			model.addAttribute("user", user);
			model.addAttribute("error", "Current password is incorrect.");
			return "client-relations-officer/profile";
		}
		
		userService.changePassword(user, form.getNewPassword());
		
		model.addAttribute("user", user);
		model.addAttribute("success", "Password changed successfully.");
		return "client-relations-officer/profile";
	}
	
	@GetMapping("/client-relations-officer/documents/user/{id}")
	public ResponseEntity<Resource> viewUserDocument(@PathVariable("id") Long documentId, Authentication authentication) {
		return documentService.streamUserDocument(documentService.findById(documentId), currentClientRelationsOfficer(authentication));
	}
	
	// ==================== Clients (Register Client) ====================
	
	@GetMapping("/client-relations-officer/register-client")
	public String showRegisterClientForm(Model model) {
		model.addAttribute("genders", Gender.values());
		return "client-relations-officer/register-client";
	}
	
	@PostMapping("/client-relations-officer/register-client")
	public String registerClient(@Valid @ModelAttribute RegisterClientRequest form, BindingResult bindingResult, Model model, RedirectAttributes redirectAttributes) {
		if (bindingResult.hasErrors()) {
			model.addAttribute("error", bindingResult.getAllErrors().getFirst().getDefaultMessage());
			model.addAttribute("genders", Gender.values());
			return "client-relations-officer/register-client";
		}
		
		try {
			userService.register(form.getEmail(), form.getPassword(), form.getName(), form.getDateOfBirth(),
								 form.getGender(), form.getAddresses(), form.getPhoneNumbers(),
								 form.getNicNumber(), form.getNicFile());
		} catch (EntityAlreadyExistsException | BusinessRuleViolationException e) {
			model.addAttribute("genders", Gender.values());
			model.addAttribute("error", e.getMessage());
			return "client-relations-officer/register-client";
		} catch (PersistenceException e) {
			model.addAttribute("genders", Gender.values());
			model.addAttribute("error", "Could not register the client. Please try again.");
			return "client-relations-officer/register-client";
		}
		
		redirectAttributes.addFlashAttribute("success", "Client registered successfully.");
		return "redirect:/client-relations-officer/register-client";
	}
	
	// ==================== Consultations (Submit Consultation, Consultation Requests) ====================
	
	@GetMapping("/client-relations-officer/consultations/new")
	public String showConsultationForm(Model model) {
		model.addAttribute("caseTypes", CaseType.values());
		return "client-relations-officer/consultation-form";
	}
	
	@PostMapping("/client-relations-officer/consultations")
	public String submitConsultation(@Valid @ModelAttribute SubmitWalkInConsultationRequest form, BindingResult bindingResult, Model model, RedirectAttributes redirectAttributes) {
		if (bindingResult.hasErrors()) {
			model.addAttribute("error", bindingResult.getAllErrors().getFirst().getDefaultMessage());
			model.addAttribute("caseTypes", CaseType.values());
			return "client-relations-officer/consultation-form";
		}
		
		Client client;
		try {
			client = userService.findClientByUserCode(form.getClientCode());
		} catch (EntityNotFoundException e) {
			model.addAttribute("caseTypes", CaseType.values());
			model.addAttribute("error", "No client found with code " + form.getClientCode() + ".");
			return "client-relations-officer/consultation-form";
		}
		
		List<MultipartFile> consultationFiles = form.getConsultationFiles() != null ? form.getConsultationFiles() : List.of();
		
		try {
			consultationService.submit(client, form.getCaseType(), form.getOpposingParties(), form.getDescription(), consultationFiles);
		} catch (PersistenceException e) {
			model.addAttribute("caseTypes", CaseType.values());
			model.addAttribute("error", "Could not submit the consultation request. Please try again.");
			return "client-relations-officer/consultation-form";
		}
		
		redirectAttributes.addFlashAttribute("success", "Consultation request submitted successfully.");
		return "redirect:/client-relations-officer/consultations";
	}
	
}
