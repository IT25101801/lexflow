package lexflow.controller;

import lexflow.entity.user.Paralegal;
import lexflow.security.CustomUserPrincipal;
import lexflow.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;


@Controller
@RequiredArgsConstructor
public class ParalegalController {
	
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
	
}
