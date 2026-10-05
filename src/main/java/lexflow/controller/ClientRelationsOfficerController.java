package lexflow.controller;

import jakarta.validation.Valid;
import lexflow.dto.consultation.EditConsultationRequest;
import lexflow.dto.consultation.SubmitWalkInConsultationRequest;
import lexflow.dto.user.*;
import lexflow.entity.consultation.CaseType;
import lexflow.entity.consultation.Consultation;
import lexflow.entity.user.Role;
import lexflow.entity.user.Client;
import lexflow.entity.user.ClientRelationsOfficer;
import lexflow.entity.user.Gender;
import lexflow.entity.user.User;
import lexflow.exception.*;
import lexflow.exception.consultation.InvalidConsultationStateException;
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
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class ClientRelationsOfficerController {
	
	private final UserService userService;
	
	private final ConsultationService consultationService;
	
	private final CaseService caseService;
	
	private final TransactionService transactionService;
	
	private final DocumentService documentService;
	
	private final EventService eventService;
	
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
	
	@GetMapping("/client-relations-officer/consultations")
	public String consultations(@RequestParam(required = false) String keyword, Model model) {
		model.addAttribute("consultations", consultationService.search(keyword));
		model.addAttribute("keyword", keyword);
		return "client-relations-officer/consultation-list";
	}
	
	@GetMapping("/client-relations-officer/clients")
	public String clients(@RequestParam(required = false) String keyword, Model model) {
		model.addAttribute("users", userService.search(Role.CLIENT, keyword));
		model.addAttribute("keyword", keyword);
		model.addAttribute("title", "Clients");
		model.addAttribute("active", "clients");
		return "client-relations-officer/client-list";
	}
	
	@GetMapping("/client-relations-officer/clients/{id}")
	public String clientDetails(@PathVariable("id") Long userId, Model model) {
		Client client = userService.findClientById(userId);
		model.addAttribute("user", client);
		model.addAttribute("genders", Gender.values());
		populateClientDetailModel(client, model);
		return "client-relations-officer/client-details";
	}
	
	private void populateClientDetailModel(Client client, Model model) {
		model.addAttribute("relatedConsultations", consultationService.findAllForClient(client));
		model.addAttribute("relatedCases", caseService.findAllForClient(client));
		model.addAttribute("relatedEvents", eventService.findAllForClient(client));
		model.addAttribute("relatedTransactions", transactionService.findAllForClient(client));
	}
	
	@PostMapping("/client-relations-officer/clients/{id}/edit")
	public String editClient(@PathVariable("id") Long userId,
							 @Valid @ModelAttribute EditProfileRequest form, BindingResult bindingResult,
							 Model model) {
		Client client = userService.findClientById(userId);
		
		if (bindingResult.hasErrors()) {
			model.addAttribute("error", bindingResult.getAllErrors().getFirst().getDefaultMessage());
			model.addAttribute("user", client);
			model.addAttribute("genders", Gender.values());
			populateClientDetailModel(client, model);
			return "client-relations-officer/client-details";
		}
		
		try {
			client = (Client) userService.updateDetails(client, form.getName(), form.getDateOfBirth(), form.getGender(),
														form.getAddresses(), form.getPhoneNumbers(), form.getNicFile(), null);
			model.addAttribute("success", "Client updated successfully.");
		} catch (NonPdfDocumentException e) {
			model.addAttribute("error", e.getMessage());
		}
		
		model.addAttribute("user", client);
		model.addAttribute("genders", Gender.values());
		populateClientDetailModel(client, model);
		return "client-relations-officer/client-details";
	}
	
	@PostMapping("/client-relations-officer/clients/{id}/reset-password")
	public String resetClientPassword(@PathVariable("id") Long userId,
									  @Valid @ModelAttribute ResetPasswordRequest form, BindingResult bindingResult,
									  Model model) {
		Client client = userService.findClientById(userId);
		
		if (bindingResult.hasErrors()) {
			model.addAttribute("error", bindingResult.getAllErrors().getFirst().getDefaultMessage());
			model.addAttribute("user", client);
			model.addAttribute("genders", Gender.values());
			populateClientDetailModel(client, model);
			return "client-relations-officer/client-details";
		}
		
		client = (Client) userService.changePassword(client, form.getTemporaryPassword());
		
		model.addAttribute("user", client);
		model.addAttribute("genders", Gender.values());
		populateClientDetailModel(client, model);
		model.addAttribute("success", "Password reset successfully.");
		return "client-relations-officer/client-details";
	}
	
	@PostMapping("/client-relations-officer/clients/{id}/delete")
	public String deleteClient(@PathVariable("id") Long userId, Model model, RedirectAttributes redirectAttributes) {
		Client client = userService.findClientById(userId);
		
		try {
			userService.delete(client);
		} catch (BusinessRuleViolationException e) {
			model.addAttribute("user", client);
			model.addAttribute("genders", Gender.values());
			populateClientDetailModel(client, model);
			model.addAttribute("error", e.getMessage());
			return "client-relations-officer/client-details";
		}
		
		redirectAttributes.addFlashAttribute("success", "Client deleted successfully.");
		return "redirect:/client-relations-officer/clients";
	}
	
	@PostMapping("/client-relations-officer/clients/{id}/reactivate")
	public String reactivateClient(@PathVariable("id") Long userId,
								   @Valid @ModelAttribute ReactivateAccountRequest form, BindingResult bindingResult,
								   Model model) {
		Client client = userService.findClientById(userId);
		
		if (bindingResult.hasErrors()) {
			model.addAttribute("error", bindingResult.getAllErrors().getFirst().getDefaultMessage());
			model.addAttribute("user", client);
			model.addAttribute("genders", Gender.values());
			populateClientDetailModel(client, model);
			return "client-relations-officer/client-details";
		}
		
		try {
			client = (Client) userService.reactivate(client, form.getTemporaryPassword());
			model.addAttribute("success", "Client account reactivated. Give the client the temporary password so they can log in and change it.");
		} catch (InvalidStateException | BusinessRuleViolationException e) {
			model.addAttribute("error", e.getMessage());
		}
		
		model.addAttribute("user", client);
		model.addAttribute("genders", Gender.values());
		populateClientDetailModel(client, model);
		return "client-relations-officer/client-details";
	}
	
	@GetMapping("/client-relations-officer/consultations/{id}")
	public String consultationDetails(@PathVariable("id") Long consultationId, Model model) {
		populateConsultationDetailModel(consultationService.findById(consultationId), model);
		return "client-relations-officer/consultation-details";
	}
	
	// ==================== Helpers ====================
	
	private void populateConsultationDetailModel(Consultation consultation, Model model) {
		model.addAttribute("consultation", consultation);
		model.addAttribute("consultationDocuments", documentService.findConsultationDocuments(consultation));
		model.addAttribute("caseTypes", CaseType.values());
	}
	
	@PostMapping("/client-relations-officer/consultations/{id}/edit")
	public String editConsultation(@PathVariable("id") Long consultationId, @Valid @ModelAttribute EditConsultationRequest form, BindingResult bindingResult, Model model) {
		Consultation consultation = consultationService.findById(consultationId);
		
		if (bindingResult.hasErrors()) {
			model.addAttribute("error", bindingResult.getAllErrors().getFirst().getDefaultMessage());
			populateConsultationDetailModel(consultation, model);
			return "client-relations-officer/consultation-details";
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
		
		return "client-relations-officer/consultation-details";
	}
	
	@PostMapping("/client-relations-officer/consultations/{id}/delete")
	public String deleteConsultation(@PathVariable("id") Long consultationId, RedirectAttributes redirectAttributes) {
		Consultation consultation = consultationService.findById(consultationId);
		
		try {
			consultationService.delete(consultation);
		} catch (InvalidConsultationStateException e) {
			redirectAttributes.addFlashAttribute("error", e.getMessage());
			return "redirect:/client-relations-officer/consultations/" + consultationId;
		}
		
		redirectAttributes.addFlashAttribute("success", "Consultation request deleted.");
		return "redirect:/client-relations-officer/consultations";
	}
	
	// ==================== Documents ====================
	
	@GetMapping("/client-relations-officer/documents/consultation/{id}")
	public ResponseEntity<Resource> viewConsultationDocument(@PathVariable("id") Long documentId, Authentication authentication) {
		return documentService.streamConsultationDocument(documentService.findById(documentId), currentClientRelationsOfficer(authentication));
	}
	
	// ==================== Schedule (Consultation Events) ====================
	
	@GetMapping("/client-relations-officer/events")
	public String events(Model model) {
		model.addAttribute("events", eventService.findAllConsultationEvents());
		return "client-relations-officer/event-list";
	}
	
	@GetMapping("/client-relations-officer/events/{id}")
	public String eventDetails(@PathVariable("id") Long eventId, Model model) {
		model.addAttribute("event", eventService.findById(eventId));
		return "client-relations-officer/event-details";
	}
	
}
