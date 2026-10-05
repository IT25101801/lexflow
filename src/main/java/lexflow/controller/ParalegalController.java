package lexflow.controller;

import jakarta.validation.Valid;
import lexflow.dto.case_.AddTaskRequest;
import lexflow.dto.case_.RenameTaskRequest;
import lexflow.dto.case_.SaveContactRequest;
import lexflow.dto.document.RenameDocumentRequest;
import lexflow.dto.scheduling.ScheduleMeetingRequest;
import lexflow.dto.user.ChangePasswordRequest;
import lexflow.entity.case_.Case;
import lexflow.entity.case_.Task;
import lexflow.entity.consultation.Consultation;
import lexflow.entity.user.Paralegal;
import lexflow.entity.user.User;
import lexflow.exception.BusinessRuleViolationException;
import lexflow.exception.InvalidStateException;
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

import java.util.List;

@Controller
@RequiredArgsConstructor
public class ParalegalController {
	
	private final ConsultationService consultationService;
	
	private final UserService userService;
	
	private final DocumentService documentService;
	
	private final EventService eventService;
	
	private final CaseService caseService;
	
	private final TaskService taskService;
	
	private final ContactService contactService;
	
	private final AnalyticsService analyticsService;
	
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
	
	@GetMapping("/paralegal/profile")
	public String profile(Authentication authentication, Model model) {
		model.addAttribute("user", currentParalegal(authentication));
		return "paralegal/profile";
	}
	
	@PostMapping("/paralegal/profile/change-password")
	public String changePassword(Authentication authentication,
								 @Valid @ModelAttribute ChangePasswordRequest form, BindingResult bindingResult,
								 Model model) {
		User user = currentParalegal(authentication);
		
		if (bindingResult.hasErrors()) {
			model.addAttribute("error", bindingResult.getAllErrors().getFirst().getDefaultMessage());
			model.addAttribute("user", user);
			return "paralegal/profile";
		}
		
		if (!userService.verifyPassword(user, form.getCurrentPassword())) {
			model.addAttribute("user", user);
			model.addAttribute("error", "Current password is incorrect.");
			return "paralegal/profile";
		}
		
		userService.changePassword(user, form.getNewPassword());
		
		model.addAttribute("user", user);
		model.addAttribute("success", "Password changed successfully.");
		return "paralegal/profile";
	}
	
	@GetMapping("/paralegal/documents/user/{id}")
	public ResponseEntity<Resource> viewUserDocument(@PathVariable("id") Long documentId, Authentication authentication) {
		return documentService.streamUserDocument(documentService.findById(documentId), currentParalegal(authentication));
	}
	
	// ==================== Consultations (Unscheduled, All) ====================
	
	@GetMapping("/paralegal/consultations/pending")
	public String pendingConsultations(Authentication authentication, Model model) {
		model.addAttribute("consultations", consultationService.findAwaitingReviewForParalegal(currentParalegal(authentication)));
		return "paralegal/consultation-pending-list";
	}
	
	@GetMapping("/paralegal/consultations")
	public String consultations(Authentication authentication, Model model) {
		model.addAttribute("consultations", consultationService.findAllForParalegal(currentParalegal(authentication)));
		return "paralegal/consultation-list";
	}
	
	@GetMapping("/paralegal/consultations/{id}")
	public String consultationDetails(@PathVariable("id") Long consultationId, Authentication authentication, Model model) {
		Consultation consultation = consultationService.findOwnedByParalegal(consultationId, currentParalegal(authentication));
		model.addAttribute("consultation", consultation);
		model.addAttribute("consultationDocuments", documentService.findConsultationDocuments(consultation));
		model.addAttribute("existingCase", caseService.findByConsultation(consultation));
		return "paralegal/consultation-details";
	}
	
	// ==================== Documents (cross-page file streaming, no dedicated nav entry) ====================
	
	@GetMapping("/paralegal/documents/consultation/{id}")
	public ResponseEntity<Resource> viewConsultationDocument(@PathVariable("id") Long documentId, Authentication authentication) {
		return documentService.streamConsultationDocument(documentService.findById(documentId), currentParalegal(authentication));
	}
	
	// ==================== Cases (Open, Closed, Details & Actions) ====================
	
	@GetMapping("/paralegal/cases/open")
	public String openCases(Authentication authentication, Model model) {
		model.addAttribute("cases", caseService.findOpenForParalegal(currentParalegal(authentication)));
		model.addAttribute("title", "Open Cases");
		model.addAttribute("active", "cases-open");
		return "paralegal/case-list";
	}
	
