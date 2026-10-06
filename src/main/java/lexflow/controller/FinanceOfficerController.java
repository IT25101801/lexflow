package lexflow.controller;

import jakarta.validation.Valid;
import lexflow.dto.finance.CompileInvoiceRequest;
import lexflow.dto.finance.EditBillableAmountRequest;
import lexflow.dto.finance.TopUpByCashRequest;
import lexflow.dto.finance.WithdrawRequest;
import lexflow.dto.user.ChangePasswordRequest;
import lexflow.entity.case_.Case;
import lexflow.entity.finance.InvoiceStatus;
import lexflow.entity.finance.TransactionType;
import lexflow.entity.user.Role;
import lexflow.entity.user.Client;
import lexflow.entity.user.FinanceOfficer;
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
	
	// ==================== Overview (Dashboard, Profile) ====================
	
	@GetMapping("/finance-officer/dashboard")
	public String dashboard(Authentication authentication, Model model) {
		model.addAttribute("user", currentFinanceOfficer(authentication));
		return "finance-officer/dashboard";
	}
	
	private FinanceOfficer currentFinanceOfficer(Authentication authentication) {
		CustomUserPrincipal principal = (CustomUserPrincipal) authentication.getPrincipal();
		return (FinanceOfficer) principal.user();
	}
	
	@GetMapping("/finance-officer/profile")
	public String profile(Authentication authentication, Model model) {
		model.addAttribute("user", currentFinanceOfficer(authentication));
		return "finance-officer/profile";
	}
	
	@PostMapping("/finance-officer/profile/change-password")
	public String changePassword(Authentication authentication,
								 @Valid @ModelAttribute ChangePasswordRequest form, BindingResult bindingResult,
								 Model model) {
		User user = currentFinanceOfficer(authentication);
		
		if (bindingResult.hasErrors()) {
			model.addAttribute("error", bindingResult.getAllErrors().getFirst().getDefaultMessage());
			model.addAttribute("user", user);
			return "finance-officer/profile";
		}
		
		if (!userService.verifyPassword(user, form.getCurrentPassword())) {
			model.addAttribute("user", user);
			model.addAttribute("error", "Current password is incorrect.");
			return "finance-officer/profile";
		}
		
		userService.changePassword(user, form.getNewPassword());
		
		model.addAttribute("user", user);
		model.addAttribute("success", "Password changed successfully.");
		return "finance-officer/profile";
	}
	
	@GetMapping("/finance-officer/documents/user/{id}")
	public ResponseEntity<Resource> viewUserDocument(@PathVariable("id") Long documentId, Authentication authentication) {
		return documentService.streamUserDocument(documentService.findById(documentId), currentFinanceOfficer(authentication));
	}
	
	// ==================== Cases (Open, Closed, Details) ====================
	
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
		Case case_ = caseService.findById(caseId);
		populateCaseDetailModel(case_, model);
		return "finance-officer/case-details";
	}
	
	// ==================== Helpers ====================
	
	private void populateCaseDetailModel(Case case_, Model model) {
		model.addAttribute("case", case_);
		model.addAttribute("tasks", taskService.findByCase(case_));
		model.addAttribute("unbilledCharges", taskService.findUnbilledByCase(case_));
		model.addAttribute("unpaidInvoices", invoiceService.findByCaseAndStatus(case_, InvoiceStatus.UNPAID));
		model.addAttribute("paidInvoices", invoiceService.findByCaseAndStatus(case_, InvoiceStatus.PAID));
		model.addAttribute("voidedInvoices", invoiceService.findByCaseAndStatus(case_, InvoiceStatus.VOIDED));
	}
	
	// ==================== Invoices & Documents (case-details sub-actions) ====================
	
	@PostMapping("/finance-officer/cases/{id}/invoices/compile")
	public String compileInvoice(@PathVariable("id") Long caseId, @Valid @ModelAttribute CompileInvoiceRequest form, Model model) {
		Case case_ = caseService.findById(caseId);
		
		try {
			invoiceService.compile(case_, form.getTaskIds());
			populateCaseDetailModel(case_, model);
			model.addAttribute("success", "Invoice compiled successfully.");
		} catch (BusinessRuleViolationException e) {
			populateCaseDetailModel(case_, model);
			model.addAttribute("error", e.getMessage());
		}
		
		return "finance-officer/case-details";
	}
	
	@PostMapping("/finance-officer/cases/{id}/invoices/{invoiceId}/charge")
	public String chargeInvoice(@PathVariable("id") Long caseId, @PathVariable Long invoiceId, Model model) {
		Case case_ = caseService.findById(caseId);
		
		try {
			invoiceService.chargeInvoice(case_, invoiceService.findById(invoiceId));
			populateCaseDetailModel(case_, model);
			model.addAttribute("success", "Invoice charged successfully.");
		} catch (InvalidInvoiceStateException | InsufficientFundsException e) {
			populateCaseDetailModel(case_, model);
			model.addAttribute("error", e.getMessage());
		}
		
		return "finance-officer/case-details";
	}
	
	@PostMapping("/finance-officer/cases/{id}/tasks/{taskId}/edit-billable-amount")
	public String editBillableAmount(@PathVariable("id") Long caseId, @PathVariable Long taskId, @Valid @ModelAttribute EditBillableAmountRequest form, BindingResult bindingResult, Model model) {
		Case case_ = caseService.findById(caseId);
		
		if (bindingResult.hasErrors()) {
			model.addAttribute("error", bindingResult.getAllErrors().getFirst().getDefaultMessage());
			populateCaseDetailModel(case_, model);
			return "finance-officer/case-details";
		}
		
		try {
			taskService.updateBillableAmount(taskService.findById(taskId), form.getAmount());
			populateCaseDetailModel(case_, model);
			model.addAttribute("success", "Charge amount updated.");
		} catch (BusinessRuleViolationException e) {
			populateCaseDetailModel(case_, model);
			model.addAttribute("error", e.getMessage());
		}
		
		return "finance-officer/case-details";
	}
	
	@PostMapping("/finance-officer/cases/{id}/invoices/{invoiceId}/void")
	public String voidInvoice(@PathVariable("id") Long caseId, @PathVariable Long invoiceId, Model model) {
		Case case_ = caseService.findById(caseId);
		
		try {
			invoiceService.voidInvoice(invoiceService.findById(invoiceId));
			populateCaseDetailModel(case_, model);
			model.addAttribute("success", "Invoice voided. Its charges are unbilled again.");
		} catch (InvalidInvoiceStateException e) {
			populateCaseDetailModel(case_, model);
			model.addAttribute("error", e.getMessage());
		}
		
		return "finance-officer/case-details";
	}
	
	@PostMapping("/finance-officer/cases/{id}/invoices/{invoiceId}/void-refund")
	public String voidAndRefund(@PathVariable("id") Long caseId, @PathVariable Long invoiceId, Model model) {
		Case case_ = caseService.findById(caseId);
		
		try {
			invoiceService.voidAndRefundInvoice(case_, invoiceService.findById(invoiceId));
			populateCaseDetailModel(case_, model);
			model.addAttribute("success", "Invoice voided and refunded. Its charges are unbilled again.");
		} catch (InvalidInvoiceStateException e) {
			populateCaseDetailModel(case_, model);
			model.addAttribute("error", e.getMessage());
		}
		
		return "finance-officer/case-details";
	}
	
	@GetMapping("/finance-officer/documents/invoice/{id}")
	public ResponseEntity<Resource> viewInvoiceDocument(@PathVariable("id") Long documentId, Authentication authentication) {
		return documentService.streamInvoiceDocument(documentService.findById(documentId), currentFinanceOfficer(authentication));
	}
	
	@GetMapping("/finance-officer/transactions")
	public String transactions(Model model) {
		model.addAttribute("transactions", transactionService.findAll());
		model.addAttribute("totalCharges", transactionService.totalCharges());
		model.addAttribute("totalRefunds", transactionService.totalRefunds());
		model.addAttribute("netRevenue", transactionService.netRevenue());
		return "finance-officer/transactions";
	}
	
	// ==================== Clients (view and search only) ====================
	
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
	
	@GetMapping("/finance-officer/withdrawal")
	public String showWithdrawalForm(Model model) {
		return "finance-officer/withdrawal";
	}
	
	@PostMapping("/finance-officer/withdrawal")
	public String withdraw(@Valid @ModelAttribute WithdrawRequest form, BindingResult bindingResult, Model model) {
		if (bindingResult.hasErrors()) {
			model.addAttribute("error", bindingResult.getAllErrors().getFirst().getDefaultMessage());
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
			model.addAttribute("error", "The withdrawal could not be processed. Please try again.");
			return "finance-officer/withdrawal";
		}
		
		model.addAttribute("success", String.format("Withdrawal of %,.2f recorded for %s. Remaining balance: %,.2f.",
													form.getAmount(), client.getName(), client.getWalletBalance()));
		return "finance-officer/withdrawal";
	}
	
	// ==================== Payments (Cash Top-Up, Transactions) ====================
	
	@GetMapping("/finance-officer/cash-top-up")
	public String showCashTopUpForm(Model model) {
		return "finance-officer/cash-top-up";
	}
	
	@PostMapping("/finance-officer/cash-top-up")
	public String topUpByCash(@Valid @ModelAttribute TopUpByCashRequest form, BindingResult bindingResult, Model model) {
		if (bindingResult.hasErrors()) {
			model.addAttribute("error", bindingResult.getAllErrors().getFirst().getDefaultMessage());
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
			model.addAttribute("error", "The cash top-up could not be processed. Please try again.");
			return "finance-officer/cash-top-up";
		}
		
		model.addAttribute("success", "Trust account topped up successfully for " + client.getName() + ".");
		return "finance-officer/cash-top-up";
	}
	
	@GetMapping("/finance-officer/analytics")
	public String analytics(Authentication authentication, Model model) {
		model.addAttribute("user", currentFinanceOfficer(authentication));
		
		Map<Integer, BigDecimal> revenueByMonth = analyticsService.revenueByMonth(LocalDate.now().getYear());
		model.addAttribute("revenueByMonthLabels", monthLabels());
		model.addAttribute("revenueByMonthValues", revenueByMonth.values());
		
		model.addAttribute("outstandingInvoiceTotal", analyticsService.outstandingInvoiceTotal());
		model.addAttribute("avgDaysToPayment", analyticsService.avgDaysToPayment());
		
		Map<TransactionType, Integer> topUps = analyticsService.topUpCountComparison();
		model.addAttribute("cardTopUpCount", topUps.getOrDefault(TransactionType.CARD_TOP_UP, 0));
		model.addAttribute("cashTopUpCount", topUps.getOrDefault(TransactionType.CASH_TOP_UP, 0));
		
		model.addAttribute("totalClientWalletFloat", analyticsService.totalClientWalletFloat());
		
		return "finance-officer/analytics";
	}
	
	private List<String> monthLabels() {
		return List.of("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec");
	}
	
}
