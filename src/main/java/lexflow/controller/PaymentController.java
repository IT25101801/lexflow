package lexflow.controller;

import jakarta.validation.Valid;
import lexflow.dto.finance.FillCardDetailsRequest;
import lexflow.entity.user.Client;
import lexflow.exception.PersistenceException;
import lexflow.security.CustomUserPrincipal;
import lexflow.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;

@Controller
@RequiredArgsConstructor
public class PaymentController {
	
	private final TransactionService transactionService;
	
	@GetMapping("/payment/portal")
	public String showPaymentPortal(@RequestParam BigDecimal amount, Model model) {
		model.addAttribute("amount", amount);
		return "payment/portal";
	}
	
	@PostMapping("/payment/process")
	public String fillCardDetails(@Valid @ModelAttribute FillCardDetailsRequest form, BindingResult bindingResult, Authentication authentication, Model model) {
		
		if (bindingResult.hasErrors()) {
			model.addAttribute("amount", form.getAmount());
			model.addAttribute("error", "Invalid card details. Please check and try again.");
			return "payment/portal";
		}
		
		Client client = currentClient(authentication);
		
		try {
			transactionService.topUpByCard(client, form.getAmount());
		} catch (PersistenceException e) {
			model.addAttribute("amount", form.getAmount());
			model.addAttribute("error", "The payment could not be processed. Please try again.");
			return "payment/portal";
		}
		
		model.addAttribute("client", client);
		model.addAttribute("transactions", transactionService.findAllForClient(client));
		model.addAttribute("success", "Trust account topped up successfully.");
		return "client/transactions";
	}
	
	private Client currentClient(Authentication authentication) {
		CustomUserPrincipal principal = (CustomUserPrincipal) authentication.getPrincipal();
		return (Client) principal.user();
	}
	
}
