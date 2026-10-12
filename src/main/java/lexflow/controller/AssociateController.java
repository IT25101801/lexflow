package lexflow.controller;

import jakarta.validation.Valid;
import lexflow.dto.case_.AddTaskRequest;
import lexflow.dto.case_.CloseCaseRequest;
import lexflow.dto.case_.EditTaskRequest;
import lexflow.dto.case_.SaveContactRequest;
import lexflow.dto.consultation.RejectConsultationRequest;
import lexflow.dto.document.RenameDocumentRequest;
import lexflow.dto.document.SelectDocumentsRequest;
import lexflow.dto.scheduling.ScheduleMeetingRequest;
import lexflow.dto.user.ChangePasswordRequest;
import lexflow.entity.case_.Case;
import lexflow.entity.case_.Task;
import lexflow.entity.consultation.Consultation;
import lexflow.entity.scheduling.Event;
import lexflow.entity.user.Associate;
import lexflow.exception.BusinessRuleViolationException;
import lexflow.exception.InvalidStateException;
import lexflow.security.CustomUserPrincipal;
import lexflow.service.*;
import lexflow.util.ValidationUtil;
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
	
	private final CaseService caseService;
	private final ConsultationService consultationService;
	private final ContactService contactService;
	private final DocumentService documentService;
	private final EventService eventService;
	private final TaskService taskService;
	private final UserService userService;
	
	// Overview
	
	@GetMapping("/associate/dashboard")
	public String dashboard(Authentication authentication, Model model) {
		model.addAttribute("user", currentAssociate(authentication));
		
		return "associate/dashboard";
	}
	
	@GetMapping("/associate/profile")
	public String profile(Authentication authentication, Model model) {
		model.addAttribute("user", currentAssociate(authentication));
		
		return "associate/profile";
	}
	
	@PostMapping("/associate/profile/change-password")
	public String changePassword(@Valid @ModelAttribute ChangePasswordRequest form, BindingResult bindingResult, Authentication authentication, Model model, RedirectAttributes redirectAttributes) {
		Associate associate = currentAssociate(authentication);
		
		if (bindingResult.hasErrors()) {
			model.addAttribute("error", ValidationUtil.allErrors(bindingResult));
			model.addAttribute("user", associate);
			
			return "associate/profile";
		}
		
		if (!userService.verifyPassword(associate, form.getCurrentPassword())) {
			model.addAttribute("user", associate);
			model.addAttribute("error", "Current password is incorrect.");
			
			return "associate/profile";
		}
		
		userService.changePassword(associate, form.getNewPassword());
		
		redirectAttributes.addFlashAttribute("success", "Password changed successfully.");
		
		return "redirect:/associate/profile";
	}
	
	// Consultations
	
	@GetMapping("/associate/consultations/pending")
	public String pendingConsultations(Authentication authentication, Model model) {
		model.addAttribute("consultations", consultationService.findAwaitingReviewForAssociate(currentAssociate(authentication)));
		
		return "associate/consultation-pending-list";
	}
	
	@GetMapping("/associate/consultations")
	public String consultations(Authentication authentication, Model model) {
		model.addAttribute("consultations", consultationService.findAllForAssociate(currentAssociate(authentication)));
		model.addAttribute("title", "Consultation Requests");
		model.addAttribute("active", "consultations");
		
		return "associate/consultation-list";
	}
	
	@GetMapping("/associate/consultations/{id}")
	public String consultationDetails(@PathVariable("id") Long consultationId, Authentication authentication, Model model) {
		Consultation consultation = consultationService.findOwnedByAssociate(consultationId, currentAssociate(authentication));
		populateConsultationDetailModel(consultation, model);
		
		return "associate/consultation-details";
	}
	
	@PostMapping("/associate/consultations/{id}/schedule")
	public String scheduleMeeting(@PathVariable("id") Long consultationId, @Valid @ModelAttribute ScheduleMeetingRequest form, BindingResult bindingResult, Authentication authentication, Model model, RedirectAttributes redirectAttributes) {
		Consultation consultation = consultationService.findOwnedByAssociate(consultationId, currentAssociate(authentication));
		
		if (bindingResult.hasErrors()) {
			model.addAttribute("error", ValidationUtil.allErrors(bindingResult));
			populateConsultationDetailModel(consultation, model);
			
			return "associate/consultation-details";
		}
		
		try {
			eventService.schedule(consultation, form.getSubject(), form.getDate(), form.getStartTime(), form.getEndTime(), form.getLocation(), form.getNotes());
		} catch (InvalidStateException | BusinessRuleViolationException e) {
			populateConsultationDetailModel(consultation, model);
			model.addAttribute("error", e.getMessage());
			
			return "associate/consultation-details";
		}
		
		redirectAttributes.addFlashAttribute("success", "Meeting scheduled successfully.");
		
		return "redirect:/associate/consultations/{id}";
	}
	
	@PostMapping("/associate/consultations/{id}/accept")
	public String acceptConsultation(@PathVariable("id") Long consultationId, Authentication authentication, Model model, RedirectAttributes redirectAttributes) {
		Consultation consultation = consultationService.findOwnedByAssociate(consultationId, currentAssociate(authentication));
		
		try {
			consultationService.associateAccept(consultation);
		} catch (InvalidStateException | BusinessRuleViolationException e) {
			populateConsultationDetailModel(consultation, model);
			model.addAttribute("error", e.getMessage());
			
			return "associate/consultation-details";
		}
		
		redirectAttributes.addFlashAttribute("success", "Consultation request accepted successfully.");
		
		return "redirect:/associate/consultations/{id}";
	}
	
	@PostMapping("/associate/consultations/{id}/reject")
	public String rejectConsultation(@PathVariable("id") Long consultationId, @Valid @ModelAttribute RejectConsultationRequest form, BindingResult bindingResult, Authentication authentication, Model model, RedirectAttributes redirectAttributes) {
		Consultation consultation = consultationService.findOwnedByAssociate(consultationId, currentAssociate(authentication));
		
		if (bindingResult.hasErrors()) {
			model.addAttribute("error", ValidationUtil.allErrors(bindingResult));
			populateConsultationDetailModel(consultation, model);
			
			return "associate/consultation-details";
		}
		
		try {
			consultationService.associateReject(consultation, form.getRejectionReason());
		} catch (InvalidStateException | BusinessRuleViolationException e) {
			populateConsultationDetailModel(consultation, model);
			model.addAttribute("error", e.getMessage());
			
			return "associate/consultation-details";
		}
		
		redirectAttributes.addFlashAttribute("success", "Consultation request rejected successfully.");
		
		return "redirect:/associate/consultations/{id}";
	}
	
	@PostMapping("/associate/consultations/{id}/open-case")
	public String openCase(@PathVariable("id") Long consultationId, Authentication authentication, RedirectAttributes redirectAttributes) {
		Consultation consultation = consultationService.findOwnedByAssociate(consultationId, currentAssociate(authentication));
		Case case_ = consultationService.openCase(consultation);
		
		redirectAttributes.addFlashAttribute("success", "Case opened successfully.");
		
		return "redirect:/associate/cases/" + case_.getCaseId();
	}
	
	// Cases
	
	@GetMapping("/associate/cases/to-open")
	public String casesToOpen(Authentication authentication, Model model) {
		model.addAttribute("consultations", consultationService.findAwaitingCaseForAssociate(currentAssociate(authentication)));
		model.addAttribute("title", "Cases to Open");
		model.addAttribute("active", "cases-to-open");
		
		return "associate/consultation-list";
	}
	
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
	
	@PostMapping("/associate/cases/{id}/tasks")
	public String addTask(@PathVariable("id") Long caseId, @Valid @ModelAttribute AddTaskRequest form, BindingResult bindingResult, Authentication authentication, Model model, RedirectAttributes redirectAttributes) {
		Case case_ = caseService.findOwnedByAssociate(caseId, currentAssociate(authentication));
		
		if (bindingResult.hasErrors()) {
			model.addAttribute("error", ValidationUtil.allErrors(bindingResult));
			populateCaseDetailModel(case_, model);
			
			return "associate/case-details";
		}
		
		try {
			taskService.add(case_, form.getTitle(), form.getDocumentingTask(), form.getSchedulingTask(), form.getBillingTask(), form.getCaseFiles(), form.getDate(), form.getStartTime(), form.getEndTime(), form.getLocation(), form.getNotes(), form.getBillableAmount());
		} catch (InvalidStateException | BusinessRuleViolationException e) {
			populateCaseDetailModel(case_, model);
			model.addAttribute("error", e.getMessage());
			
			return "associate/case-details";
		}
		
		redirectAttributes.addFlashAttribute("success", "Task added successfully.");
		
		return "redirect:/associate/cases/{id}";
	}
	
	@PostMapping("/associate/cases/{id}/tasks/{taskId}/edit")
	public String editTask(@PathVariable("id") Long caseId, @PathVariable Long taskId, @Valid @ModelAttribute EditTaskRequest form, BindingResult bindingResult, Authentication authentication, Model model, RedirectAttributes redirectAttributes) {
		Case case_ = caseService.findOwnedByAssociate(caseId, currentAssociate(authentication));
		
		if (bindingResult.hasErrors()) {
			model.addAttribute("error", ValidationUtil.allErrors(bindingResult));
			populateCaseDetailModel(case_, model);
			
			return "associate/case-details";
		}
		
		try {
			taskService.update(case_, taskService.findOwnedByCase(taskId, case_), form.getTitle(), form.getDocumentingTask(), form.getSchedulingTask(), form.getBillingTask(), form.getCaseFiles(), form.getDate(), form.getStartTime(), form.getEndTime(), form.getLocation(), form.getNotes(), form.getBillableAmount());
		} catch (InvalidStateException | BusinessRuleViolationException e) {
			populateCaseDetailModel(case_, model);
			model.addAttribute("error", e.getMessage());
			
			return "associate/case-details";
		}
		
		redirectAttributes.addFlashAttribute("success", "Task updated successfully.");
		
		return "redirect:/associate/cases/{id}";
	}
	
	@PostMapping("/associate/cases/{id}/tasks/{taskId}/delete")
	public String deleteTask(@PathVariable("id") Long caseId, @PathVariable Long taskId, Authentication authentication, Model model, RedirectAttributes redirectAttributes) {
		Case case_ = caseService.findOwnedByAssociate(caseId, currentAssociate(authentication));
		
		try {
			taskService.delete(case_, taskService.findOwnedByCase(taskId, case_));
		} catch (InvalidStateException | BusinessRuleViolationException e) {
			populateCaseDetailModel(case_, model);
			model.addAttribute("error", e.getMessage());
			
			return "associate/case-details";
		}
		
		redirectAttributes.addFlashAttribute("success", "Task deleted successfully.");
		
		return "redirect:/associate/cases/{id}";
	}
	
	@PostMapping("/associate/cases/{id}/documents/{documentId}/accept")
	public String acceptStagedDocument(@PathVariable("id") Long caseId, @PathVariable Long documentId, Authentication authentication, RedirectAttributes redirectAttributes) {
		Case case_ = caseService.findOwnedByAssociate(caseId, currentAssociate(authentication));
		documentService.addStagedToCase(case_, List.of(documentId));
		
		redirectAttributes.addFlashAttribute("success", "Document added to case successfully.");
		
		return "redirect:/associate/cases/{id}";
	}
	
	@PostMapping("/associate/cases/{id}/documents/{documentId}/rename")
	public String renameCaseDocument(@PathVariable("id") Long caseId, @PathVariable Long documentId, @Valid @ModelAttribute RenameDocumentRequest form, BindingResult bindingResult, Authentication authentication, Model model, RedirectAttributes redirectAttributes) {
		Case case_ = caseService.findOwnedByAssociate(caseId, currentAssociate(authentication));
		
		if (bindingResult.hasErrors()) {
			model.addAttribute("error", ValidationUtil.allErrors(bindingResult));
			populateCaseDetailModel(case_, model);
			
			return "associate/case-details";
		}
		
		documentService.rename(case_, documentId, form.getOriginalFilename());
		
		redirectAttributes.addFlashAttribute("success", "Document renamed successfully.");
		
		return "redirect:/associate/cases/{id}";
	}
	
	@PostMapping("/associate/cases/{id}/documents/{documentId}/delete")
	public String deleteCaseDocument(@PathVariable("id") Long caseId, @PathVariable Long documentId, Authentication authentication, RedirectAttributes redirectAttributes) {
		Case case_ = caseService.findOwnedByAssociate(caseId, currentAssociate(authentication));
		documentService.deleteStaged(case_, List.of(documentId));
		
		redirectAttributes.addFlashAttribute("success", "Document deleted successfully.");
		
		return "redirect:/associate/cases/{id}";
	}
	
	@PostMapping("/associate/cases/{id}/documents/{documentId}/reject")
	public String rejectStagedDocument(@PathVariable("id") Long caseId, @PathVariable Long documentId, Authentication authentication, RedirectAttributes redirectAttributes) {
		Case case_ = caseService.findOwnedByAssociate(caseId, currentAssociate(authentication));
		documentService.deleteStaged(case_, List.of(documentId));
		
		redirectAttributes.addFlashAttribute("success", "Document deleted successfully.");
		
		return "redirect:/associate/cases/{id}";
	}
	
	@PostMapping("/associate/cases/{id}/documents/accept-selected")
	public String acceptStagedDocuments(@PathVariable("id") Long caseId, @Valid @ModelAttribute SelectDocumentsRequest form, BindingResult bindingResult, Authentication authentication, Model model, RedirectAttributes redirectAttributes) {
		Case case_ = caseService.findOwnedByAssociate(caseId, currentAssociate(authentication));
		
		if (bindingResult.hasErrors()) {
			model.addAttribute("error", ValidationUtil.allErrors(bindingResult));
			populateCaseDetailModel(case_, model);
			
			return "associate/case-details";
		}
		
		documentService.addStagedToCase(case_, form.getDocumentIds());
		
		redirectAttributes.addFlashAttribute("success", "Documents added to case successfully.");
		
		return "redirect:/associate/cases/{id}";
	}
	
	@PostMapping("/associate/cases/{id}/documents/reject-selected")
	public String rejectStagedDocuments(@PathVariable("id") Long caseId, @Valid @ModelAttribute SelectDocumentsRequest form, BindingResult bindingResult, Authentication authentication, Model model, RedirectAttributes redirectAttributes) {
		Case case_ = caseService.findOwnedByAssociate(caseId, currentAssociate(authentication));
		
		if (bindingResult.hasErrors()) {
			model.addAttribute("error", ValidationUtil.allErrors(bindingResult));
			populateCaseDetailModel(case_, model);
			
			return "associate/case-details";
		}
		
		documentService.deleteStaged(case_, form.getDocumentIds());
		
		redirectAttributes.addFlashAttribute("success", "Documents deleted successfully.");
		
		return "redirect:/associate/cases/{id}";
	}
	
	@PostMapping("/associate/cases/{id}/close")
	public String closeCase(@PathVariable("id") Long caseId, @Valid @ModelAttribute CloseCaseRequest form, BindingResult bindingResult, Authentication authentication, Model model, RedirectAttributes redirectAttributes) {
		Case case_ = caseService.findOwnedByAssociate(caseId, currentAssociate(authentication));
		
		if (bindingResult.hasErrors()) {
			model.addAttribute("error", ValidationUtil.allErrors(bindingResult));
			populateCaseDetailModel(case_, model);
			
			return "associate/case-details";
		}
		
		try {
			caseService.close(case_, form.getCaseResolutionStatus());
		} catch (InvalidStateException e) {
			populateCaseDetailModel(case_, model);
			model.addAttribute("error", e.getMessage());
			
			return "associate/case-details";
		}
		
		redirectAttributes.addFlashAttribute("success", "Case closed successfully.");
		
		return "redirect:/associate/cases/{id}";
	}
	
	@PostMapping("/associate/cases/{id}/contacts")
	public String saveContact(@PathVariable("id") Long caseId, @Valid @ModelAttribute SaveContactRequest form, BindingResult bindingResult, Authentication authentication, Model model, RedirectAttributes redirectAttributes) {
		Case case_ = caseService.findOwnedByAssociate(caseId, currentAssociate(authentication));
		
		if (bindingResult.hasErrors()) {
			model.addAttribute("error", ValidationUtil.allErrors(bindingResult));
			populateCaseDetailModel(case_, model);
			
			return "associate/case-details";
		}
		
		try {
			contactService.save(case_, form.getName(), form.getPhoneNumbers(), form.getEmails(), form.getAddresses(), form.getNotes());
		} catch (InvalidStateException e) {
			populateCaseDetailModel(case_, model);
			model.addAttribute("error", e.getMessage());
			
			return "associate/case-details";
		}
		
		redirectAttributes.addFlashAttribute("success", "Contact added successfully.");
		
		return "redirect:/associate/cases/{id}";
	}
	
	@PostMapping("/associate/cases/{id}/contacts/{contactId}/edit")
	public String editContact(@PathVariable("id") Long caseId, @PathVariable Long contactId, @Valid @ModelAttribute SaveContactRequest form, BindingResult bindingResult, Authentication authentication, Model model, RedirectAttributes redirectAttributes) {
		Case case_ = caseService.findOwnedByAssociate(caseId, currentAssociate(authentication));
		
		if (bindingResult.hasErrors()) {
			model.addAttribute("error", ValidationUtil.allErrors(bindingResult));
			populateCaseDetailModel(case_, model);
			
			return "associate/case-details";
		}
		
		try {
			contactService.update(case_, contactService.findOwnedByCase(contactId, case_), form.getName(), form.getPhoneNumbers(), form.getEmails(), form.getAddresses(), form.getNotes());
		} catch (InvalidStateException e) {
			populateCaseDetailModel(case_, model);
			model.addAttribute("error", e.getMessage());
			
			return "associate/case-details";
		}
		
		redirectAttributes.addFlashAttribute("success", "Contact updated successfully.");
		
		return "redirect:/associate/cases/{id}";
	}
	
	@PostMapping("/associate/cases/{id}/contacts/{contactId}/delete")
	public String deleteContact(@PathVariable("id") Long caseId, @PathVariable Long contactId, Authentication authentication, Model model, RedirectAttributes redirectAttributes) {
		Case case_ = caseService.findOwnedByAssociate(caseId, currentAssociate(authentication));
		
		try {
			contactService.delete(case_, contactService.findOwnedByCase(contactId, case_));
		} catch (InvalidStateException e) {
			populateCaseDetailModel(case_, model);
			model.addAttribute("error", e.getMessage());
			
			return "associate/case-details";
		}
		
		redirectAttributes.addFlashAttribute("success", "Contact deleted successfully.");
		
		return "redirect:/associate/cases/{id}";
	}
	
	// Schedule
	
	@GetMapping("/associate/events/upcoming")
	public String upcomingEvents(Authentication authentication, Model model) {
		model.addAttribute("events", eventService.upcoming(eventService.findAllForAssociate(currentAssociate(authentication))));
		model.addAttribute("title", "Upcoming Events");
		model.addAttribute("active", "events-upcoming");
		
		return "associate/event-list";
	}
	
	@GetMapping("/associate/events/past")
	public String pastEvents(Authentication authentication, Model model) {
		model.addAttribute("events", eventService.past(eventService.findAllForAssociate(currentAssociate(authentication))));
		model.addAttribute("title", "Past Events");
		model.addAttribute("active", "events-past");
		
		return "associate/event-list";
	}
	
	@GetMapping("/associate/events/{id}")
	public String eventDetails(@PathVariable("id") Long eventId, Authentication authentication, Model model) {
		model.addAttribute("event", eventService.findOwnedByAssociate(eventId, currentAssociate(authentication)));
		
		return "associate/event-details";
	}
	
	@PostMapping("/associate/events/{id}/complete")
	public String completeEvent(@PathVariable("id") Long eventId, Authentication authentication, Model model, RedirectAttributes redirectAttributes) {
		Event event = eventService.findOwnedByAssociate(eventId, currentAssociate(authentication));
		
		try {
			eventService.complete(event);
		} catch (InvalidStateException | BusinessRuleViolationException e) {
			model.addAttribute("event", event);
			model.addAttribute("error", e.getMessage());
			
			return "associate/event-details";
		}
		
		redirectAttributes.addFlashAttribute("success", "Event completed successfully.");
		
		return "redirect:/associate/events/{id}";
	}
	
	@PostMapping("/associate/events/{id}/cancel")
	public String cancelEvent(@PathVariable("id") Long eventId, Authentication authentication, Model model, RedirectAttributes redirectAttributes) {
		Event event = eventService.findOwnedByAssociate(eventId, currentAssociate(authentication));
		
		try {
			eventService.cancel(event);
		} catch (InvalidStateException e) {
			model.addAttribute("event", event);
			model.addAttribute("error", e.getMessage());
			
			return "associate/event-details";
		}
		
		redirectAttributes.addFlashAttribute("success", "Event canceled successfully.");
		
		return "redirect:/associate/events/{id}";
	}
	
	@PostMapping("/associate/events/{id}/reschedule")
	public String rescheduleEvent(@PathVariable("id") Long eventId, @Valid @ModelAttribute ScheduleMeetingRequest form, BindingResult bindingResult, Authentication authentication, Model model, RedirectAttributes redirectAttributes) {
		Event event = eventService.findOwnedByAssociate(eventId, currentAssociate(authentication));
		
		if (bindingResult.hasErrors()) {
			model.addAttribute("event", event);
			model.addAttribute("error", ValidationUtil.allErrors(bindingResult));
			
			return "associate/event-details";
		}
		
		try {
			eventService.reschedule(event, form.getSubject(), form.getDate(), form.getStartTime(), form.getEndTime(), form.getLocation(), form.getNotes());
		} catch (InvalidStateException | BusinessRuleViolationException e) {
			model.addAttribute("event", event);
			model.addAttribute("error", e.getMessage());
			
			return "associate/event-details";
		}
		
		redirectAttributes.addFlashAttribute("success", "Event rescheduled successfully.");
		
		return "redirect:/associate/events/{id}";
	}
	
	// Documents
	
	@GetMapping({"/associate/documents/user/{id}", "/associate/documents/user/{id}/{filename}"})
	public ResponseEntity<Resource> viewUserDocument(@PathVariable("id") Long documentId, Authentication authentication) {
		return documentService.streamUserDocument(documentService.findById(documentId), currentAssociate(authentication));
	}
	
	@GetMapping({"/associate/documents/consultation/{id}", "/associate/documents/consultation/{id}/{filename}"})
	public ResponseEntity<Resource> viewConsultationDocument(@PathVariable("id") Long documentId, Authentication authentication) {
		return documentService.streamConsultationDocument(documentService.findById(documentId), currentAssociate(authentication));
	}
	
	@GetMapping({"/associate/documents/case/{id}", "/associate/documents/case/{id}/{filename}"})
	public ResponseEntity<Resource> viewCaseDocument(@PathVariable("id") Long documentId, Authentication authentication) {
		return documentService.streamCaseDocument(documentService.findById(documentId), currentAssociate(authentication));
	}
	
	// Private Helpers
	
	private Associate currentAssociate(Authentication authentication) {
		CustomUserPrincipal principal = (CustomUserPrincipal) authentication.getPrincipal();
		
		return (Associate) principal.user();
	}
	
	private void populateConsultationDetailModel(Consultation consultation, Model model) {
		model.addAttribute("consultation", consultation);
		model.addAttribute("consultationDocuments", documentService.findByConsultation(consultation));
		model.addAttribute("consultationEvents", eventService.findByConsultation(consultation));
		model.addAttribute("hasCompletedMeeting", consultationService.hasCompletedMeeting(consultation));
		model.addAttribute("existingCase", caseService.findByConsultation(consultation));
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
