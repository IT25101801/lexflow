package lexflow.controller;

import lexflow.entity.user.Client;
import lexflow.security.CustomUserPrincipal;
import lexflow.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;


@Controller
@RequiredArgsConstructor
public class ClientController {
	
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
	
}
