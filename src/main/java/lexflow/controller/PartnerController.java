package lexflow.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lexflow.dto.case_.AddTaskRequest;
import lexflow.dto.case_.CloseCaseRequest;
import lexflow.dto.case_.EditTaskRequest;
import lexflow.dto.case_.SaveContactRequest;
import lexflow.dto.consultation.AssignStaffRequest;
import lexflow.dto.consultation.RejectConsultationRequest;
import lexflow.dto.document.RenameDocumentRequest;
import lexflow.dto.document.SelectDocumentsRequest;
import lexflow.dto.finance.CompileInvoiceRequest;
import lexflow.dto.finance.EditBillableAmountRequest;
import lexflow.dto.scheduling.ScheduleMeetingRequest;
import lexflow.dto.user.*;
import lexflow.entity.case_.Case;
import lexflow.entity.case_.CaseResolutionStatus;
import lexflow.entity.case_.Contact;
import lexflow.entity.case_.Task;
import lexflow.entity.consultation.Consultation;
import lexflow.entity.consultation.ConsultationStatus;
import lexflow.entity.document.DocumentType;
import lexflow.entity.finance.InvoiceStatus;
import lexflow.entity.scheduling.Event;
import lexflow.entity.scheduling.EventStatus;
import lexflow.entity.user.*;
import lexflow.exception.BusinessRuleViolationException;
import lexflow.exception.EntityAlreadyExistsException;
import lexflow.exception.InvalidStateException;
import lexflow.exception.PersistenceException;
import lexflow.exception.document.NonPdfDocumentException;
import lexflow.security.CustomUserPrincipal;
import lexflow.service.*;
import lexflow.util.ValidationUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class PartnerController {
	
	private final AnalyticsService analyticsService;
	private final CaseService caseService;
	private final ConsultationService consultationService;
	private final ConflictCheckService conflictCheckService;
	private final ContactService contactService;
	private final DocumentService documentService;
	private final EventService eventService;
	private final InvoiceService invoiceService;
	private final TaskService taskService;
	private final TransactionService transactionService;
	private final UserService userService;
	
	// Overview
	
	@GetMapping("/partner/dashboard")
	public String dashboard(Authentication authentication, Model model) {
		model.addAttribute("user", currentPartner(authentication));
		
		return "partner/dashboard";
	}
	
	@GetMapping("/partner/analytics")
	public String analytics(Authentication authentication, Model model) {
		model.addAttribute("user", currentPartner(authentication));
		
		Map<Role, Integer> activeUserCountByRole = analyticsService.activeUserCountByRole();
		model.addAttribute("activeUserCountByRoleLabels", buildLabels(activeUserCountByRole));
		model.addAttribute("activeUserCountByRoleValues", buildIntValues(activeUserCountByRole));
		model.addAttribute("totalClientWalletFloat", analyticsService.totalClientWalletFloat());
		
		Map<ConsultationStatus, Integer> consultationStatusBreakdown = analyticsService.consultationStatusBreakdown();
		model.addAttribute("consultationStatusBreakdownLabels", buildLabels(consultationStatusBreakdown));
		model.addAttribute("consultationStatusBreakdownValues", buildIntValues(consultationStatusBreakdown));
		model.addAttribute("consultationAcceptanceRate", analyticsService.consultationAcceptanceRate());
		
		Map<CaseResolutionStatus, Integer> caseResolutionBreakdown = analyticsService.caseResolutionBreakdown();
		model.addAttribute("caseResolutionBreakdownLabels", buildLabels(caseResolutionBreakdown));
		model.addAttribute("caseResolutionBreakdownValues", buildIntValues(caseResolutionBreakdown));
		model.addAttribute("averageCaseDurationDays", analyticsService.averageCaseDurationDays());
		
		Map<DocumentType, Integer> documentCountByType = analyticsService.documentCountByType();
		model.addAttribute("documentCountByTypeLabels", buildLabels(documentCountByType));
		model.addAttribute("documentCountByTypeValues", buildIntValues(documentCountByType));
		model.addAttribute("stagedDocumentCount", analyticsService.stagedDocumentCount());
		
		Map<Integer, BigDecimal> revenueByMonth = analyticsService.revenueByMonth(LocalDate.now().getYear());
		model.addAttribute("revenueByMonthLabels", monthLabels());
		model.addAttribute("revenueByMonthValues", revenueByMonth.values());
		model.addAttribute("outstandingInvoiceTotal", analyticsService.outstandingInvoiceTotal());
		
		Map<EventStatus, Integer> eventStatusBreakdown = analyticsService.eventStatusBreakdown();
		model.addAttribute("eventStatusBreakdownLabels", buildLabels(eventStatusBreakdown));
		model.addAttribute("eventStatusBreakdownValues", buildIntValues(eventStatusBreakdown));
		model.addAttribute("upcomingEventCountThisWeek", analyticsService.upcomingEventCountThisWeek());
		
		return "partner/analytics";
	}
	
	@GetMapping("/partner/profile")
	public String profile(Authentication authentication, Model model) {
		model.addAttribute("user", currentPartner(authentication));
		model.addAttribute("genders", Gender.values());
		
		return "partner/profile";
	}
	
	@PostMapping("/partner/profile/change-password")
	public String changePassword(@Valid @ModelAttribute ChangePasswordRequest form, BindingResult bindingResult, Authentication authentication, Model model, RedirectAttributes redirectAttributes) {
		Partner partner = currentPartner(authentication);
		model.addAttribute("genders", Gender.values());
		
		if (bindingResult.hasErrors()) {
			model.addAttribute("error", ValidationUtil.allErrors(bindingResult));
			model.addAttribute("user", partner);
			
			return "partner/profile";
		}
		
		if (!userService.verifyPassword(partner, form.getCurrentPassword())) {
			model.addAttribute("user", partner);
			model.addAttribute("error", "Current password is incorrect.");
			
			return "partner/profile";
		}
		
		userService.changePassword(partner, form.getNewPassword());
		
		redirectAttributes.addFlashAttribute("success", "Password changed successfully.");
		
		return "redirect:/partner/profile";
	}
	
	@PostMapping("/partner/profile/edit")
	public String editProfile(@Valid @ModelAttribute EditProfileRequest form, BindingResult bindingResult, Authentication authentication, Model model, RedirectAttributes redirectAttributes) {
		Partner partner = currentPartner(authentication);
		
		if (bindingResult.hasErrors()) {
			model.addAttribute("error", ValidationUtil.allErrors(bindingResult));
			model.addAttribute("user", partner);
			model.addAttribute("genders", Gender.values());
			
			return "partner/profile";
		}
		
		try {
			userService.updateDetails(partner, form.getName(), form.getDateOfBirth(), form.getGender(), form.getAddresses(), form.getPhoneNumbers(), form.getNicFile(), form.getBarFile());
		} catch (NonPdfDocumentException e) {
			model.addAttribute("user", partner);
			model.addAttribute("error", e.getMessage());
			model.addAttribute("genders", Gender.values());
			
			return "partner/profile";
		}
		
		redirectAttributes.addFlashAttribute("success", "Profile updated successfully.");
		
		return "redirect:/partner/profile";
	}
	
	// Consultations
	
	@GetMapping("/partner/consultations/pending")
	public String pendingConsultations(Model model) {
		model.addAttribute("consultations", consultationService.findAwaitingReviewForPartner());
		
		return "partner/consultation-pending-list";
	}
	
	@GetMapping("/partner/consultations")
	public String consultations(Model model) {
		model.addAttribute("consultations", consultationService.findAll());
		model.addAttribute("title", "Consultations");
		model.addAttribute("active", "consultations");
		
		return "partner/consultation-list";
	}
	
	@GetMapping("/partner/consultations/{id}")
	public String consultationDetails(@PathVariable("id") Long consultationId, Model model) {
		populateConsultationDetailModel(consultationService.findById(consultationId), model);
		
		return "partner/consultation-details";
	}
	
	@PostMapping("/partner/consultations/{id}/accept")
	public String acceptConsultation(@PathVariable("id") Long consultationId, @Valid @ModelAttribute AssignStaffRequest form, BindingResult bindingResult, Model model, RedirectAttributes redirectAttributes) {
		Consultation consultation = consultationService.findById(consultationId);
		
		if (bindingResult.hasErrors()) {
			model.addAttribute("error", ValidationUtil.allErrors(bindingResult));
			populateConsultationDetailModel(consultation, model);
			
			return "partner/consultation-details";
		}
		
		Associate associate = (Associate) userService.findById(form.getAssociateId());
		Paralegal paralegal = (Paralegal) userService.findById(form.getParalegalId());
		
		try {
			consultationService.partnerAccept(consultation, associate, paralegal);
		} catch (InvalidStateException e) {
			populateConsultationDetailModel(consultation, model);
			model.addAttribute("error", e.getMessage());
			
			return "partner/consultation-details";
		}
		
		redirectAttributes.addFlashAttribute("success", "Consultation accepted successfully.");
		
		return "redirect:/partner/consultations/{id}";
	}
	
	@PostMapping("/partner/consultations/{id}/reject")
	public String rejectConsultation(@PathVariable("id") Long consultationId, @Valid @ModelAttribute RejectConsultationRequest form, BindingResult bindingResult, Model model, RedirectAttributes redirectAttributes) {
		Consultation consultation = consultationService.findById(consultationId);
		
		if (bindingResult.hasErrors()) {
			model.addAttribute("error", ValidationUtil.allErrors(bindingResult));
			populateConsultationDetailModel(consultation, model);
			
			return "partner/consultation-details";
		}
		
		try {
			consultationService.partnerReject(consultation, form.getRejectionReason());
		} catch (InvalidStateException e) {
			populateConsultationDetailModel(consultation, model);
			model.addAttribute("error", e.getMessage());
			
			return "partner/consultation-details";
		}
		
		redirectAttributes.addFlashAttribute("success", "Consultation rejected successfully.");
		
		return "redirect:/partner/consultations/{id}";
	}
	
	@PostMapping("/partner/consultations/{id}/schedule")
	public String scheduleMeeting(@PathVariable("id") Long consultationId, @Valid @ModelAttribute ScheduleMeetingRequest form, BindingResult bindingResult, Model model, RedirectAttributes redirectAttributes) {
		Consultation consultation = consultationService.findById(consultationId);
		
		if (bindingResult.hasErrors()) {
			model.addAttribute("error", ValidationUtil.allErrors(bindingResult));
			populateConsultationDetailModel(consultation, model);
			
			return "partner/consultation-details";
		}
		
		try {
			eventService.schedule(consultation, form.getSubject(), form.getDate(), form.getStartTime(), form.getEndTime(), form.getLocation(), form.getNotes());
		} catch (InvalidStateException | BusinessRuleViolationException e) {
			populateConsultationDetailModel(consultation, model);
			model.addAttribute("error", e.getMessage());
			
			return "partner/consultation-details";
		}
		
		redirectAttributes.addFlashAttribute("success", "Meeting scheduled successfully.");
		
		return "redirect:/partner/consultations/{id}";
	}
	
	@PostMapping("/partner/consultations/{id}/open-case")
	public String openCase(@PathVariable("id") Long consultationId, RedirectAttributes redirectAttributes) {
		Case case_ = caseService.open(consultationService.findById(consultationId));
		
		redirectAttributes.addFlashAttribute("success", "Case opened successfully.");
		
		return "redirect:/partner/cases/" + case_.getCaseId();
	}
	
	@PostMapping("/partner/consultations/{id}/reassign")
	public String reassignConsultation(@PathVariable("id") Long consultationId, @Valid @ModelAttribute AssignStaffRequest form, BindingResult bindingResult, Model model, RedirectAttributes redirectAttributes) {
		Consultation consultation = consultationService.findById(consultationId);
		
		if (bindingResult.hasErrors()) {
			model.addAttribute("error", ValidationUtil.allErrors(bindingResult));
			populateConsultationDetailModel(consultation, model);
			
			return "partner/consultation-details";
		}
		
		Associate associate = (Associate) userService.findById(form.getAssociateId());
		Paralegal paralegal = (Paralegal) userService.findById(form.getParalegalId());
		consultationService.reassignStaff(consultation, associate, paralegal);
		
		redirectAttributes.addFlashAttribute("success", "Staff reassigned successfully.");
		
		return "redirect:/partner/consultations/{id}";
	}
	
	@GetMapping("/partner/conflict-check")
	public String conflictCheck(@RequestParam(required = false) String keyword, Model model) {
		
		if (keyword != null && !keyword.isBlank()) {
			model.addAttribute("conflictCheckResults", conflictCheckService.check(keyword));
			model.addAttribute("keyword", keyword);
		}
		
		return "partner/conflict-check";
	}
	
	// Cases
	
	@GetMapping("/partner/cases/to-open")
	public String casesToOpen(Model model) {
		model.addAttribute("consultations", consultationService.findAwaitingCase());
		model.addAttribute("title", "Cases to Open");
		model.addAttribute("active", "cases-to-open");
		
		return "partner/consultation-list";
	}
	
	@GetMapping("/partner/cases/open")
	public String openCases(Model model) {
		model.addAttribute("cases", caseService.findAllOpen());
		model.addAttribute("title", "Open Cases");
		model.addAttribute("active", "cases-open");
		
		return "partner/case-list";
	}
	
	@GetMapping("/partner/cases/closed")
	public String closedCases(Model model) {
		model.addAttribute("cases", caseService.findAllClosed());
		model.addAttribute("title", "Closed Cases");
		model.addAttribute("active", "cases-closed");
		
		return "partner/case-list";
	}
	
	@GetMapping("/partner/cases/{id}")
	public String caseDetails(@PathVariable("id") Long caseId, @RequestParam(required = false) String documentKeyword, Model model) {
		Case case_ = caseService.findById(caseId);
		populateCaseDetailModel(case_, model);
		
		model.addAttribute("caseDocuments", documentService.findByCaseAndKeyword(case_, documentKeyword));
		model.addAttribute("documentKeyword", documentKeyword);
		
		return "partner/case-details";
	}
	
	@PostMapping("/partner/cases/{id}/tasks")
	public String addTask(@PathVariable("id") Long caseId, @Valid @ModelAttribute AddTaskRequest form, BindingResult bindingResult, Model model, RedirectAttributes redirectAttributes) {
		Case case_ = caseService.findById(caseId);
		
		if (bindingResult.hasErrors()) {
			model.addAttribute("error", ValidationUtil.allErrors(bindingResult));
			populateCaseDetailModel(case_, model);
			
			return "partner/case-details";
		}
		
		try {
			taskService.add(case_, form.getTitle(), form.getDocumentingTask(), form.getSchedulingTask(), form.getBillingTask(), form.getCaseFiles(), form.getDate(), form.getStartTime(), form.getEndTime(), form.getLocation(), form.getNotes(), form.getBillableAmount());
		} catch (InvalidStateException | BusinessRuleViolationException e) {
			populateCaseDetailModel(case_, model);
			model.addAttribute("error", e.getMessage());
			
			return "partner/case-details";
		}
		
		redirectAttributes.addFlashAttribute("success", "Task added successfully.");
		
		return "redirect:/partner/cases/{id}";
	}
	
	@PostMapping("/partner/cases/{id}/tasks/{taskId}/edit")
	public String editTask(@PathVariable("id") Long caseId, @PathVariable Long taskId, @Valid @ModelAttribute EditTaskRequest form, BindingResult bindingResult, Model model, RedirectAttributes redirectAttributes) {
		Case case_ = caseService.findById(caseId);
		
		if (bindingResult.hasErrors()) {
			model.addAttribute("error", ValidationUtil.allErrors(bindingResult));
			populateCaseDetailModel(case_, model);
			
			return "partner/case-details";
		}
		
		try {
			taskService.update(case_, taskService.findOwnedByCase(taskId, case_), form.getTitle(), form.getDocumentingTask(), form.getSchedulingTask(), form.getBillingTask(), form.getCaseFiles(), form.getDate(), form.getStartTime(), form.getEndTime(), form.getLocation(), form.getNotes(), form.getBillableAmount());
		} catch (InvalidStateException | BusinessRuleViolationException e) {
			populateCaseDetailModel(case_, model);
			model.addAttribute("error", e.getMessage());
			
			return "partner/case-details";
		}
		
		redirectAttributes.addFlashAttribute("success", "Task updated successfully.");
		
		return "redirect:/partner/cases/{id}";
	}
	
	@PostMapping("/partner/cases/{id}/tasks/{taskId}/delete")
	public String deleteTask(@PathVariable("id") Long caseId, @PathVariable Long taskId, Model model, RedirectAttributes redirectAttributes) {
		Case case_ = caseService.findById(caseId);
		
		try {
			taskService.delete(case_, taskService.findOwnedByCase(taskId, case_));
		} catch (InvalidStateException | BusinessRuleViolationException e) {
			populateCaseDetailModel(case_, model);
			model.addAttribute("error", e.getMessage());
			
			return "partner/case-details";
		}
		
		redirectAttributes.addFlashAttribute("success", "Task deleted successfully.");
		
		return "redirect:/partner/cases/{id}";
	}
	
	@PostMapping("/partner/cases/{id}/tasks/{taskId}/edit-billable-amount")
	public String editBillableAmount(@PathVariable("id") Long caseId, @PathVariable Long taskId, @Valid @ModelAttribute EditBillableAmountRequest form, BindingResult bindingResult, Model model, RedirectAttributes redirectAttributes) {
		Case case_ = caseService.findById(caseId);
		
		if (bindingResult.hasErrors()) {
			model.addAttribute("error", ValidationUtil.allErrors(bindingResult));
			populateCaseDetailModel(case_, model);
			
			return "partner/case-details";
		}
		
		try {
			taskService.updateBillableAmount(case_, taskService.findById(taskId), form.getAmount());
		} catch (BusinessRuleViolationException e) {
			populateCaseDetailModel(case_, model);
			model.addAttribute("error", e.getMessage());
			
			return "partner/case-details";
		}
		
		redirectAttributes.addFlashAttribute("success", "Billable amount updated successfully.");
		
		return "redirect:/partner/cases/{id}";
	}
	
	@PostMapping("/partner/cases/{id}/invoices/compile")
	public String compileInvoice(@PathVariable("id") Long caseId, @Valid @ModelAttribute CompileInvoiceRequest form, Model model, RedirectAttributes redirectAttributes) {
		Case case_ = caseService.findById(caseId);
		
		try {
			invoiceService.compile(case_, form.getTaskIds());
		} catch (BusinessRuleViolationException e) {
			populateCaseDetailModel(case_, model);
			model.addAttribute("error", e.getMessage());
			
			return "partner/case-details";
		}
		
		redirectAttributes.addFlashAttribute("success", "Invoice compiled successfully.");
		
		return "redirect:/partner/cases/{id}";
	}
	
	@PostMapping("/partner/cases/{id}/invoices/{invoiceId}/charge")
	public String chargeInvoice(@PathVariable("id") Long caseId, @PathVariable Long invoiceId, Model model, RedirectAttributes redirectAttributes) {
		Case case_ = caseService.findById(caseId);
		
		try {
			invoiceService.charge(case_, invoiceService.findById(invoiceId));
		} catch (InvalidStateException | BusinessRuleViolationException e) {
			populateCaseDetailModel(case_, model);
			model.addAttribute("error", e.getMessage());
			
			return "partner/case-details";
		}
		
		redirectAttributes.addFlashAttribute("success", "Invoice charged successfully.");
		
		return "redirect:/partner/cases/{id}";
	}
	
	@PostMapping("/partner/cases/{id}/invoices/{invoiceId}/void")
	public String voidInvoice(@PathVariable("id") Long caseId, @PathVariable Long invoiceId, Model model, RedirectAttributes redirectAttributes) {
		Case case_ = caseService.findById(caseId);
		
		try {
			invoiceService.void_(case_, invoiceService.findById(invoiceId));
		} catch (InvalidStateException e) {
			populateCaseDetailModel(case_, model);
			model.addAttribute("error", e.getMessage());
			
			return "partner/case-details";
		}
		
		redirectAttributes.addFlashAttribute("success", "Invoice voided successfully.");
		
		return "redirect:/partner/cases/{id}";
	}
	
	@PostMapping("/partner/cases/{id}/invoices/{invoiceId}/void-refund")
	public String voidAndRefundInvoice(@PathVariable("id") Long caseId, @PathVariable Long invoiceId, Model model, RedirectAttributes redirectAttributes) {
		Case case_ = caseService.findById(caseId);
		
		try {
			invoiceService.voidAndRefund(case_, invoiceService.findById(invoiceId));
		} catch (InvalidStateException e) {
			populateCaseDetailModel(case_, model);
			model.addAttribute("error", e.getMessage());
			
			return "partner/case-details";
		}
		
		redirectAttributes.addFlashAttribute("success", "Invoice voided and refunded successfully.");
		
		return "redirect:/partner/cases/{id}";
	}
	
	@PostMapping("/partner/cases/{id}/documents/{documentId}/rename")
	public String renameCaseDocument(@PathVariable("id") Long caseId, @PathVariable Long documentId, @Valid @ModelAttribute RenameDocumentRequest form, BindingResult bindingResult, Model model, RedirectAttributes redirectAttributes) {
		Case case_ = caseService.findById(caseId);
		
		if (bindingResult.hasErrors()) {
			model.addAttribute("error", ValidationUtil.allErrors(bindingResult));
			populateCaseDetailModel(case_, model);
			
			return "partner/case-details";
		}
		
		documentService.rename(case_, documentId, form.getOriginalFilename());
		
		redirectAttributes.addFlashAttribute("success", "Document renamed successfully.");
		
		return "redirect:/partner/cases/{id}";
	}
	
	@PostMapping("/partner/cases/{id}/documents/{documentId}/delete")
	public String deleteCaseDocument(@PathVariable("id") Long caseId, @PathVariable Long documentId, RedirectAttributes redirectAttributes) {
		Case case_ = caseService.findById(caseId);
		documentService.deleteStaged(case_, List.of(documentId));
		
		redirectAttributes.addFlashAttribute("success", "Document deleted successfully.");
		
		return "redirect:/partner/cases/{id}";
	}
	
	@PostMapping("/partner/cases/{id}/documents/{documentId}/accept")
	public String acceptStagedDocument(@PathVariable("id") Long caseId, @PathVariable Long documentId, RedirectAttributes redirectAttributes) {
		Case case_ = caseService.findById(caseId);
		documentService.addStagedToCase(case_, List.of(documentId));
		
		redirectAttributes.addFlashAttribute("success", "Document added to case successfully.");
		
		return "redirect:/partner/cases/{id}";
	}
	
	@PostMapping("/partner/cases/{id}/documents/{documentId}/reject")
	public String rejectStagedDocument(@PathVariable("id") Long caseId, @PathVariable Long documentId, RedirectAttributes redirectAttributes) {
		Case case_ = caseService.findById(caseId);
		documentService.deleteStaged(case_, List.of(documentId));
		
		redirectAttributes.addFlashAttribute("success", "Document deleted successfully.");
		
		return "redirect:/partner/cases/{id}";
	}
	
	@PostMapping("/partner/cases/{id}/documents/accept-selected")
	public String acceptStagedDocuments(@PathVariable("id") Long caseId, @Valid @ModelAttribute SelectDocumentsRequest form, BindingResult bindingResult, Model model, RedirectAttributes redirectAttributes) {
		Case case_ = caseService.findById(caseId);
		
		if (bindingResult.hasErrors()) {
			model.addAttribute("error", ValidationUtil.allErrors(bindingResult));
			populateCaseDetailModel(case_, model);
			
			return "partner/case-details";
		}
		
		documentService.addStagedToCase(case_, form.getDocumentIds());
		
		redirectAttributes.addFlashAttribute("success", "Documents added to case successfully.");
		
		return "redirect:/partner/cases/{id}";
	}
	
	@PostMapping("/partner/cases/{id}/documents/reject-selected")
	public String rejectStagedDocuments(@PathVariable("id") Long caseId, @Valid @ModelAttribute SelectDocumentsRequest form, BindingResult bindingResult, Model model, RedirectAttributes redirectAttributes) {
		Case case_ = caseService.findById(caseId);
		
		if (bindingResult.hasErrors()) {
			model.addAttribute("error", ValidationUtil.allErrors(bindingResult));
			populateCaseDetailModel(case_, model);
			
			return "partner/case-details";
		}
		
		documentService.deleteStaged(case_, form.getDocumentIds());
		
		redirectAttributes.addFlashAttribute("success", "Documents deleted successfully.");
		
		return "redirect:/partner/cases/{id}";
	}
	
	@PostMapping("/partner/cases/{id}/contacts")
	public String saveContact(@PathVariable("id") Long caseId, @Valid @ModelAttribute SaveContactRequest form, BindingResult bindingResult, Model model, RedirectAttributes redirectAttributes) {
		Case case_ = caseService.findById(caseId);
		
		if (bindingResult.hasErrors()) {
			model.addAttribute("error", ValidationUtil.allErrors(bindingResult));
			populateCaseDetailModel(case_, model);
			
			return "partner/case-details";
		}
		
		try {
			contactService.save(case_, form.getName(), form.getPhoneNumbers(), form.getEmails(), form.getAddresses(), form.getNotes());
		} catch (InvalidStateException e) {
			populateCaseDetailModel(case_, model);
			model.addAttribute("error", e.getMessage());
			
			return "partner/case-details";
		}
		
		redirectAttributes.addFlashAttribute("success", "Contact added successfully.");
		
		return "redirect:/partner/cases/{id}";
	}
	
	@PostMapping("/partner/cases/{id}/contacts/{contactId}/edit")
	public String editContact(@PathVariable("id") Long caseId, @PathVariable Long contactId, @Valid @ModelAttribute SaveContactRequest form, BindingResult bindingResult, Model model, RedirectAttributes redirectAttributes) {
		Case case_ = caseService.findById(caseId);
		
		if (bindingResult.hasErrors()) {
			model.addAttribute("error", ValidationUtil.allErrors(bindingResult));
			populateCaseDetailModel(case_, model);
			
			return "partner/case-details";
		}
		
		Contact contact = contactService.findOwnedByCase(contactId, case_);
		
		try {
			contactService.update(case_, contact, form.getName(), form.getPhoneNumbers(), form.getEmails(), form.getAddresses(), form.getNotes());
		} catch (InvalidStateException e) {
			populateCaseDetailModel(case_, model);
			model.addAttribute("error", e.getMessage());
			
			return "partner/case-details";
		}
		
		redirectAttributes.addFlashAttribute("success", "Contact updated successfully.");
		
		return "redirect:/partner/cases/{id}";
	}
	
	@PostMapping("/partner/cases/{id}/contacts/{contactId}/delete")
	public String deleteContact(@PathVariable("id") Long caseId, @PathVariable Long contactId, Model model, RedirectAttributes redirectAttributes) {
		Case case_ = caseService.findById(caseId);
		
		try {
			contactService.delete(case_, contactService.findOwnedByCase(contactId, case_));
		} catch (InvalidStateException e) {
			populateCaseDetailModel(case_, model);
			model.addAttribute("error", e.getMessage());
			
			return "partner/case-details";
		}
		
		redirectAttributes.addFlashAttribute("success", "Contact deleted successfully.");
		
		return "redirect:/partner/cases/{id}";
	}
	
	@PostMapping("/partner/cases/{id}/reassign")
	public String reassignCase(@PathVariable("id") Long caseId, @Valid @ModelAttribute AssignStaffRequest form, BindingResult bindingResult, Model model, RedirectAttributes redirectAttributes) {
		Case case_ = caseService.findById(caseId);
		
		if (bindingResult.hasErrors()) {
			model.addAttribute("error", ValidationUtil.allErrors(bindingResult));
			populateCaseDetailModel(case_, model);
			
			return "partner/case-details";
		}
		
		Associate associate = (Associate) userService.findById(form.getAssociateId());
		Paralegal paralegal = (Paralegal) userService.findById(form.getParalegalId());
		
		try {
			caseService.reassignStaff(case_, associate, paralegal);
		} catch (InvalidStateException e) {
			populateCaseDetailModel(case_, model);
			model.addAttribute("error", e.getMessage());
			
			return "partner/case-details";
		}
		
		redirectAttributes.addFlashAttribute("success", "Staff reassigned successfully.");
		
		return "redirect:/partner/cases/{id}";
	}
	
	@PostMapping("/partner/cases/{id}/close")
	public String closeCase(@PathVariable("id") Long caseId, @Valid @ModelAttribute CloseCaseRequest form, BindingResult bindingResult, Model model, RedirectAttributes redirectAttributes) {
		Case case_ = caseService.findById(caseId);
		
		if (bindingResult.hasErrors()) {
			model.addAttribute("error", ValidationUtil.allErrors(bindingResult));
			populateCaseDetailModel(case_, model);
			
			return "partner/case-details";
		}
		
		try {
			caseService.close(case_, form.getCaseResolutionStatus());
		} catch (InvalidStateException | BusinessRuleViolationException e) {
			populateCaseDetailModel(case_, model);
			model.addAttribute("error", e.getMessage());
			
			return "partner/case-details";
		}
		
		redirectAttributes.addFlashAttribute("success", "Case closed successfully.");
		
		return "redirect:/partner/cases/{id}";
	}
	
	// Schedule
	
	@GetMapping("/partner/events/upcoming")
	public String upcomingEvents(Model model) {
		model.addAttribute("events", eventService.upcoming(eventService.findAll()));
		model.addAttribute("title", "Upcoming Events");
		model.addAttribute("active", "events-upcoming");
		
		return "partner/event-list";
	}
	
	@GetMapping("/partner/events/past")
	public String pastEvents(Model model) {
		model.addAttribute("events", eventService.past(eventService.findAll()));
		model.addAttribute("title", "Past Events");
		model.addAttribute("active", "events-past");
		
		return "partner/event-list";
	}
	
	@GetMapping("/partner/events/{id}")
	public String eventDetails(@PathVariable("id") Long eventId, Model model) {
		model.addAttribute("event", eventService.findById(eventId));
		
		return "partner/event-details";
	}
	
	@PostMapping("/partner/events/{id}/complete")
	public String completeEvent(@PathVariable("id") Long eventId, Model model, RedirectAttributes redirectAttributes) {
		Event event = eventService.findById(eventId);
		
		try {
			eventService.complete(event);
		} catch (InvalidStateException | BusinessRuleViolationException e) {
			model.addAttribute("event", event);
			model.addAttribute("error", e.getMessage());
			
			return "partner/event-details";
		}
		
		redirectAttributes.addFlashAttribute("success", "Event completed successfully.");
		
		return "redirect:/partner/events/{id}";
	}
	
	@PostMapping("/partner/events/{id}/cancel")
	public String cancelEvent(@PathVariable("id") Long eventId, Model model, RedirectAttributes redirectAttributes) {
		Event event = eventService.findById(eventId);
		
		try {
			eventService.cancel(event);
		} catch (InvalidStateException e) {
			model.addAttribute("event", event);
			model.addAttribute("error", e.getMessage());
			
			return "partner/event-details";
		}
		
		redirectAttributes.addFlashAttribute("success", "Event canceled successfully.");
		
		return "redirect:/partner/events/{id}";
	}
	
	@PostMapping("/partner/events/{id}/reschedule")
	public String rescheduleEvent(@PathVariable("id") Long eventId, @Valid @ModelAttribute ScheduleMeetingRequest form, BindingResult bindingResult, Model model, RedirectAttributes redirectAttributes) {
		Event event = eventService.findById(eventId);
		
		if (bindingResult.hasErrors()) {
			model.addAttribute("event", event);
			model.addAttribute("error", ValidationUtil.allErrors(bindingResult));
			
			return "partner/event-details";
		}
		
		try {
			eventService.reschedule(event, form.getSubject(), form.getDate(), form.getStartTime(), form.getEndTime(), form.getLocation(), form.getNotes());
		} catch (InvalidStateException | BusinessRuleViolationException e) {
			model.addAttribute("event", event);
			model.addAttribute("error", e.getMessage());
			
			return "partner/event-details";
		}
		
		redirectAttributes.addFlashAttribute("success", "Event rescheduled successfully.");
		
		return "redirect:/partner/events/{id}";
	}
	
	// Finance
	
	@GetMapping("/partner/transactions")
	public String transactions(Model model) {
		model.addAttribute("transactions", transactionService.findAll());
		model.addAttribute("totalCharges", transactionService.totalCharges());
		model.addAttribute("totalRefunds", transactionService.totalRefunds());
		model.addAttribute("netRevenue", transactionService.netRevenue());
		
		return "partner/transactions";
	}
	
	// Users
	
	@GetMapping("/partner/register-staff")
	public String showRegisterStaffForm(Model model) {
		model.addAttribute("genders", Gender.values());
		model.addAttribute("roles", List.of(Role.PARTNER, Role.ASSOCIATE, Role.PARALEGAL, Role.CLIENT_RELATIONS_OFFICER, Role.FINANCE_OFFICER));
		
		return "partner/register-staff";
	}
	
	@PostMapping("/partner/register-staff")
	public String registerStaff(@Valid @ModelAttribute RegisterStaffRequest form, BindingResult bindingResult, Model model, RedirectAttributes redirectAttributes) {
		
		if (bindingResult.hasErrors()) {
			model.addAttribute("error", ValidationUtil.allErrors(bindingResult));
			model.addAttribute("genders", Gender.values());
			model.addAttribute("roles", List.of(Role.PARTNER, Role.ASSOCIATE, Role.PARALEGAL, Role.CLIENT_RELATIONS_OFFICER, Role.FINANCE_OFFICER));
			
			return "partner/register-staff";
		}
		
		User user;
		
		try {
			user = userService.register(form.getRole(), form.getEmail(), form.getPassword(), form.getName(), form.getDateOfBirth(), form.getGender(), form.getAddresses(), form.getPhoneNumbers(), form.getNicNumber(), form.getBarNumber(), form.getNicFile(), form.getBarFile());
		} catch (EntityAlreadyExistsException | BusinessRuleViolationException e) {
			model.addAttribute("genders", Gender.values());
			model.addAttribute("roles", List.of(Role.PARTNER, Role.ASSOCIATE, Role.PARALEGAL, Role.CLIENT_RELATIONS_OFFICER, Role.FINANCE_OFFICER));
			model.addAttribute("error", e.getMessage());
			
			return "partner/register-staff";
		} catch (PersistenceException e) {
			model.addAttribute("genders", Gender.values());
			model.addAttribute("roles", List.of(Role.PARTNER, Role.ASSOCIATE, Role.PARALEGAL, Role.CLIENT_RELATIONS_OFFICER, Role.FINANCE_OFFICER));
			model.addAttribute("error", "Could not register the staff member. Please try again.");
			
			return "partner/register-staff";
		}
		
		redirectAttributes.addFlashAttribute("success", "Staff member registered successfully.");
		
		return "redirect:/partner/users/" + user.getUserId();
	}
	
	@GetMapping("/partner/users/partners")
	public String partners(@RequestParam(required = false) String keyword, Authentication authentication, Model model) {
		model.addAttribute("users", userService.findByRoleAndKeyword(Role.PARTNER, keyword));
		model.addAttribute("keyword", keyword);
		model.addAttribute("title", "Partners");
		model.addAttribute("currentUserEmail", authentication.getName());
		
		return "partner/partner-list";
	}
	
	@GetMapping("/partner/users/associates")
	public String associates(@RequestParam(required = false) String keyword, Model model) {
		model.addAttribute("users", userService.findByRoleAndKeyword(Role.ASSOCIATE, keyword));
		model.addAttribute("keyword", keyword);
		model.addAttribute("title", "Associates");
		model.addAttribute("active", "users-associates");
		model.addAttribute("listUrl", "/partner/users/associates");
		
		return "partner/user-list";
	}
	
	@GetMapping("/partner/users/paralegals")
	public String paralegals(@RequestParam(required = false) String keyword, Model model) {
		model.addAttribute("users", userService.findByRoleAndKeyword(Role.PARALEGAL, keyword));
		model.addAttribute("keyword", keyword);
		model.addAttribute("title", "Paralegals");
		model.addAttribute("active", "users-paralegals");
		model.addAttribute("listUrl", "/partner/users/paralegals");
		
		return "partner/user-list";
	}
	
	@GetMapping("/partner/users/client-relations-officers")
	public String clientRelationsOfficers(@RequestParam(required = false) String keyword, Model model) {
		model.addAttribute("users", userService.findByRoleAndKeyword(Role.CLIENT_RELATIONS_OFFICER, keyword));
		model.addAttribute("keyword", keyword);
		model.addAttribute("title", "Client Relations Officers");
		model.addAttribute("active", "users-client-relations-officers");
		model.addAttribute("listUrl", "/partner/users/client-relations-officers");
		
		return "partner/user-list";
	}
	
	@GetMapping("/partner/users/finance-officers")
	public String financeOfficers(@RequestParam(required = false) String keyword, Model model) {
		model.addAttribute("users", userService.findByRoleAndKeyword(Role.FINANCE_OFFICER, keyword));
		model.addAttribute("keyword", keyword);
		model.addAttribute("title", "Finance Officers");
		model.addAttribute("active", "users-finance-officers");
		model.addAttribute("listUrl", "/partner/users/finance-officers");
		
		return "partner/user-list";
	}
	
	@GetMapping("/partner/users/clients")
	public String clients(@RequestParam(required = false) String keyword, Model model) {
		model.addAttribute("users", userService.findByRoleAndKeyword(Role.CLIENT, keyword));
		model.addAttribute("keyword", keyword);
		model.addAttribute("title", "Clients");
		model.addAttribute("active", "users-clients");
		model.addAttribute("listUrl", "/partner/users/clients");
		
		return "partner/user-list";
	}
	
	@GetMapping("/partner/users/{id}")
	public String userDetails(@PathVariable("id") Long userId, Authentication authentication, Model model) {
		User user = userService.findById(userId);
		populateUserDetailModel(user, authentication, model);
		
		return userDetailsView(user);
	}
	
	@PostMapping("/partner/users/{id}/delete")
	public String deleteUser(@PathVariable("id") Long userId, Authentication authentication, HttpServletRequest request, HttpServletResponse response, Model model, RedirectAttributes redirectAttributes) {
		User user = userService.findById(userId);
		
		try {
			userService.delete(user);
		} catch (BusinessRuleViolationException e) {
			populateUserDetailModel(user, authentication, model);
			model.addAttribute("error", e.getMessage());
			
			return userDetailsView(user);
		}
		
		if (user.getEmail().equals(authentication.getName())) {
			new SecurityContextLogoutHandler().logout(request, response, authentication);
			redirectAttributes.addFlashAttribute("success", "Account deleted successfully.");
			
			return "redirect:/auth/login";
		}
		
		String listUrl = switch (user) {
			case Partner ignored -> "/partner/users/partners";
			case Associate ignored -> "/partner/users/associates";
			case Paralegal ignored -> "/partner/users/paralegals";
			case Client ignored -> "/partner/users/clients";
			case ClientRelationsOfficer ignored -> "/partner/users/client-relations-officers";
			case FinanceOfficer ignored -> "/partner/users/finance-officers";
			default -> "/partner/dashboard";
		};
		
		redirectAttributes.addFlashAttribute("success", "User deleted successfully.");
		
		return "redirect:" + listUrl;
	}
	
	@PostMapping("/partner/users/{id}/reactivate")
	public String reactivateUser(@PathVariable("id") Long userId, @Valid @ModelAttribute ReactivateAccountRequest form, BindingResult bindingResult, Authentication authentication, Model model, RedirectAttributes redirectAttributes) {
		User user = userService.findById(userId);
		
		if (bindingResult.hasErrors()) {
			model.addAttribute("error", ValidationUtil.allErrors(bindingResult));
			populateUserDetailModel(user, authentication, model);
			
			return userDetailsView(user);
		}
		
		try {
			userService.reactivate(user, form.getTemporaryPassword());
		} catch (InvalidStateException | BusinessRuleViolationException e) {
			populateUserDetailModel(user, authentication, model);
			model.addAttribute("error", e.getMessage());
			
			return userDetailsView(user);
		}
		
		redirectAttributes.addFlashAttribute("success", "Account reactivated successfully.");
		
		return "redirect:/partner/users/{id}";
	}
	
	@PostMapping("/partner/users/{id}/reset-password")
	public String resetUserPassword(@PathVariable("id") Long userId, @Valid @ModelAttribute ResetPasswordRequest form, BindingResult bindingResult, Authentication authentication, HttpServletRequest request, HttpServletResponse response, Model model, RedirectAttributes redirectAttributes) {
		User user = userService.findById(userId);
		
		if (bindingResult.hasErrors()) {
			model.addAttribute("error", ValidationUtil.allErrors(bindingResult));
			populateUserDetailModel(user, authentication, model);
			
			return userDetailsView(user);
		}
		
		userService.changePassword(user, form.getTemporaryPassword());
		
		if (user.getEmail().equals(authentication.getName())) {
			new SecurityContextLogoutHandler().logout(request, response, authentication);
			redirectAttributes.addFlashAttribute("success", "Password reset successfully.");
			
			return "redirect:/auth/login";
		}
		
		redirectAttributes.addFlashAttribute("success", "Password reset successfully.");
		
		return "redirect:/partner/users/{id}";
	}
	
	@PostMapping("/partner/users/{id}/edit")
	public String editUser(@PathVariable("id") Long userId, @Valid @ModelAttribute EditProfileRequest form, BindingResult bindingResult, Authentication authentication, Model model, RedirectAttributes redirectAttributes) {
		User user = userService.findById(userId);
		
		if (bindingResult.hasErrors()) {
			model.addAttribute("error", ValidationUtil.allErrors(bindingResult));
			populateUserDetailModel(user, authentication, model);
			
			return userDetailsView(user);
		}
		
		try {
			userService.updateDetails(user, form.getName(), form.getDateOfBirth(), form.getGender(), form.getAddresses(), form.getPhoneNumbers(), form.getNicFile(), form.getBarFile());
		} catch (NonPdfDocumentException e) {
			model.addAttribute("error", e.getMessage());
			populateUserDetailModel(user, authentication, model);
			
			return userDetailsView(user);
		}
		
		redirectAttributes.addFlashAttribute("success", "User updated successfully.");
		
		return "redirect:/partner/users/{id}";
	}
	
	// Documents
	
	@GetMapping({"/partner/documents/user/{id}", "/partner/documents/user/{id}/{filename}"})
	public ResponseEntity<Resource> viewUserDocument(@PathVariable("id") Long documentId, Authentication authentication) {
		return documentService.streamUserDocument(documentService.findById(documentId), currentPartner(authentication));
	}
	
	@GetMapping({"/partner/documents/consultation/{id}", "/partner/documents/consultation/{id}/{filename}"})
	public ResponseEntity<Resource> viewConsultationDocument(@PathVariable("id") Long documentId, Authentication authentication) {
		return documentService.streamConsultationDocument(documentService.findById(documentId), currentPartner(authentication));
	}
	
	@GetMapping({"/partner/documents/case/{id}", "/partner/documents/case/{id}/{filename}"})
	public ResponseEntity<Resource> viewCaseDocument(@PathVariable("id") Long documentId, Authentication authentication) {
		return documentService.streamCaseDocument(documentService.findById(documentId), currentPartner(authentication));
	}
	
	@GetMapping({"/partner/documents/invoice/{id}", "/partner/documents/invoice/{id}/{filename}"})
	public ResponseEntity<Resource> viewInvoiceDocument(@PathVariable("id") Long documentId, Authentication authentication) {
		return documentService.streamInvoiceDocument(documentService.findById(documentId), currentPartner(authentication));
	}
	
	// Private Helpers
	
	private Partner currentPartner(Authentication authentication) {
		CustomUserPrincipal principal = (CustomUserPrincipal) authentication.getPrincipal();
		
		return (Partner) principal.user();
	}
	
	private List<String> buildLabels(Map<?, ?> counts) {
		List<String> labels = new ArrayList<>();
		
		for (Object key : counts.keySet()) {
			labels.add(readableLabel(key.toString()));
		}
		
		return labels;
	}
	
	private List<Integer> buildIntValues(Map<?, Integer> counts) {
		List<Integer> values = new ArrayList<>();
		
		for (Integer value : counts.values()) {
			values.add(value);
		}
		
		return values;
	}
	
	private List<String> monthLabels() {
		return List.of("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec");
	}
	
	private void populateConsultationDetailModel(Consultation consultation, Model model) {
		model.addAttribute("consultation", consultation);
		model.addAttribute("consultationDocuments", documentService.findByConsultation(consultation));
		model.addAttribute("associates", userService.findActiveAssociates());
		model.addAttribute("paralegals", userService.findActiveParalegals());
		model.addAttribute("consultationEvents", eventService.findByConsultation(consultation));
		
		Case existingCase = caseService.findByConsultation(consultation);
		model.addAttribute("existingCase", existingCase);
		model.addAttribute("hasCase", existingCase != null);
	}
	
	private void populateCaseDetailModel(Case case_, Model model) {
		model.addAttribute("case", case_);
		model.addAttribute("associates", userService.findActiveAssociates());
		model.addAttribute("paralegals", userService.findActiveParalegals());
		model.addAttribute("stagedDocuments", documentService.findStagedByCase(case_));
		model.addAttribute("caseDocuments", documentService.findByCase(case_));
		model.addAttribute("contacts", contactService.findByCase(case_));
		
		List<Task> tasks = taskService.findByCase(case_);
		model.addAttribute("tasks", tasks);
		model.addAttribute("taskDocuments", documentService.findByTasks(tasks));
		model.addAttribute("unbilledTasks", taskService.findUnbilledByCase(case_));
		model.addAttribute("unpaidInvoices", invoiceService.findByCaseAndStatus(case_, InvoiceStatus.UNPAID));
		model.addAttribute("paidInvoices", invoiceService.findByCaseAndStatus(case_, InvoiceStatus.PAID));
		model.addAttribute("voidedInvoices", invoiceService.findByCaseAndStatus(case_, InvoiceStatus.VOIDED));
	}
	
	private void populateUserDetailModel(User user, Authentication authentication, Model model) {
		model.addAttribute("user", user);
		model.addAttribute("isSelf", user.getEmail().equals(authentication.getName()));
		model.addAttribute("isAssociate", user instanceof Associate);
		model.addAttribute("isClient", user instanceof Client);
		model.addAttribute("genders", Gender.values());
		
		switch (user) {
			
			case Client client -> {
				model.addAttribute("relatedConsultations", consultationService.findAllForClient(client));
				model.addAttribute("relatedCases", caseService.findAllForClient(client));
				model.addAttribute("relatedEvents", eventService.findAllForClient(client));
				model.addAttribute("relatedTransactions", transactionService.findAllForClient(client));
			}
			
			case Associate associate -> {
				model.addAttribute("relatedConsultations", consultationService.findAllForAssociate(associate));
				model.addAttribute("relatedCases", caseService.findAllForAssociate(associate));
				model.addAttribute("relatedEvents", eventService.findAllForAssociate(associate));
			}
			
			case Paralegal paralegal -> {
				model.addAttribute("relatedConsultations", consultationService.findAllForParalegal(paralegal));
				model.addAttribute("relatedCases", caseService.findAllForParalegal(paralegal));
				model.addAttribute("relatedEvents", eventService.findAllForParalegal(paralegal));
			}
			
			default -> {
			}
			
		}
		
		model.addAttribute("active", switch (user) {
			case Partner ignored -> "users-partners";
			case Associate ignored -> "users-associates";
			case Paralegal ignored -> "users-paralegals";
			case Client ignored -> "users-clients";
			case ClientRelationsOfficer ignored -> "users-client-relations-officers";
			case FinanceOfficer ignored -> "users-finance-officers";
			default -> "";
		});
	}
	
	private String userDetailsView(User user) {
		return user instanceof Partner ? "partner/partner-details" : "partner/user-details";
	}
	
	private String readableLabel(String enumName) {
		String[] words = enumName.toLowerCase().split("_");
		StringBuilder label = new StringBuilder();
		
		for (String word : words) {
			label.append(label.isEmpty() ? "" : " ").append(word.equals("nic") ? "NIC" : Character.toUpperCase(word.charAt(0)) + word.substring(1));
		}
		
		return label.toString();
	}
	
}
