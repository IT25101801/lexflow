package lexflow.controller;

import lexflow.entity.user.FinanceOfficer;
import lexflow.security.CustomUserPrincipal;
import lexflow.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;


@Controller
@RequiredArgsConstructor
public class FinanceController {
	
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
	
}
