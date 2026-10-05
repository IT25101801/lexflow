package lexflow.controller;

import jakarta.validation.Valid;
import lexflow.dto.user.*;
import lexflow.entity.user.*;
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


@Controller
@RequiredArgsConstructor
public class PartnerController {
	
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
	
}
