package lexflow.controller;

import jakarta.validation.Valid;
import lexflow.dto.case_.AddTaskRequest;
import lexflow.dto.case_.CloseCaseRequest;
import lexflow.dto.case_.RenameTaskRequest;
import lexflow.dto.case_.SaveContactRequest;
import lexflow.dto.consultation.RejectConsultationRequest;
import lexflow.dto.document.RenameDocumentRequest;
import lexflow.dto.scheduling.ScheduleMeetingRequest;
import lexflow.dto.user.ChangePasswordRequest;
import lexflow.entity.case_.Case;
import lexflow.entity.case_.Task;
import lexflow.entity.consultation.Consultation;
import lexflow.entity.scheduling.ConsultationEvent;
import lexflow.entity.scheduling.Event;
import lexflow.entity.user.Associate;
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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class AssociateController {
	
	private final AnalyticsService analyticsService;
	private final CaseService caseService;
	private final ConsultationService consultationService;
	private final ContactService contactService;
	private final DocumentService documentService;
	private final EventService eventService;
	private final TaskService taskService;
	private final UserService userService;
	
	// Overview (Dashboard, Profile)
	
	@GetMapping("/associate/dashboard")
	public String dashboard(Authentication authentication, Model model) {
		model.addAttribute("user", currentAssociate(authentication));
		
		return "associate/dashboard";
	}
	
	// Private Helpers
	
	private Associate currentAssociate(Authentication authentication) {
		CustomUserPrincipal principal = (CustomUserPrincipal) authentication.getPrincipal();
		
		return (Associate) principal.user();
	}
	
	@GetMapping("/associate/profile")
	public String profile(Authentication authentication, Model model) {
		model.addAttribute("user", currentAssociate(authentication));
		
		return "associate/profile";
	}
	
	@PostMapping("/associate/profile/change-password")
	public String changePassword(@Valid @ModelAttribute ChangePasswordRequest form, BindingResult bindingResult, Authentication authentication, Model model) {
		Associate associate = currentAssociate(authentication);
		
		if (bindingResult.hasErrors()) {
			model.addAttribute("error", bindingResult.getAllErrors().getFirst().getDefaultMessage());
			model.addAttribute("user", associate);
			
			return "associate/profile";
		}
		
		if (!userService.verifyPassword(associate, form.getCurrentPassword())) {
			model.addAttribute("user", associate);
			model.addAttribute("error", "Current password is incorrect.");
			
			return "associate/profile";
		}
		
		userService.changePassword(associate, form.getNewPassword());
		
		model.addAttribute("user", associate);
		model.addAttribute("success", "Password changed successfully.");
		
		return "associate/profile";
	}
	
	@GetMapping("/associate/documents/user/{id}")
	public ResponseEntity<Resource> viewUserDocument(@PathVariable("id") Long documentId, Authentication authentication) {
		return documentService.streamUserDocument(documentService.findById(documentId), currentAssociate(authentication));
	}
	
	// Consultations (Pending, All)
	
	@GetMapping("/associate/consultations/pending")
	public String pendingConsultations(Authentication authentication, Model model) {
		model.addAttribute("consultations", consultationService.findAwaitingReviewForAssociate(currentAssociate(authentication)));
		
		return "associate/consultation-pending-list";
	}
	
	@GetMapping("/associate/consultations")
	public String consultations(Authentication authentication, Model model) {
		model.addAttribute("consultations", consultationService.findAllForAssociate(currentAssociate(authentication)));
		
		return "associate/consultation-list";
	}
	
	@GetMapping("/associate/consultations/{id}")
	public String consultationDetails(@PathVariable("id") Long consultationId, Authentication authentication, Model model) {
		Consultation consultation = consultationService.findOwnedByAssociate(consultationId, currentAssociate(authentication));
		populateConsultationDetailModel(consultation, model);
		
		return "associate/consultation-details";
	}
	
	private void populateConsultationDetailModel(Consultation consultation, Model model) {
		model.addAttribute("consultation", consultation);
		model.addAttribute("consultationDocuments", documentService.findByConsultation(consultation));
		model.addAttribute("meetings", eventService.findByConsultation(consultation));
		model.addAttribute("hasCompletedMeeting", consultationService.hasCompletedMeeting(consultation));
		model.addAttribute("existingCase", caseService.findByConsultation(consultation));
	}
	
	@PostMapping("/associate/consultations/{id}/accept")
	public String acceptConsultation(@PathVariable("id") Long consultationId, Authentication authentication, Model model) {
		Consultation consultation = consultationService.findOwnedByAssociate(consultationId, currentAssociate(authentication));
		
		try {
			consultation = consultationService.associateAccept(consultation);
			populateConsultationDetailModel(consultation, model);
			model.addAttribute("success", "Consultation request accepted.");
		} catch (InvalidStateException | BusinessRuleViolationException e) {
			populateConsultationDetailModel(consultation, model);
			model.addAttribute("error", e.getMessage());
		}
		
		return "associate/consultation-details";
	}
	
	@PostMapping("/associate/consultations/{id}/reject")
	public String rejectConsultation(@PathVariable("id") Long consultationId, @Valid @ModelAttribute RejectConsultationRequest form, BindingResult bindingResult, Authentication authentication, Model model) {
		Consultation consultation = consultationService.findOwnedByAssociate(consultationId, currentAssociate(authentication));
		
		if (bindingResult.hasErrors()) {
			model.addAttribute("error", bindingResult.getAllErrors().getFirst().getDefaultMessage());
			populateConsultationDetailModel(consultation, model);
			
			return "associate/consultation-details";
		}
		
		try {
			consultation = consultationService.associateReject(consultation, form.getRejectionReason());
			populateConsultationDetailModel(consultation, model);
			model.addAttribute("success", "Consultation request rejected.");
		} catch (InvalidStateException | BusinessRuleViolationException e) {
			populateConsultationDetailModel(consultation, model);
			model.addAttribute("error", e.getMessage());
		}
		
		return "associate/consultation-details";
	}
	
	@PostMapping("/associate/consultations/{id}/schedule")
	public String scheduleMeeting(@PathVariable("id") Long consultationId, @Valid @ModelAttribute ScheduleMeetingRequest form, BindingResult bindingResult, Authentication authentication, Model model) {
		Consultation consultation = consultationService.findOwnedByAssociate(consultationId, currentAssociate(authentication));
		
		if (bindingResult.hasErrors()) {
			model.addAttribute("error", bindingResult.getAllErrors().getFirst().getDefaultMessage());
			populateConsultationDetailModel(consultation, model);
			
			return "associate/consultation-details";
		}
		
		try {
			eventService.schedule(consultation, form.getSubject(), form.getDate(), form.getStartTime(), form.getEndTime(), form.getLocation(), form.getNotes());
			populateConsultationDetailModel(consultation, model);
			model.addAttribute("success", "Meeting scheduled successfully.");
		} catch (InvalidStateException | BusinessRuleViolationException e) {
			populateConsultationDetailModel(consultation, model);
			model.addAttribute("error", e.getMessage());
		}
		
		return "associate/consultation-details";
	}
	
	@PostMapping("/associate/consultations/{id}/events/{eventId}/reschedule")
	public String rescheduleMeeting(@PathVariable("id") Long consultationId, @PathVariable Long eventId, @Valid @ModelAttribute ScheduleMeetingRequest form, BindingResult bindingResult, Authentication authentication, Model model) {
		Consultation consultation = consultationService.findOwnedByAssociate(consultationId, currentAssociate(authentication));
		
		if (bindingResult.hasErrors()) {
			model.addAttribute("error", bindingResult.getAllErrors().getFirst().getDefaultMessage());
			populateConsultationDetailModel(consultation, model);
			
			return "associate/consultation-details";
		}
		
		ConsultationEvent event = eventService.findOwnedByConsultation(eventId, consultation);
		
		try {
			eventService.reschedule(event, form.getSubject(), form.getDate(), form.getStartTime(), form.getEndTime(), form.getLocation(), form.getNotes());
			populateConsultationDetailModel(consultation, model);
			model.addAttribute("success", "Meeting rescheduled successfully.");
		} catch (InvalidStateException | BusinessRuleViolationException e) {
			populateConsultationDetailModel(consultation, model);
			model.addAttribute("error", e.getMessage());
		}
		
		return "associate/consultation-details";
	}
	
	@PostMapping("/associate/consultations/{id}/events/{eventId}/complete")
	public String completeMeeting(@PathVariable("id") Long consultationId, @PathVariable Long eventId, Authentication authentication, Model model) {
		Consultation consultation = consultationService.findOwnedByAssociate(consultationId, currentAssociate(authentication));
		
		try {
			eventService.complete(eventService.findOwnedByConsultation(eventId, consultation));
			populateConsultationDetailModel(consultation, model);
			model.addAttribute("success", "Meeting marked as completed.");
		} catch (InvalidStateException | BusinessRuleViolationException e) {
			populateConsultationDetailModel(consultation, model);
			model.addAttribute("error", e.getMessage());
		}
		
		return "associate/consultation-details";
	}
	
	@PostMapping("/associate/consultations/{id}/events/{eventId}/cancel")
	public String cancelMeeting(@PathVariable("id") Long consultationId, @PathVariable Long eventId, Authentication authentication, Model model) {
		Consultation consultation = consultationService.findOwnedByAssociate(consultationId, currentAssociate(authentication));
		
		try {
			eventService.cancel(eventService.findOwnedByConsultation(eventId, consultation));
			populateConsultationDetailModel(consultation, model);
			model.addAttribute("success", "Meeting canceled.");
		} catch (InvalidStateException e) {
			populateConsultationDetailModel(consultation, model);
			model.addAttribute("error", e.getMessage());
		}
		
		return "associate/consultation-details";
	}
	
	@PostMapping("/associate/consultations/{id}/open-case")
	public String openCase(@PathVariable("id") Long consultationId, Authentication authentication, RedirectAttributes redirectAttributes) {
		Consultation consultation = consultationService.findOwnedByAssociate(consultationId, currentAssociate(authentication));
		Case case_ = caseService.open(consultation);
		
		redirectAttributes.addFlashAttribute("success", "Case opened successfully.");
		
		return "redirect:/associate/cases/" + case_.getCaseId();
	}
	
	// Documents
	
	@GetMapping("/associate/documents/consultation/{id}")
	public ResponseEntity<Resource> viewConsultationDocument(@PathVariable("id") Long documentId, Authentication authentication) {
		return documentService.streamConsultationDocument(documentService.findById(documentId), currentAssociate(authentication));
	}
	
	// Cases (Open, Closed)
	
	@GetMapping("/associate/cases/open")
	public String openCases(Authentication authentication, Model model) {
		model.addAttribute("cases", caseService.findOpenForAssociate(currentAssociate(authentication)));
		model.addAttribute("title", "Open Cases");
		model.addAttribute("active", "cases-open");
		
		return "associate/case-list";
	}
	
	@GetMapping("/associate/cases/closed")
	public String closedCases(Authentication authentication, Model model) {
		model.addAttribute("cases", caseService.findClosedForAssociate(currentAssociate(authentication)));
		model.addAttribute("title", "Closed Cases");
		model.addAttribute("active", "cases-closed");
		
		return "associate/case-list";
	}
	
	@GetMapping("/associate/cases/{id}")
	public String caseDetails(@PathVariable("id") Long caseId, @RequestParam(required = false) String documentKeyword, Authentication authentication, Model model) {
		Case case_ = caseService.findOwnedByAssociate(caseId, currentAssociate(authentication));
		populateCaseDetailModel(case_, model);
		
		model.addAttribute("caseDocuments", documentService.findByCaseAndKeyword(case_, documentKeyword));
		model.addAttribute("documentKeyword", documentKeyword);
		
		return "associate/case-details";
	}
	
	private void populateCaseDetailModel(Case case_, Model model) {
		model.addAttribute("case", case_);
		model.addAttribute("stagedDocuments", documentService.findStagedByCase(case_));
		model.addAttribute("caseDocuments", documentService.findByCase(case_));
		model.addAttribute("contacts", contactService.findByCase(case_));
		
		List<Task> tasks = taskService.findByCase(case_);
		model.addAttribute("tasks", tasks);
		model.addAttribute("taskDocuments", documentService.findByTasks(tasks));
	}
	
	@PostMapping("/associate/cases/{id}/close")
	public String closeCase(@PathVariable("id") Long caseId, @Valid @ModelAttribute CloseCaseRequest form, BindingResult bindingResult, Authentication authentication, Model model) {
		Case case_ = caseService.findOwnedByAssociate(caseId, currentAssociate(authentication));
		
		if (bindingResult.hasErrors()) {
			model.addAttribute("error", bindingResult.getAllErrors().getFirst().getDefaultMessage());
			populateCaseDetailModel(case_, model);
			
			return "associate/case-details";
		}
		
		try {
			case_ = caseService.close(case_, form.getCaseResolutionStatus());
			populateCaseDetailModel(case_, model);
			model.addAttribute("success", "Case closed successfully.");
		} catch (InvalidStateException e) {
			populateCaseDetailModel(case_, model);
			model.addAttribute("error", e.getMessage());
		}
		
		return "associate/case-details";
	}
	
	@PostMapping("/associate/cases/{id}/documents/{documentId}/accept")
	public String acceptStagedDocument(@PathVariable("id") Long caseId, @PathVariable Long documentId, Authentication authentication, Model model) {
		Case case_ = caseService.findOwnedByAssociate(caseId, currentAssociate(authentication));
		documentService.addStagedToCase(case_, List.of(documentId));
		
		populateCaseDetailModel(case_, model);
		model.addAttribute("success", "Document added to case.");
		
		return "associate/case-details";
	}
	
	@PostMapping("/associate/cases/{id}/documents/{documentId}/reject")
	public String rejectStagedDocument(@PathVariable("id") Long caseId, @PathVariable Long documentId, Authentication authentication, Model model) {
		Case case_ = caseService.findOwnedByAssociate(caseId, currentAssociate(authentication));
		documentService.deleteStaged(case_, List.of(documentId));
		
		populateCaseDetailModel(case_, model);
		model.addAttribute("success", "Document deleted.");
		
		return "associate/case-details";
	}
	
	@PostMapping("/associate/cases/{id}/documents/{documentId}/rename")
	public String renameCaseDocument(@PathVariable("id") Long caseId, @PathVariable Long documentId, @Valid @ModelAttribute RenameDocumentRequest form, BindingResult bindingResult, Authentication authentication, Model model) {
		Case case_ = caseService.findOwnedByAssociate(caseId, currentAssociate(authentication));
		
		if (bindingResult.hasErrors()) {
			model.addAttribute("error", bindingResult.getAllErrors().getFirst().getDefaultMessage());
			populateCaseDetailModel(case_, model);
			
			return "associate/case-details";
		}
		
		documentService.rename(case_, documentId, form.getOriginalFilename());
		
		populateCaseDetailModel(case_, model);
		model.addAttribute("success", "Document renamed.");
		
		return "associate/case-details";
	}
	
	@PostMapping("/associate/cases/{id}/contacts")
	public String addContact(@PathVariable("id") Long caseId, @Valid @ModelAttribute SaveContactRequest form, BindingResult bindingResult, Authentication authentication, Model model) {
		Case case_ = caseService.findOwnedByAssociate(caseId, currentAssociate(authentication));
		
		if (bindingResult.hasErrors()) {
			model.addAttribute("error", bindingResult.getAllErrors().getFirst().getDefaultMessage());
			populateCaseDetailModel(case_, model);
			
			return "associate/case-details";
		}
		
		try {
			contactService.add(case_, form.getName(), form.getPhoneNumbers(), form.getEmails(), form.getAddresses(), form.getNotes());
			populateCaseDetailModel(case_, model);
			model.addAttribute("success", "Contact added.");
		} catch (InvalidStateException e) {
			populateCaseDetailModel(case_, model);
			model.addAttribute("error", e.getMessage());
		}
		
		return "associate/case-details";
	}
	
	@PostMapping("/associate/cases/{id}/tasks")
	public String addTask(@PathVariable("id") Long caseId, @Valid @ModelAttribute AddTaskRequest form, BindingResult bindingResult, Authentication authentication, Model model) {
		Case case_ = caseService.findOwnedByAssociate(caseId, currentAssociate(authentication));
		
		if (bindingResult.hasErrors()) {
			model.addAttribute("error", bindingResult.getAllErrors().getFirst().getDefaultMessage());
			populateCaseDetailModel(case_, model);
			
			return "associate/case-details";
		}
		
		try {
			taskService.add(case_, form.getTitle(), form.getDocumentingTask(), form.getSchedulingTask(), form.getBillingTask(), form.getCaseFiles(), form.getDate(), form.getStartTime(), form.getEndTime(), form.getLocation(), form.getNotes(), form.getBillableAmount());
			populateCaseDetailModel(case_, model);
			model.addAttribute("success", "Task added.");
		} catch (InvalidStateException | BusinessRuleViolationException e) {
			populateCaseDetailModel(case_, model);
			model.addAttribute("error", e.getMessage());
		}
		
		return "associate/case-details";
	}
	
	@PostMapping("/associate/cases/{id}/tasks/{taskId}/edit-title")
	public String editTaskTitle(@PathVariable("id") Long caseId, @PathVariable Long taskId, @Valid @ModelAttribute RenameTaskRequest form, BindingResult bindingResult, Authentication authentication, Model model) {
		Case case_ = caseService.findOwnedByAssociate(caseId, currentAssociate(authentication));
		
		if (bindingResult.hasErrors()) {
			model.addAttribute("error", bindingResult.getAllErrors().getFirst().getDefaultMessage());
			populateCaseDetailModel(case_, model);
			
			return "associate/case-details";
		}
		
		taskService.updateTitle(taskService.findOwnedByCase(taskId, case_), form.getTitle());
		
		populateCaseDetailModel(case_, model);
		model.addAttribute("success", "Task renamed.");
		
		return "associate/case-details";
	}
	
	@PostMapping("/associate/cases/{id}/tasks/{taskId}/reschedule")
	public String rescheduleTaskEvent(@PathVariable("id") Long caseId, @PathVariable Long taskId, @Valid @ModelAttribute ScheduleMeetingRequest form, BindingResult bindingResult, Authentication authentication, Model model) {
		Case case_ = caseService.findOwnedByAssociate(caseId, currentAssociate(authentication));
		
		if (bindingResult.hasErrors()) {
			model.addAttribute("error", bindingResult.getAllErrors().getFirst().getDefaultMessage());
			populateCaseDetailModel(case_, model);
			
			return "associate/case-details";
		}
		
		try {
			taskService.rescheduleEvent(taskService.findOwnedByCase(taskId, case_), form.getSubject(), form.getDate(), form.getStartTime(), form.getEndTime(), form.getLocation(), form.getNotes());
			populateCaseDetailModel(case_, model);
			model.addAttribute("success", "Event rescheduled successfully.");
		} catch (InvalidStateException | BusinessRuleViolationException e) {
			populateCaseDetailModel(case_, model);
			model.addAttribute("error", e.getMessage());
		}
		
		return "associate/case-details";
	}
	
	@GetMapping("/associate/documents/case/{id}")
	public ResponseEntity<Resource> viewCaseDocument(@PathVariable("id") Long documentId, Authentication authentication) {
		return documentService.streamCaseDocument(documentService.findById(documentId), currentAssociate(authentication));
	}
	
	// Schedule (My Events)
	
	@GetMapping("/associate/events")
	public String events(Authentication authentication, Model model) {
		model.addAttribute("events", eventService.findAllForAssociate(currentAssociate(authentication)));
		
		return "associate/event-list";
	}
	
	@GetMapping("/associate/events/{id}")
	public String eventDetails(@PathVariable("id") Long eventId, Authentication authentication, Model model) {
		model.addAttribute("event", eventService.findOwnedByAssociate(eventId, currentAssociate(authentication)));
		
		return "associate/event-details";
	}
	
	@PostMapping("/associate/events/{id}/complete")
	public String completeEvent(@PathVariable("id") Long eventId, Authentication authentication, RedirectAttributes redirectAttributes) {
		Event event = eventService.findOwnedByAssociate(eventId, currentAssociate(authentication));
		
		try {
			eventService.complete(event);
			redirectAttributes.addFlashAttribute("success", "Event marked as completed.");
		} catch (InvalidStateException | BusinessRuleViolationException e) {
			redirectAttributes.addFlashAttribute("error", e.getMessage());
		}
		
		return "redirect:/associate/events/{id}";
	}
	
	@PostMapping("/associate/events/{id}/cancel")
	public String cancelEvent(@PathVariable("id") Long eventId, Authentication authentication, RedirectAttributes redirectAttributes) {
		Event event = eventService.findOwnedByAssociate(eventId, currentAssociate(authentication));
		
		try {
			eventService.cancel(event);
			redirectAttributes.addFlashAttribute("success", "Event canceled.");
		} catch (InvalidStateException e) {
			redirectAttributes.addFlashAttribute("error", e.getMessage());
		}
		
		return "redirect:/associate/events/{id}";
	}
	
	@GetMapping("/associate/analytics")
	public String analytics(Authentication authentication, Model model) {
		Associate associate = currentAssociate(authentication);
		model.addAttribute("user", associate);
		
		model.addAttribute("winRate", analyticsService.winRateForAssociate(associate.getUserId()));
		model.addAttribute("averageCaseDurationDays", analyticsService.averageCaseDurationDaysForAssociate(associate.getUserId()));
		model.addAttribute("billableAmountThisMonth", analyticsService.billableAmountThisMonthForAssociate(associate.getUserId()));
		model.addAttribute("billableAmountLastMonth", analyticsService.billableAmountLastMonthForAssociate(associate.getUserId()));
		
		return "associate/analytics";
	}
	
}
