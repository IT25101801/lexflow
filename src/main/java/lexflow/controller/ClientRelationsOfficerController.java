package lexflow.controller;

import jakarta.validation.Valid;
import lexflow.dto.consultation.EditConsultationRequest;
import lexflow.dto.consultation.SubmitWalkInConsultationRequest;
import lexflow.dto.user.*;
import lexflow.entity.consultation.CaseType;
import lexflow.entity.consultation.Consultation;
import lexflow.entity.user.*;
import lexflow.exception.*;
import lexflow.exception.consultation.InvalidConsultationStateException;
import lexflow.exception.document.NonPdfDocumentException;
import lexflow.security.CustomUserPrincipal;
import lexflow.service.*;
import lexflow.util.ValidationUtil;
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
	
	// Overview
	
	@GetMapping("/client-relations-officer/dashboard")
	public String dashboard(Authentication authentication, Model model) {
		model.addAttribute("user", currentClientRelationsOfficer(authentication));
		
		return "client-relations-officer/dashboard";
	}
	
	@GetMapping("/client-relations-officer/profile")
	public String profile(Authentication authentication, Model model) {
		model.addAttribute("user", currentClientRelationsOfficer(authentication));
		
		return "client-relations-officer/profile";
	}
	
	@PostMapping("/client-relations-officer/profile/change-password")
	public String changePassword(Authentication authentication, @Valid @ModelAttribute ChangePasswordRequest form, BindingResult bindingResult, Model model, RedirectAttributes redirectAttributes) {
		User user = currentClientRelationsOfficer(authentication);
		
		if (bindingResult.hasErrors()) {
			model.addAttribute("error", ValidationUtil.allErrors(bindingResult));
			model.addAttribute("user", user);
			
			return "client-relations-officer/profile";
		}
		
		if (!userService.verifyPassword(user, form.getCurrentPassword())) {
			model.addAttribute("user", user);
			model.addAttribute("error", "Current password is incorrect.");
			
			return "client-relations-officer/profile";
		}
		
		userService.changePassword(user, form.getNewPassword());
		
		redirectAttributes.addFlashAttribute("success", "Password changed successfully.");
		
		return "redirect:/client-relations-officer/profile";
	}
	
	// Clients
	
	@GetMapping("/client-relations-officer/register-client")
	public String showRegisterClientForm(Model model) {
		model.addAttribute("genders", Gender.values());
		
		return "client-relations-officer/register-client";
	}
	
	@PostMapping("/client-relations-officer/register-client")
	public String registerClient(@Valid @ModelAttribute RegisterClientRequest form, BindingResult bindingResult, Model model, RedirectAttributes redirectAttributes) {
		
		if (bindingResult.hasErrors()) {
			model.addAttribute("error", ValidationUtil.allErrors(bindingResult));
			model.addAttribute("genders", Gender.values());
			
			return "client-relations-officer/register-client";
		}
		
		Client client;
		
		try {
			client = userService.register(form.getEmail(), form.getPassword(), form.getName(), form.getDateOfBirth(), form.getGender(), form.getAddresses(), form.getPhoneNumbers(), form.getNicNumber(), form.getNicFile());
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
		
		return "redirect:/client-relations-officer/clients/" + client.getUserId();
	}
	
	@GetMapping("/client-relations-officer/clients")
	public String clients(@RequestParam(required = false) String keyword, Model model) {
		model.addAttribute("users", userService.findByRoleAndKeyword(Role.CLIENT, keyword));
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
	public String reactivateClient(@PathVariable("id") Long userId, @Valid @ModelAttribute ReactivateAccountRequest form, BindingResult bindingResult, Model model, RedirectAttributes redirectAttributes) {
		Client client = userService.findClientById(userId);
		
		if (bindingResult.hasErrors()) {
			model.addAttribute("error", ValidationUtil.allErrors(bindingResult));
			model.addAttribute("user", client);
			model.addAttribute("genders", Gender.values());
			populateClientDetailModel(client, model);
			
			return "client-relations-officer/client-details";
		}
		
		try {
			userService.reactivate(client, form.getTemporaryPassword());
		} catch (InvalidStateException | BusinessRuleViolationException e) {
			model.addAttribute("error", e.getMessage());
			model.addAttribute("user", client);
			model.addAttribute("genders", Gender.values());
			populateClientDetailModel(client, model);
			
			return "client-relations-officer/client-details";
		}
		
		redirectAttributes.addFlashAttribute("success", "Account reactivated successfully.");
		
		return "redirect:/client-relations-officer/clients/{id}";
	}
	
	@PostMapping("/client-relations-officer/clients/{id}/reset-password")
	public String resetClientPassword(@PathVariable("id") Long userId, @Valid @ModelAttribute ResetPasswordRequest form, BindingResult bindingResult, Model model, RedirectAttributes redirectAttributes) {
		Client client = userService.findClientById(userId);
		
		if (bindingResult.hasErrors()) {
			model.addAttribute("error", ValidationUtil.allErrors(bindingResult));
			model.addAttribute("user", client);
			model.addAttribute("genders", Gender.values());
			populateClientDetailModel(client, model);
			
			return "client-relations-officer/client-details";
		}
		
		userService.changePassword(client, form.getTemporaryPassword());
		
		redirectAttributes.addFlashAttribute("success", "Password reset successfully.");
		
		return "redirect:/client-relations-officer/clients/{id}";
	}
	
	@PostMapping("/client-relations-officer/clients/{id}/edit")
	public String editClient(@PathVariable("id") Long userId, @Valid @ModelAttribute EditProfileRequest form, BindingResult bindingResult, Model model, RedirectAttributes redirectAttributes) {
		Client client = userService.findClientById(userId);
		
		if (bindingResult.hasErrors()) {
			model.addAttribute("error", ValidationUtil.allErrors(bindingResult));
			model.addAttribute("user", client);
			model.addAttribute("genders", Gender.values());
			populateClientDetailModel(client, model);
			
			return "client-relations-officer/client-details";
		}
		
		try {
			userService.updateDetails(client, form.getName(), form.getDateOfBirth(), form.getGender(), form.getAddresses(), form.getPhoneNumbers(), form.getNicFile(), null);
		} catch (NonPdfDocumentException e) {
			model.addAttribute("error", e.getMessage());
			model.addAttribute("user", client);
			model.addAttribute("genders", Gender.values());
			populateClientDetailModel(client, model);
			
			return "client-relations-officer/client-details";
		}
		
		redirectAttributes.addFlashAttribute("success", "Client updated successfully.");
		
		return "redirect:/client-relations-officer/clients/{id}";
	}
	
	// Consultations
	
	@GetMapping("/client-relations-officer/consultations/new")
	public String showConsultationForm(Model model) {
		model.addAttribute("caseTypes", CaseType.values());
		
		return "client-relations-officer/consultation-form";
	}
	
	@PostMapping("/client-relations-officer/consultations")
	public String submitConsultation(@Valid @ModelAttribute SubmitWalkInConsultationRequest form, BindingResult bindingResult, Model model, RedirectAttributes redirectAttributes) {
		
		if (bindingResult.hasErrors()) {
			model.addAttribute("error", ValidationUtil.allErrors(bindingResult));
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
		
		Consultation consultation;
		
		try {
			consultation = consultationService.submit(client, form.getCaseType(), form.getOpposingParties(), form.getDescription(), consultationFiles);
		} catch (BusinessRuleViolationException e) {
			model.addAttribute("caseTypes", CaseType.values());
			model.addAttribute("error", e.getMessage());
			
			return "client-relations-officer/consultation-form";
		} catch (PersistenceException e) {
			model.addAttribute("caseTypes", CaseType.values());
			model.addAttribute("error", "Could not submit the consultation. Please try again.");
			
			return "client-relations-officer/consultation-form";
		}
		
		redirectAttributes.addFlashAttribute("success", "Consultation submitted successfully.");
		
		return "redirect:/client-relations-officer/consultations/" + consultation.getConsultationId();
	}
	
	@GetMapping("/client-relations-officer/consultations")
	public String consultations(@RequestParam(required = false) String keyword, Model model) {
		model.addAttribute("consultations", consultationService.findByKeyword(keyword));
		model.addAttribute("keyword", keyword);
		
		return "client-relations-officer/consultation-list";
	}
	
	@GetMapping("/client-relations-officer/consultations/{id}")
	public String consultationDetails(@PathVariable("id") Long consultationId, Model model) {
		populateConsultationDetailModel(consultationService.findById(consultationId), model);
		
		return "client-relations-officer/consultation-details";
	}
	
	@PostMapping("/client-relations-officer/consultations/{id}/delete")
	public String deleteConsultation(@PathVariable("id") Long consultationId, Model model, RedirectAttributes redirectAttributes) {
		Consultation consultation = consultationService.findById(consultationId);
		
		try {
			consultationService.delete(consultation);
		} catch (InvalidConsultationStateException e) {
			model.addAttribute("error", e.getMessage());
			populateConsultationDetailModel(consultation, model);
			
			return "client-relations-officer/consultation-details";
		}
		
		redirectAttributes.addFlashAttribute("success", "Consultation deleted successfully.");
		
		return "redirect:/client-relations-officer/consultations";
	}
	
	@PostMapping("/client-relations-officer/consultations/{id}/edit")
	public String editConsultation(@PathVariable("id") Long consultationId, @Valid @ModelAttribute EditConsultationRequest form, BindingResult bindingResult, Model model, RedirectAttributes redirectAttributes) {
		Consultation consultation = consultationService.findById(consultationId);
		
		if (bindingResult.hasErrors()) {
			model.addAttribute("error", ValidationUtil.allErrors(bindingResult));
			populateConsultationDetailModel(consultation, model);
			
			return "client-relations-officer/consultation-details";
		}
		
		List<MultipartFile> consultationFiles = form.getConsultationFiles() != null ? form.getConsultationFiles() : List.of();
		
		try {
			consultationService.updateDetails(consultation, form.getCaseType(), form.getOpposingParties(), form.getDescription(), consultationFiles, form.getDeleteDocumentIds(), form.getDocumentIds(), form.getOriginalFilenames());
		} catch (InvalidConsultationStateException | BusinessRuleViolationException e) {
			populateConsultationDetailModel(consultation, model);
			model.addAttribute("error", e.getMessage());
			
			return "client-relations-officer/consultation-details";
		}
		
		redirectAttributes.addFlashAttribute("success", "Consultation updated successfully.");
		
		return "redirect:/client-relations-officer/consultations/{id}";
	}
	
	// Schedule
	
	@GetMapping("/client-relations-officer/events/upcoming")
	public String upcomingEvents(Model model) {
		model.addAttribute("events", eventService.upcoming(eventService.findAllForConsultations()));
		model.addAttribute("title", "Upcoming Events");
		model.addAttribute("active", "events-upcoming");
		
		return "client-relations-officer/event-list";
	}
	
	@GetMapping("/client-relations-officer/events/past")
	public String pastEvents(Model model) {
		model.addAttribute("events", eventService.past(eventService.findAllForConsultations()));
		model.addAttribute("title", "Past Events");
		model.addAttribute("active", "events-past");
		
		return "client-relations-officer/event-list";
	}
	
	@GetMapping("/client-relations-officer/events/{id}")
	public String eventDetails(@PathVariable("id") Long eventId, Model model) {
		model.addAttribute("event", eventService.findById(eventId));
		
		return "client-relations-officer/event-details";
	}
	
	// Documents
	
	@GetMapping({"/client-relations-officer/documents/user/{id}", "/client-relations-officer/documents/user/{id}/{filename}"})
	public ResponseEntity<Resource> viewUserDocument(@PathVariable("id") Long documentId, Authentication authentication) {
		return documentService.streamUserDocument(documentService.findById(documentId), currentClientRelationsOfficer(authentication));
	}
	
	@GetMapping({"/client-relations-officer/documents/consultation/{id}", "/client-relations-officer/documents/consultation/{id}/{filename}"})
	public ResponseEntity<Resource> viewConsultationDocument(@PathVariable("id") Long documentId, Authentication authentication) {
		return documentService.streamConsultationDocument(documentService.findById(documentId), currentClientRelationsOfficer(authentication));
	}
	
	// Private Helpers
	
	private ClientRelationsOfficer currentClientRelationsOfficer(Authentication authentication) {
		CustomUserPrincipal principal = (CustomUserPrincipal) authentication.getPrincipal();
		
		return (ClientRelationsOfficer) principal.user();
	}
	
	private void populateClientDetailModel(Client client, Model model) {
		model.addAttribute("relatedConsultations", consultationService.findAllForClient(client));
		model.addAttribute("relatedCases", caseService.findAllForClient(client));
		model.addAttribute("relatedEvents", eventService.findAllForClient(client));
		model.addAttribute("relatedTransactions", transactionService.findAllForClient(client));
	}
	
	private void populateConsultationDetailModel(Consultation consultation, Model model) {
		model.addAttribute("consultation", consultation);
		model.addAttribute("consultationDocuments", documentService.findByConsultation(consultation));
		model.addAttribute("caseTypes", CaseType.values());
	}
	
}
