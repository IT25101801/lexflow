package lexflow.controller;

import lexflow.dto.user.*;
import lexflow.entity.user.*;
import lexflow.security.CustomUserPrincipal;
import lexflow.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;


@Controller
@RequiredArgsConstructor
public class PartnerController {
	
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
	
}
