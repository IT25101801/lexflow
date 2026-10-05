package lexflow.controller;

import jakarta.validation.Valid;
import lexflow.dto.user.ChangePasswordRequest;
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
	
	private final UserService userService;
	
	private final DocumentService documentService;
	
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
	
}
