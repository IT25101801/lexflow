package lexflow.controller;

import jakarta.validation.Valid;
import lexflow.dto.user.*;
import lexflow.entity.user.Role;
import lexflow.entity.user.*;
import lexflow.exception.BusinessRuleViolationException;
import lexflow.exception.EntityAlreadyExistsException;
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
	
	private final ConsultationService consultationService;
	
	private final ConflictCheckService conflictCheckService;
	
	private final DocumentService documentService;
	
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
	
}
