package lexflow.controller;

import jakarta.validation.Valid;
import lexflow.dto.finance.FillCardDetailsRequest;
import lexflow.entity.user.Client;
import lexflow.exception.PersistenceException;
import lexflow.security.CustomUserPrincipal;
import lexflow.util.ValidationUtil;
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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

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
	public String fillCardDetails(@Valid @ModelAttribute FillCardDetailsRequest form, BindingResult bindingResult, Authentication authentication, Model model, RedirectAttributes redirectAttributes) {
		
		if (bindingResult.hasErrors()) {
			model.addAttribute("amount", form.getAmount());
			model.addAttribute("error", ValidationUtil.allErrors(bindingResult));
			
			return "payment/portal";
		}
		
		try {
			transactionService.topUpByCard(currentClient(authentication), form.getAmount());
		} catch (PersistenceException e) {
			model.addAttribute("amount", form.getAmount());
			model.addAttribute("error", "Could not process the payment. Please try again.");
			
			return "payment/portal";
		}
		
		redirectAttributes.addFlashAttribute("success", "Wallet topped up successfully.");
		
		return "redirect:/client/transactions";
	}
	
	private Client currentClient(Authentication authentication) {
		CustomUserPrincipal principal = (CustomUserPrincipal) authentication.getPrincipal();
		
		return (Client) principal.user();
	}
	
}