	@GetMapping("/paralegal/cases/closed")
	public String closedCases(Authentication authentication, Model model) {
		model.addAttribute("cases", caseService.findClosedForParalegal(currentParalegal(authentication)));
		model.addAttribute("title", "Closed Cases");
		model.addAttribute("active", "cases-closed");
		return "paralegal/case-list";
	}
	
	@GetMapping("/paralegal/cases/{id}")
	public String caseDetails(@PathVariable("id") Long caseId, @RequestParam(required = false) String documentKeyword,
							  Authentication authentication, Model model) {
		Case case_ = caseService.findOwnedByParalegal(caseId, currentParalegal(authentication));
		populateCaseDetailModel(case_, model);
		
		model.addAttribute("caseDocuments", documentService.searchCaseDocuments(case_, documentKeyword));
		model.addAttribute("documentKeyword", documentKeyword);
		
		return "paralegal/case-details";
	}
	
	// ==================== Helpers ====================
	
	private void populateCaseDetailModel(Case case_, Model model) {
		model.addAttribute("case", case_);
		model.addAttribute("stagedDocuments", documentService.findStagedDocuments(case_));
		model.addAttribute("caseDocuments", documentService.findCaseDocuments(case_));
		model.addAttribute("contacts", contactService.findByCase(case_));
		
		List<Task> tasks = taskService.findByCase(case_);
		model.addAttribute("tasks", tasks);
		model.addAttribute("taskDocuments", documentService.findDocumentsByTasks(tasks));
	}
	
	@PostMapping("/paralegal/cases/{id}/contacts")
	public String addContact(@PathVariable("id") Long caseId, @Valid @ModelAttribute SaveContactRequest form, BindingResult bindingResult,
							 Authentication authentication, Model model) {
		Case case_ = caseService.findOwnedByParalegal(caseId, currentParalegal(authentication));
		
		if (bindingResult.hasErrors()) {
			model.addAttribute("error", bindingResult.getAllErrors().getFirst().getDefaultMessage());
			populateCaseDetailModel(case_, model);
			return "paralegal/case-details";
		}
		
		try {
			contactService.add(case_, form.getName(), form.getPhoneNumbers(), form.getEmails(), form.getAddresses(), form.getNotes());
			populateCaseDetailModel(case_, model);
			model.addAttribute("success", "Contact added.");
		} catch (InvalidStateException e) {
			populateCaseDetailModel(case_, model);
			model.addAttribute("error", e.getMessage());
		}
		
		return "paralegal/case-details";
	}
	
	@PostMapping("/paralegal/cases/{id}/tasks")
	public String addTask(@PathVariable("id") Long caseId, @Valid @ModelAttribute AddTaskRequest form, BindingResult bindingResult,
						  Authentication authentication, Model model) {
		Case case_ = caseService.findOwnedByParalegal(caseId, currentParalegal(authentication));
		
		if (bindingResult.hasErrors()) {
			model.addAttribute("error", bindingResult.getAllErrors().getFirst().getDefaultMessage());
			populateCaseDetailModel(case_, model);
			return "paralegal/case-details";
		}
		
		try {
			taskService.add(case_, form.getTitle(), form.getDocumentingTask(), form.getSchedulingTask(), form.getBillingTask(),
							form.getCaseFiles(),
							form.getDate(), form.getStartTime(), form.getEndTime(), form.getLocation(), form.getNotes(),
							form.getBillableAmount());
			populateCaseDetailModel(case_, model);
			model.addAttribute("success", "Task added.");
		} catch (InvalidStateException | BusinessRuleViolationException e) {
			populateCaseDetailModel(case_, model);
			model.addAttribute("error", e.getMessage());
		}
		
		return "paralegal/case-details";
	}
	
	@PostMapping("/paralegal/cases/{id}/tasks/{taskId}/edit-title")
	public String editTaskTitle(@PathVariable("id") Long caseId, @PathVariable Long taskId, @Valid @ModelAttribute RenameTaskRequest form,
								BindingResult bindingResult, Authentication authentication, Model model) {
		Case case_ = caseService.findOwnedByParalegal(caseId, currentParalegal(authentication));
		
		if (bindingResult.hasErrors()) {
			model.addAttribute("error", bindingResult.getAllErrors().getFirst().getDefaultMessage());
			populateCaseDetailModel(case_, model);
			return "paralegal/case-details";
		}
		
		taskService.editTitle(taskService.findOwnedByCase(taskId, case_), form.getTitle());
		
		populateCaseDetailModel(case_, model);
		model.addAttribute("success", "Task renamed.");
		return "paralegal/case-details";
	}
	
