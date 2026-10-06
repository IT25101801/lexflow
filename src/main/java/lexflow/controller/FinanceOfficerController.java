package lexflow.controller;

import lexflow.util.ValidationUtil;
import jakarta.validation.Valid;
import lexflow.dto.finance.CompileInvoiceRequest;
import lexflow.dto.finance.EditBillableAmountRequest;
import lexflow.dto.finance.TopUpByCashRequest;
import lexflow.dto.finance.WithdrawRequest;
import lexflow.dto.user.ChangePasswordRequest;
import lexflow.entity.case_.Case;
import lexflow.entity.finance.InvoiceStatus;
import lexflow.entity.finance.TransactionType;
import lexflow.entity.user.Client;
import lexflow.entity.user.FinanceOfficer;
import lexflow.entity.user.Role;
import lexflow.entity.user.User;
import lexflow.exception.BusinessRuleViolationException;
import lexflow.exception.EntityNotFoundException;
import lexflow.exception.PersistenceException;
import lexflow.exception.finance.InsufficientFundsException;
import lexflow.exception.finance.InvalidInvoiceStateException;
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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class FinanceOfficerController {
	
	private final UserService userService;
	private final CaseService caseService;
	private final InvoiceService invoiceService;
	private final TransactionService transactionService;
	private final DocumentService documentService;
	private final TaskService taskService;
	private final AnalyticsService analyticsService;
	
	// Overview
	
	@GetMapping("/finance-officer/dashboard")
	public String dashboard(Authentication authentication, Model model) {
		model.addAttribute("user", currentFinanceOfficer(authentication));
		
		return "finance-officer/dashboard";
	}
	
	@GetMapping("/finance-officer/analytics")
	public String analytics(Authentication authentication, Model model) {
		model.addAttribute("user", currentFinanceOfficer(authentication));
		
		Map<Integer, BigDecimal> revenueByMonth = analyticsService.revenueByMonth(LocalDate.now().getYear());
		model.addAttribute("revenueByMonthLabels", monthLabels());
		model.addAttribute("revenueByMonthValues", revenueByMonth.values());
		
		model.addAttribute("outstandingInvoiceTotal", analyticsService.outstandingInvoiceTotal());
		model.addAttribute("averageDaysToPayment", analyticsService.averageDaysToPayment());
		
		Map<TransactionType, Integer> topUpCountComparison = analyticsService.topUpCountComparison();
		model.addAttribute("cardTopUpCount", topUpCountComparison.getOrDefault(TransactionType.CARD_TOP_UP, 0));
		model.addAttribute("cashTopUpCount", topUpCountComparison.getOrDefault(TransactionType.CASH_TOP_UP, 0));
		
		model.addAttribute("totalClientWalletFloat", analyticsService.totalClientWalletFloat());
		
		return "finance-officer/analytics";
	}
	
	@GetMapping("/finance-officer/profile")
	public String profile(Authentication authentication, Model model) {
		model.addAttribute("user", currentFinanceOfficer(authentication));
		
		return "finance-officer/profile";
	}
	
	@PostMapping("/finance-officer/profile/change-password")
	public String changePassword(Authentication authentication, @Valid @ModelAttribute ChangePasswordRequest form, BindingResult bindingResult, Model model, RedirectAttributes redirectAttributes) {
		User user = currentFinanceOfficer(authentication);
		
		if (bindingResult.hasErrors()) {
			model.addAttribute("error", ValidationUtil.allErrors(bindingResult));
			model.addAttribute("user", user);
			
			return "finance-officer/profile";
		}
		
		if (!userService.verifyPassword(user, form.getCurrentPassword())) {
			model.addAttribute("user", user);
			model.addAttribute("error", "Current password is incorrect.");
			
			return "finance-officer/profile";
		}
		
		userService.changePassword(user, form.getNewPassword());
		
		redirectAttributes.addFlashAttribute("success", "Password changed successfully.");
		
		return "redirect:/finance-officer/profile";
	}
	
	// Cases
	
	@GetMapping("/finance-officer/cases/open")
	public String openCases(Model model) {
		model.addAttribute("cases", caseService.findAllOpen());
		model.addAttribute("title", "Open Cases");
		model.addAttribute("active", "cases-open");
		
		return "finance-officer/case-list";
	}
	
	@GetMapping("/finance-officer/cases/closed")
	public String closedCases(Model model) {
		model.addAttribute("cases", caseService.findAllClosed());
		model.addAttribute("title", "Closed Cases");
		model.addAttribute("active", "cases-closed");
		
		return "finance-officer/case-list";
	}
	
	@GetMapping("/finance-officer/cases/{id}")
	public String caseDetails(@PathVariable("id") Long caseId, Model model) {
		populateCaseDetailModel(caseService.findById(caseId), model);
		
		return "finance-officer/case-details";
	}
	
	@PostMapping("/finance-officer/cases/{id}/tasks/{taskId}/edit-billable-amount")
	public String editBillableAmount(@PathVariable("id") Long caseId, @PathVariable Long taskId, @Valid @ModelAttribute EditBillableAmountRequest form, BindingResult bindingResult, Model model, RedirectAttributes redirectAttributes) {
		Case case_ = caseService.findById(caseId);
		
		if (bindingResult.hasErrors()) {
			model.addAttribute("error", ValidationUtil.allErrors(bindingResult));
			populateCaseDetailModel(case_, model);
			
			return "finance-officer/case-details";
		}
		
		try {
			taskService.updateBillableAmount(taskService.findById(taskId), form.getAmount());
		} catch (BusinessRuleViolationException e) {
			populateCaseDetailModel(case_, model);
			model.addAttribute("error", e.getMessage());
			
			return "finance-officer/case-details";
		}
		
		redirectAttributes.addFlashAttribute("success", "Billable amount updated successfully.");
		
		return "redirect:/finance-officer/cases/{id}";
	}
	
	@PostMapping("/finance-officer/cases/{id}/invoices/compile")
	public String compileInvoice(@PathVariable("id") Long caseId, @Valid @ModelAttribute CompileInvoiceRequest form, Model model, RedirectAttributes redirectAttributes) {
		Case case_ = caseService.findById(caseId);
		
		try {
			invoiceService.compile(case_, form.getTaskIds());
		} catch (BusinessRuleViolationException e) {
			populateCaseDetailModel(case_, model);
			model.addAttribute("error", e.getMessage());
			
			return "finance-officer/case-details";
		}
		
		redirectAttributes.addFlashAttribute("success", "Invoice compiled successfully.");
		
		return "redirect:/finance-officer/cases/{id}";
	}
	
	@PostMapping("/finance-officer/cases/{id}/invoices/{invoiceId}/charge")
	public String chargeInvoice(@PathVariable("id") Long caseId, @PathVariable Long invoiceId, Model model, RedirectAttributes redirectAttributes) {
		Case case_ = caseService.findById(caseId);
		
		try {
			invoiceService.charge(case_, invoiceService.findById(invoiceId));
		} catch (InvalidInvoiceStateException | InsufficientFundsException e) {
			populateCaseDetailModel(case_, model);
			model.addAttribute("error", e.getMessage());
			
			return "finance-officer/case-details";
		}
		
		redirectAttributes.addFlashAttribute("success", "Invoice charged successfully.");
		
		return "redirect:/finance-officer/cases/{id}";
	}
	
	@PostMapping("/finance-officer/cases/{id}/invoices/{invoiceId}/void")
	public String voidInvoice(@PathVariable("id") Long caseId, @PathVariable Long invoiceId, Model model, RedirectAttributes redirectAttributes) {
		Case case_ = caseService.findById(caseId);
		
		try {
			invoiceService.void_(invoiceService.findById(invoiceId));
		} catch (InvalidInvoiceStateException e) {
			populateCaseDetailModel(case_, model);
			model.addAttribute("error", e.getMessage());
			
			return "finance-officer/case-details";
		}
		
		redirectAttributes.addFlashAttribute("success", "Invoice voided successfully.");
		
		return "redirect:/finance-officer/cases/{id}";
	}
	
	@PostMapping("/finance-officer/cases/{id}/invoices/{invoiceId}/void-refund")
	public String voidAndRefundInvoice(@PathVariable("id") Long caseId, @PathVariable Long invoiceId, Model model, RedirectAttributes redirectAttributes) {
		Case case_ = caseService.findById(caseId);
		
		try {
			invoiceService.voidAndRefund(case_, invoiceService.findById(invoiceId));
		} catch (InvalidInvoiceStateException e) {
			populateCaseDetailModel(case_, model);
			model.addAttribute("error", e.getMessage());
			
			return "finance-officer/case-details";
		}
		
		redirectAttributes.addFlashAttribute("success", "Invoice voided and refunded successfully.");
		
		return "redirect:/finance-officer/cases/{id}";
	}
	
	// Clients
	
	@GetMapping("/finance-officer/clients")
	public String clients(@RequestParam(required = false) String keyword, Model model) {
		model.addAttribute("users", userService.findByRoleAndKeyword(Role.CLIENT, keyword));
		model.addAttribute("keyword", keyword);
		model.addAttribute("title", "Clients");
		model.addAttribute("active", "clients");
		
		return "finance-officer/client-list";
	}
	
	@GetMapping("/finance-officer/clients/{id}")
	public String clientDetails(@PathVariable("id") Long userId, Model model) {
		Client client = userService.findClientById(userId);
		
		model.addAttribute("user", client);
		model.addAttribute("relatedCases", caseService.findAllForClient(client));
		model.addAttribute("relatedTransactions", transactionService.findAllForClient(client));
		
		return "finance-officer/client-details";
	}
	
	// Payments
	
	@GetMapping("/finance-officer/cash-top-up")
	public String showCashTopUpForm(Model model) {
		return "finance-officer/cash-top-up";
	}
	
	@PostMapping("/finance-officer/cash-top-up")
	public String topUpWalletByCash(@Valid @ModelAttribute TopUpByCashRequest form, BindingResult bindingResult, Model model, RedirectAttributes redirectAttributes) {
		
		if (bindingResult.hasErrors()) {
			model.addAttribute("error", ValidationUtil.allErrors(bindingResult));
			
			return "finance-officer/cash-top-up";
		}
		
		Client client;
		
		try {
			client = userService.findClientByUserCode(form.getClientCode());
		} catch (EntityNotFoundException e) {
			model.addAttribute("error", "No client found with code " + form.getClientCode() + ".");
			
			return "finance-officer/cash-top-up";
		}
		
		try {
			transactionService.topUpByCash(client, form.getAmount());
		} catch (PersistenceException e) {
			model.addAttribute("error", "Could not top up the wallet. Please try again.");
			
			return "finance-officer/cash-top-up";
		}
		
		redirectAttributes.addFlashAttribute("success", "Wallet topped up successfully.");
		
		return "redirect:/finance-officer/cash-top-up";
	}
	
	@GetMapping("/finance-officer/withdrawal")
	public String showWithdrawalForm(Model model) {
		return "finance-officer/withdrawal";
	}
	
	@PostMapping("/finance-officer/withdrawal")
	public String withdrawFromWallet(@Valid @ModelAttribute WithdrawRequest form, BindingResult bindingResult, Model model, RedirectAttributes redirectAttributes) {
		
		if (bindingResult.hasErrors()) {
			model.addAttribute("error", ValidationUtil.allErrors(bindingResult));
			
			return "finance-officer/withdrawal";
		}
		
		Client client;
		
		try {
			client = userService.findClientByUserCode(form.getClientCode());
		} catch (EntityNotFoundException e) {
			model.addAttribute("error", "No client found with code " + form.getClientCode() + ".");
			
			return "finance-officer/withdrawal";
		}
		
		try {
			transactionService.withdraw(client, form.getAmount());
		} catch (InsufficientFundsException e) {
			model.addAttribute("error", e.getMessage());
			
			return "finance-officer/withdrawal";
		} catch (PersistenceException e) {
			model.addAttribute("error", "Could not record the withdrawal. Please try again.");
			
			return "finance-officer/withdrawal";
		}
		
		redirectAttributes.addFlashAttribute("success", "Withdrawal recorded successfully.");
		
		return "redirect:/finance-officer/withdrawal";
	}
	
	@GetMapping("/finance-officer/transactions")
	public String transactions(Model model) {
		model.addAttribute("transactions", transactionService.findAll());
		model.addAttribute("totalCharges", transactionService.totalCharges());
		model.addAttribute("totalRefunds", transactionService.totalRefunds());
		model.addAttribute("netRevenue", transactionService.netRevenue());
		
		return "finance-officer/transactions";
	}
	
	// Documents
	
	@GetMapping({"/finance-officer/documents/user/{id}", "/finance-officer/documents/user/{id}/{filename}"})
	public ResponseEntity<Resource> viewUserDocument(@PathVariable("id") Long documentId, Authentication authentication) {
		return documentService.streamUserDocument(documentService.findById(documentId), currentFinanceOfficer(authentication));
	}
	
	@GetMapping({"/finance-officer/documents/invoice/{id}", "/finance-officer/documents/invoice/{id}/{filename}"})
	public ResponseEntity<Resource> viewInvoiceDocument(@PathVariable("id") Long documentId, Authentication authentication) {
		return documentService.streamInvoiceDocument(documentService.findById(documentId), currentFinanceOfficer(authentication));
	}
	
	// Private Helpers
	
	private FinanceOfficer currentFinanceOfficer(Authentication authentication) {
		CustomUserPrincipal principal = (CustomUserPrincipal) authentication.getPrincipal();
		
		return (FinanceOfficer) principal.user();
	}
	
	private List<String> monthLabels() {
		return List.of("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec");
	}
	
	private void populateCaseDetailModel(Case case_, Model model) {
		model.addAttribute("case", case_);
		model.addAttribute("tasks", taskService.findByCase(case_));
		model.addAttribute("unbilledTasks", taskService.findUnbilledByCase(case_));
		model.addAttribute("unpaidInvoices", invoiceService.findByCaseAndStatus(case_, InvoiceStatus.UNPAID));
		model.addAttribute("paidInvoices", invoiceService.findByCaseAndStatus(case_, InvoiceStatus.PAID));
		model.addAttribute("voidedInvoices", invoiceService.findByCaseAndStatus(case_, InvoiceStatus.VOIDED));
	}
	
}
