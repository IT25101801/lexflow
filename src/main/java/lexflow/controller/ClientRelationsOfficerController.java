package lexflow.controller;

import lexflow.dto.user.*;
import lexflow.entity.user.ClientRelationsOfficer;
import lexflow.exception.*;
import lexflow.security.CustomUserPrincipal;
import lexflow.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;


@Controller
@RequiredArgsConstructor
public class ClientRelationsOfficerController {
	
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
	
}