	@PostMapping("/paralegal/cases/{id}/tasks/{taskId}/reschedule")
	public String rescheduleTaskEvent(@PathVariable("id") Long caseId, @PathVariable Long taskId, @Valid @ModelAttribute ScheduleMeetingRequest form,
									  BindingResult bindingResult, Authentication authentication, Model model) {
		Case case_ = caseService.findOwnedByParalegal(caseId, currentParalegal(authentication));
		
		if (bindingResult.hasErrors()) {
			model.addAttribute("error", bindingResult.getAllErrors().getFirst().getDefaultMessage());
			populateCaseDetailModel(case_, model);
			return "paralegal/case-details";
		}
		
		try {
			taskService.rescheduleTaskEvent(taskService.findOwnedByCase(taskId, case_), form.getSubject(), form.getDate(), form.getStartTime(), form.getEndTime(), form.getLocation(), form.getNotes());
			populateCaseDetailModel(case_, model);
			model.addAttribute("success", "Event rescheduled successfully.");
		} catch (InvalidStateException | BusinessRuleViolationException e) {
			populateCaseDetailModel(case_, model);
			model.addAttribute("error", e.getMessage());
		}
		
		return "paralegal/case-details";
	}
	
	@PostMapping("/paralegal/cases/{id}/documents/{documentId}/accept")
	public String acceptStagedDocument(@PathVariable("id") Long caseId, @PathVariable Long documentId, Authentication authentication, Model model) {
		Case case_ = caseService.findOwnedByParalegal(caseId, currentParalegal(authentication));
		documentService.addStagedDocumentsToCase(case_, List.of(documentId));
		populateCaseDetailModel(case_, model);
		model.addAttribute("success", "Document added to case.");
		return "paralegal/case-details";
	}
	
	@PostMapping("/paralegal/cases/{id}/documents/{documentId}/reject")
	public String rejectStagedDocument(@PathVariable("id") Long caseId, @PathVariable Long documentId, Authentication authentication, Model model) {
		Case case_ = caseService.findOwnedByParalegal(caseId, currentParalegal(authentication));
		documentService.deleteStagedDocuments(case_, List.of(documentId));
		populateCaseDetailModel(case_, model);
		model.addAttribute("success", "Document deleted.");
		return "paralegal/case-details";
	}
	
	@PostMapping("/paralegal/cases/{id}/documents/{documentId}/rename")
	public String renameCaseDocument(@PathVariable("id") Long caseId, @PathVariable Long documentId, @Valid @ModelAttribute RenameDocumentRequest form,
									 BindingResult bindingResult, Authentication authentication, Model model) {
		Case case_ = caseService.findOwnedByParalegal(caseId, currentParalegal(authentication));
		
		if (bindingResult.hasErrors()) {
			model.addAttribute("error", bindingResult.getAllErrors().getFirst().getDefaultMessage());
			populateCaseDetailModel(case_, model);
			return "paralegal/case-details";
		}
		
		documentService.renameCaseDocument(case_, documentId, form.getOriginalFilename());
		
		populateCaseDetailModel(case_, model);
		model.addAttribute("success", "Document renamed.");
		return "paralegal/case-details";
	}
	
	@GetMapping("/paralegal/documents/case/{id}")
	public ResponseEntity<Resource> viewCaseDocument(@PathVariable("id") Long documentId, Authentication authentication) {
		return documentService.streamCaseDocument(documentService.findById(documentId), currentParalegal(authentication));
	}
	
	// ==================== Schedule (My Events) ====================
	
	@GetMapping("/paralegal/events")
	public String events(Authentication authentication, Model model) {
		model.addAttribute("events", eventService.findAllForParalegal(currentParalegal(authentication)));
		return "paralegal/event-list";
	}
	
	@GetMapping("/paralegal/events/{id}")
	public String eventDetails(@PathVariable("id") Long eventId, Authentication authentication, Model model) {
		model.addAttribute("event", eventService.findOwnedByParalegal(eventId, currentParalegal(authentication)));
		return "paralegal/event-details";
	}
	
	@GetMapping("/paralegal/analytics")
	public String analytics(Authentication authentication, Model model) {
		Paralegal paralegal = currentParalegal(authentication);
		model.addAttribute("user", paralegal);
		
		model.addAttribute("winRate", analyticsService.winRateForParalegal(paralegal.getUserId()));
		model.addAttribute("avgCaseDurationDays", analyticsService.avgCaseDurationDaysForParalegal(paralegal.getUserId()));
		model.addAttribute("billableThisMonth", analyticsService.billableAmountThisMonthForParalegal(paralegal.getUserId()));
		model.addAttribute("billableLastMonth", analyticsService.billableAmountLastMonthForParalegal(paralegal.getUserId()));
		
		return "paralegal/analytics";
	}
	
}
