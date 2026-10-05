package lexflow.controller;

import jakarta.validation.Valid;
import lexflow.dto.user.*;
import lexflow.entity.user.ClientRelationsOfficer;
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


@Controller
@RequiredArgsConstructor
public class ClientRelationsOfficerController {
	
	private final UserService userService;
	
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
	
}
