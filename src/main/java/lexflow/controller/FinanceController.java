package lexflow.controller;

import jakarta.validation.Valid;
import lexflow.dto.user.ChangePasswordRequest;
import lexflow.entity.user.FinanceOfficer;
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
public class FinanceController {
	
	private final UserService userService;
	
	private final CaseService caseService;
	
	private final DocumentService documentService;
	
	// ==================== Overview (Dashboard, Profile) ====================
	
	@GetMapping("/finance/dashboard")
	public String dashboard(Authentication authentication, Model model) {
		model.addAttribute("user", currentFinanceOfficer(authentication));
		return "finance/dashboard";
	}
	
	private FinanceOfficer currentFinanceOfficer(Authentication authentication) {
		CustomUserPrincipal principal = (CustomUserPrincipal) authentication.getPrincipal();
		return (FinanceOfficer) principal.user();
	}
	
	@GetMapping("/finance/profile")
	public String profile(Authentication authentication, Model model) {
		model.addAttribute("user", currentFinanceOfficer(authentication));
		return "finance/profile";
	}
	
	@PostMapping("/finance/profile/change-password")
	public String changePassword(Authentication authentication,
								 @Valid @ModelAttribute ChangePasswordRequest form, BindingResult bindingResult,
								 Model model) {
		User user = currentFinanceOfficer(authentication);
		
		if (bindingResult.hasErrors()) {
			model.addAttribute("error", bindingResult.getAllErrors().getFirst().getDefaultMessage());
			model.addAttribute("user", user);
			return "finance/profile";
		}
		
		if (!userService.verifyPassword(user, form.getCurrentPassword())) {
			model.addAttribute("user", user);
			model.addAttribute("error", "Current password is incorrect.");
			return "finance/profile";
		}
		
		userService.changePassword(user, form.getNewPassword());
		
		model.addAttribute("user", user);
		model.addAttribute("success", "Password changed successfully.");
		return "finance/profile";
	}
	
	@GetMapping("/finance/documents/user/{id}")
	public ResponseEntity<Resource> viewUserDocument(@PathVariable("id") Long documentId, Authentication authentication) {
		return documentService.streamUserDocument(documentService.findById(documentId), currentFinanceOfficer(authentication));
	}
	
	// ==================== Cases (Open, Closed, Details) ====================
	
	@GetMapping("/finance/cases/open")
	public String openCases(Model model) {
		model.addAttribute("cases", caseService.findAllOpen());
		model.addAttribute("title", "Open Cases");
		model.addAttribute("active", "cases-open");
		return "finance/case-list";
	}
	
	@GetMapping("/finance/cases/closed")
	public String closedCases(Model model) {
		model.addAttribute("cases", caseService.findAllClosed());
		model.addAttribute("title", "Closed Cases");
		model.addAttribute("active", "cases-closed");
		return "finance/case-list";
	}
	
}
