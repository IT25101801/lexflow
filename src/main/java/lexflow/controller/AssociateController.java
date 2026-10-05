package lexflow.controller;

import jakarta.validation.Valid;
import lexflow.dto.user.ChangePasswordRequest;
import lexflow.entity.user.Associate;
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
public class AssociateController {
	
	private final DocumentService documentService;
	
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
	
}
