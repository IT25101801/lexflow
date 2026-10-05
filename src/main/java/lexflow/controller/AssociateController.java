package lexflow.controller;

import lexflow.entity.user.Associate;
import lexflow.security.CustomUserPrincipal;
import lexflow.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;


@Controller
@RequiredArgsConstructor
public class AssociateController {
	
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
	
}
