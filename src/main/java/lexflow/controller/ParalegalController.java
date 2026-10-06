package lexflow.controller;

import lexflow.dto.document.SelectDocumentsRequest;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import lexflow.entity.scheduling.Event;
import lexflow.dto.scheduling.ScheduleMeetingRequest;
import lexflow.util.ValidationUtil;
import jakarta.validation.Valid;
import lexflow.dto.case_.AddTaskRequest;
import lexflow.dto.case_.EditTaskRequest;
import lexflow.dto.case_.SaveContactRequest;
import lexflow.dto.document.RenameDocumentRequest;
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
	
	// Overview
	
	@GetMapping("/paralegal/dashboard")
	public String dashboard(Authentication authentication, Model model) {
		model.addAttribute("user", currentParalegal(authentication));
		
		return "paralegal/dashboard";
	}
	
	@GetMapping("/paralegal/analytics")
	public String analytics(Authentication authentication, Model model) {
		Paralegal paralegal = currentParalegal(authentication);
		model.addAttribute("user", paralegal);
		
		model.addAttribute("winRate", analyticsService.winRateForParalegal(paralegal.getUserId()));
		model.addAttribute("averageCaseDurationDays", analyticsService.averageCaseDurationDaysForParalegal(paralegal.getUserId()));
		model.addAttribute("billableAmountThisMonth", analyticsService.billableAmountThisMonthForParalegal(paralegal.getUserId()));
		model.addAttribute("billableAmountLastMonth", analyticsService.billableAmountLastMonthForParalegal(paralegal.getUserId()));
		
		return "paralegal/analytics";
	}
	
	@GetMapping("/paralegal/profile")
	public String profile(Authentication authentication, Model model) {
		model.addAttribute("user", currentParalegal(authentication));
		
		return "paralegal/profile";
	}
	
	@PostMapping("/paralegal/profile/change-password")
	public String changePassword(Authentication authentication, @Valid @ModelAttribute ChangePasswordRequest form, BindingResult bindingResult, Model model, RedirectAttributes redirectAttributes) {
		User user = currentParalegal(authentication);
		
		if (bindingResult.hasErrors()) {
			model.addAttribute("error", ValidationUtil.allErrors(bindingResult));
			model.addAttribute("user", user);
			
			return "paralegal/profile";
		}
		
		if (!userService.verifyPassword(user, form.getCurrentPassword())) {
			model.addAttribute("user", user);
			model.addAttribute("error", "Current password is incorrect.");
			
			return "paralegal/profile";
		}
		
		userService.changePassword(user, form.getNewPassword());
		
		redirectAttributes.addFlashAttribute("success", "Password changed successfully.");
		
		return "redirect:/paralegal/profile";
	}
	
	// Consultations
	
	@GetMapping("/paralegal/consultations/pending")
	public String pendingConsultations(Authentication authentication, Model model) {
		model.addAttribute("consultations", consultationService.findAwaitingReviewForParalegal(currentParalegal(authentication)));
		
		return "paralegal/consultation-pending-list";
	}
	
	@GetMapping("/paralegal/consultations")
	public String consultations(Authentication authentication, Model model) {
		model.addAttribute("consultations", consultationService.findAllForParalegal(currentParalegal(authentication)));
		model.addAttribute("title", "Consultation Requests");
		model.addAttribute("active", "consultations");
		
		return "paralegal/consultation-list";
	}
	
	@GetMapping("/paralegal/consultations/{id}")
	public String consultationDetails(@PathVariable("id") Long consultationId, Authentication authentication, Model model) {
		Consultation consultation = consultationService.findOwnedByParalegal(consultationId, currentParalegal(authentication));
		model.addAttribute("consultation", consultation);
		model.addAttribute("consultationDocuments", documentService.findByConsultation(consultation));
		model.addAttribute("existingCase", caseService.findByConsultation(consultation));
		
		return "paralegal/consultation-details";
	}
	
	// Cases
	
	@GetMapping("/paralegal/cases/to-open")
	public String casesToOpen(Authentication authentication, Model model) {
		model.addAttribute("consultations", consultationService.findAwaitingCaseForParalegal(currentParalegal(authentication)));
		model.addAttribute("title", "Cases to Open");
		model.addAttribute("active", "cases-to-open");

		return "paralegal/consultation-list";
	}

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
	public String caseDetails(@PathVariable("id") Long caseId, @RequestParam(required = false) String documentKeyword, Authentication authentication, Model model) {
		Case case_ = caseService.findOwnedByParalegal(caseId, currentParalegal(authentication));
		populateCaseDetailModel(case_, model);
		
		model.addAttribute("caseDocuments", documentService.findByCaseAndKeyword(case_, documentKeyword));
		model.addAttribute("documentKeyword", documentKeyword);
		
		return "paralegal/case-details";
	}
	
	@PostMapping("/paralegal/cases/{id}/tasks")
	public String addTask(@PathVariable("id") Long caseId, @Valid @ModelAttribute AddTaskRequest form, BindingResult bindingResult, Authentication authentication, Model model, RedirectAttributes redirectAttributes) {
		Case case_ = caseService.findOwnedByParalegal(caseId, currentParalegal(authentication));
		
		if (bindingResult.hasErrors()) {
			model.addAttribute("error", ValidationUtil.allErrors(bindingResult));
			populateCaseDetailModel(case_, model);
			
			return "paralegal/case-details";
		}
		
		try {
			taskService.add(case_, form.getTitle(), form.getDocumentingTask(), form.getSchedulingTask(), form.getBillingTask(), form.getCaseFiles(), form.getDate(), form.getStartTime(), form.getEndTime(), form.getLocation(), form.getNotes(), form.getBillableAmount());
		} catch (InvalidStateException | BusinessRuleViolationException e) {
			populateCaseDetailModel(case_, model);
			model.addAttribute("error", e.getMessage());
			
			return "paralegal/case-details";
		}
		
		redirectAttributes.addFlashAttribute("success", "Task added successfully.");
		
		return "redirect:/paralegal/cases/{id}";
	}
	
	@PostMapping("/paralegal/cases/{id}/tasks/{taskId}/edit")
	public String editTask(@PathVariable("id") Long caseId, @PathVariable Long taskId, @Valid @ModelAttribute EditTaskRequest form, BindingResult bindingResult, Authentication authentication, Model model, RedirectAttributes redirectAttributes) {
		Case case_ = caseService.findOwnedByParalegal(caseId, currentParalegal(authentication));
		
		if (bindingResult.hasErrors()) {
			model.addAttribute("error", ValidationUtil.allErrors(bindingResult));
			populateCaseDetailModel(case_, model);
			
			return "paralegal/case-details";
		}
		
		try {
			taskService.update(case_, taskService.findOwnedByCase(taskId, case_), form.getTitle(), form.getDocumentingTask(), form.getSchedulingTask(), form.getBillingTask(), form.getCaseFiles(), form.getDate(), form.getStartTime(), form.getEndTime(), form.getLocation(), form.getNotes(), form.getBillableAmount());
		} catch (InvalidStateException | BusinessRuleViolationException e) {
			populateCaseDetailModel(case_, model);
			model.addAttribute("error", e.getMessage());
			
			return "paralegal/case-details";
		}
		
		redirectAttributes.addFlashAttribute("success", "Task updated successfully.");
		
		return "redirect:/paralegal/cases/{id}";
	}
	
	@PostMapping("/paralegal/cases/{id}/tasks/{taskId}/delete")
	public String deleteTask(@PathVariable("id") Long caseId, @PathVariable Long taskId, Authentication authentication, Model model, RedirectAttributes redirectAttributes) {
		Case case_ = caseService.findOwnedByParalegal(caseId, currentParalegal(authentication));
		
		try {
			taskService.delete(case_, taskService.findOwnedByCase(taskId, case_));
		} catch (InvalidStateException | BusinessRuleViolationException e) {
			populateCaseDetailModel(case_, model);
			model.addAttribute("error", e.getMessage());
			
			return "paralegal/case-details";
		}
		
		redirectAttributes.addFlashAttribute("success", "Task deleted successfully.");
		
		return "redirect:/paralegal/cases/{id}";
	}
	
	@PostMapping("/paralegal/cases/{id}/documents/{documentId}/accept")
	public String acceptStagedDocument(@PathVariable("id") Long caseId, @PathVariable Long documentId, Authentication authentication, RedirectAttributes redirectAttributes) {
		Case case_ = caseService.findOwnedByParalegal(caseId, currentParalegal(authentication));
		documentService.addStagedToCase(case_, List.of(documentId));
		redirectAttributes.addFlashAttribute("success", "Document added to case successfully.");
		
		return "redirect:/paralegal/cases/{id}";
	}
	
	@PostMapping("/paralegal/cases/{id}/documents/{documentId}/rename")
	public String renameCaseDocument(@PathVariable("id") Long caseId, @PathVariable Long documentId, @Valid @ModelAttribute RenameDocumentRequest form, BindingResult bindingResult, Authentication authentication, Model model, RedirectAttributes redirectAttributes) {
		Case case_ = caseService.findOwnedByParalegal(caseId, currentParalegal(authentication));
		
		if (bindingResult.hasErrors()) {
			model.addAttribute("error", ValidationUtil.allErrors(bindingResult));
			populateCaseDetailModel(case_, model);
			
			return "paralegal/case-details";
		}
		
		documentService.rename(case_, documentId, form.getOriginalFilename());
		
		redirectAttributes.addFlashAttribute("success", "Document renamed successfully.");
		
		return "redirect:/paralegal/cases/{id}";
	}

	@PostMapping("/paralegal/cases/{id}/documents/{documentId}/delete")
	public String deleteCaseDocument(@PathVariable("id") Long caseId, @PathVariable Long documentId, Authentication authentication, RedirectAttributes redirectAttributes) {
		Case case_ = caseService.findOwnedByParalegal(caseId, currentParalegal(authentication));
		documentService.deleteStaged(case_, List.of(documentId));

		redirectAttributes.addFlashAttribute("success", "Document deleted successfully.");

		return "redirect:/paralegal/cases/{id}";
	}
	
	@PostMapping("/paralegal/cases/{id}/documents/{documentId}/reject")
	public String rejectStagedDocument(@PathVariable("id") Long caseId, @PathVariable Long documentId, Authentication authentication, RedirectAttributes redirectAttributes) {
		Case case_ = caseService.findOwnedByParalegal(caseId, currentParalegal(authentication));
		documentService.deleteStaged(case_, List.of(documentId));
		redirectAttributes.addFlashAttribute("success", "Document deleted successfully.");
		
		return "redirect:/paralegal/cases/{id}";
	}

	@PostMapping("/paralegal/cases/{id}/documents/accept-selected")
	public String acceptStagedDocuments(@PathVariable("id") Long caseId, @Valid @ModelAttribute SelectDocumentsRequest form, BindingResult bindingResult, Authentication authentication, Model model, RedirectAttributes redirectAttributes) {
		Case case_ = caseService.findOwnedByParalegal(caseId, currentParalegal(authentication));

		if (bindingResult.hasErrors()) {
			model.addAttribute("error", ValidationUtil.allErrors(bindingResult));
			populateCaseDetailModel(case_, model);

			return "paralegal/case-details";
		}

		documentService.addStagedToCase(case_, form.getDocumentIds());

		redirectAttributes.addFlashAttribute("success", "Documents added to case successfully.");

		return "redirect:/paralegal/cases/{id}";
	}

	@PostMapping("/paralegal/cases/{id}/documents/reject-selected")
	public String rejectStagedDocuments(@PathVariable("id") Long caseId, @Valid @ModelAttribute SelectDocumentsRequest form, BindingResult bindingResult, Authentication authentication, Model model, RedirectAttributes redirectAttributes) {
		Case case_ = caseService.findOwnedByParalegal(caseId, currentParalegal(authentication));

		if (bindingResult.hasErrors()) {
			model.addAttribute("error", ValidationUtil.allErrors(bindingResult));
			populateCaseDetailModel(case_, model);

			return "paralegal/case-details";
		}

		documentService.deleteStaged(case_, form.getDocumentIds());

		redirectAttributes.addFlashAttribute("success", "Documents deleted successfully.");

		return "redirect:/paralegal/cases/{id}";
	}
	
	@PostMapping("/paralegal/cases/{id}/contacts")
	public String addContact(@PathVariable("id") Long caseId, @Valid @ModelAttribute SaveContactRequest form, BindingResult bindingResult, Authentication authentication, Model model, RedirectAttributes redirectAttributes) {
		Case case_ = caseService.findOwnedByParalegal(caseId, currentParalegal(authentication));
		
		if (bindingResult.hasErrors()) {
			model.addAttribute("error", ValidationUtil.allErrors(bindingResult));
			populateCaseDetailModel(case_, model);
			
			return "paralegal/case-details";
		}
		
		try {
			contactService.add(case_, form.getName(), form.getPhoneNumbers(), form.getEmails(), form.getAddresses(), form.getNotes());
		} catch (InvalidStateException e) {
			populateCaseDetailModel(case_, model);
			model.addAttribute("error", e.getMessage());
			
			return "paralegal/case-details";
		}
		
		redirectAttributes.addFlashAttribute("success", "Contact added successfully.");
		
		return "redirect:/paralegal/cases/{id}";
	}
	
	@PostMapping("/paralegal/cases/{id}/contacts/{contactId}/edit")
	public String editContact(@PathVariable("id") Long caseId, @PathVariable Long contactId, @Valid @ModelAttribute SaveContactRequest form, BindingResult bindingResult, Authentication authentication, Model model, RedirectAttributes redirectAttributes) {
		Case case_ = caseService.findOwnedByParalegal(caseId, currentParalegal(authentication));
		
		if (bindingResult.hasErrors()) {
			model.addAttribute("error", ValidationUtil.allErrors(bindingResult));
			populateCaseDetailModel(case_, model);
			
			return "paralegal/case-details";
		}
		
		try {
			contactService.update(case_, contactService.findOwnedByCase(contactId, case_), form.getName(), form.getPhoneNumbers(), form.getEmails(), form.getAddresses(), form.getNotes());
		} catch (InvalidStateException e) {
			populateCaseDetailModel(case_, model);
			model.addAttribute("error", e.getMessage());
			
			return "paralegal/case-details";
		}
		
		redirectAttributes.addFlashAttribute("success", "Contact updated successfully.");
		
		return "redirect:/paralegal/cases/{id}";
	}
	
	@PostMapping("/paralegal/cases/{id}/contacts/{contactId}/delete")
	public String deleteContact(@PathVariable("id") Long caseId, @PathVariable Long contactId, Authentication authentication, Model model, RedirectAttributes redirectAttributes) {
		Case case_ = caseService.findOwnedByParalegal(caseId, currentParalegal(authentication));
		
		try {
			contactService.delete(case_, contactService.findOwnedByCase(contactId, case_));
		} catch (InvalidStateException e) {
			populateCaseDetailModel(case_, model);
			model.addAttribute("error", e.getMessage());
			
			return "paralegal/case-details";
		}
		
		redirectAttributes.addFlashAttribute("success", "Contact deleted successfully.");
		
		return "redirect:/paralegal/cases/{id}";
	}
	
	// Schedule
	
	@GetMapping("/paralegal/events/upcoming")
	public String upcomingEvents(Authentication authentication, Model model) {
		model.addAttribute("events", eventService.upcoming(eventService.findAllForParalegal(currentParalegal(authentication))));
		model.addAttribute("title", "Upcoming Events");
		model.addAttribute("active", "events-upcoming");
		
		return "paralegal/event-list";
	}
	
	@GetMapping("/paralegal/events/past")
	public String pastEvents(Authentication authentication, Model model) {
		model.addAttribute("events", eventService.past(eventService.findAllForParalegal(currentParalegal(authentication))));
		model.addAttribute("title", "Past Events");
		model.addAttribute("active", "events-past");
		
		return "paralegal/event-list";
	}
	
	@GetMapping("/paralegal/events/{id}")
	public String eventDetails(@PathVariable("id") Long eventId, Authentication authentication, Model model) {
		model.addAttribute("event", eventService.findOwnedByParalegal(eventId, currentParalegal(authentication)));
		
		return "paralegal/event-details";
	}

	@PostMapping("/paralegal/events/{id}/reschedule")
	public String rescheduleEvent(@PathVariable("id") Long eventId, @Valid @ModelAttribute ScheduleMeetingRequest form, BindingResult bindingResult, Authentication authentication, Model model, RedirectAttributes redirectAttributes) {
		Event event = eventService.findOwnedByParalegal(eventId, currentParalegal(authentication));
		
		if (bindingResult.hasErrors()) {
			model.addAttribute("event", event);
			model.addAttribute("error", ValidationUtil.allErrors(bindingResult));
			
			return "paralegal/event-details";
		}
		
		try {
			eventService.reschedule(event, form.getSubject(), form.getDate(), form.getStartTime(), form.getEndTime(), form.getLocation(), form.getNotes());
		} catch (InvalidStateException | BusinessRuleViolationException e) {
			model.addAttribute("event", event);
			model.addAttribute("error", e.getMessage());
			
			return "paralegal/event-details";
		}
		
		redirectAttributes.addFlashAttribute("success", "Event rescheduled successfully.");
		
		return "redirect:/paralegal/events/{id}";
	}
	
	// Documents
	
	@GetMapping({"/paralegal/documents/user/{id}", "/paralegal/documents/user/{id}/{filename}"})
	public ResponseEntity<Resource> viewUserDocument(@PathVariable("id") Long documentId, Authentication authentication) {
		return documentService.streamUserDocument(documentService.findById(documentId), currentParalegal(authentication));
	}
	
	@GetMapping({"/paralegal/documents/consultation/{id}", "/paralegal/documents/consultation/{id}/{filename}"})
	public ResponseEntity<Resource> viewConsultationDocument(@PathVariable("id") Long documentId, Authentication authentication) {
		return documentService.streamConsultationDocument(documentService.findById(documentId), currentParalegal(authentication));
	}
	
	@GetMapping({"/paralegal/documents/case/{id}", "/paralegal/documents/case/{id}/{filename}"})
	public ResponseEntity<Resource> viewCaseDocument(@PathVariable("id") Long documentId, Authentication authentication) {
		return documentService.streamCaseDocument(documentService.findById(documentId), currentParalegal(authentication));
	}
	
	// Private Helpers
	
	private Paralegal currentParalegal(Authentication authentication) {
		CustomUserPrincipal principal = (CustomUserPrincipal) authentication.getPrincipal();
		
		return (Paralegal) principal.user();
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
	
}
