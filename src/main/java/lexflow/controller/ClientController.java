package lexflow.controller;

import jakarta.validation.Valid;
import lexflow.dto.user.ChangePasswordRequest;
import lexflow.entity.user.Client;
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
public class ClientController {
	
	private final UserService userService;
	
	private final DocumentService documentService;
	
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
	
}
