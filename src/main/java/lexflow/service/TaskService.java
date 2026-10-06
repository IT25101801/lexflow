package lexflow.service;

import lexflow.dao.TaskDao;
import lexflow.entity.case_.Case;
import lexflow.entity.case_.Task;
import lexflow.entity.finance.Invoice;
import lexflow.entity.scheduling.CaseEvent;
import lexflow.entity.scheduling.EventStatus;
import lexflow.exception.case_.IncompleteBillingTaskException;
import lexflow.exception.case_.IncompleteSchedulingTaskException;
import lexflow.exception.case_.NoTaskTypeSelectedException;
import lexflow.exception.case_.SchedulingTaskNotRemovableException;
import lexflow.exception.case_.TaskNotDeletableException;
import lexflow.exception.finance.BillableAmountNotEditableException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class TaskService {
	
	private final TaskDao taskDao;
	private final CaseService caseService;
	private final DocumentService documentService;
	private final EventService eventService;
	
	// Create
	
	@Transactional
	public Task add(Case case_, String title, Boolean documentingTask, Boolean schedulingTask, Boolean billingTask, List<MultipartFile> caseFiles, LocalDate date, LocalTime startTime, LocalTime endTime, String location, String notes, BigDecimal billableAmount) {
		caseService.requireOpen(case_);
		
		boolean isDocumentingTask = Boolean.TRUE.equals(documentingTask);
		boolean isSchedulingTask = Boolean.TRUE.equals(schedulingTask);
		boolean isBillingTask = Boolean.TRUE.equals(billingTask);
		
		if (!isDocumentingTask && !isSchedulingTask && !isBillingTask) {
			throw new NoTaskTypeSelectedException(title);
		}
		
		if (isSchedulingTask && isMissingSchedule(date, startTime, endTime, location)) {
			throw new IncompleteSchedulingTaskException(title);
		}
		
		if (isBillingTask && billableAmount == null) {
			throw new IncompleteBillingTaskException(title);
		}
		
		LocalDateTime loggedAt = LocalDateTime.now();
		Task task = new Task(case_.getCaseId(), title, isDocumentingTask, isSchedulingTask, isBillingTask, isBillingTask ? billableAmount : null, loggedAt);
		
		taskDao.insert(task);
		
		if (isSchedulingTask) {
			CaseEvent scheduledEvent = eventService.schedule(case_, task, title, date, startTime, endTime, location, notes);
			task.setScheduledEvent(scheduledEvent);
		}
		
		if (isDocumentingTask && hasAtLeastOneFile(caseFiles)) {
			documentService.attach(case_, task.getTaskId(), caseFiles, loggedAt);
		}
		
		return task;
	}
	
	// Read
	
	public Task findById(Long taskId) {
		return taskDao.findById(taskId);
	}
	
	public Task findOwnedByCase(Long taskId, Case case_) {
		Task task = findById(taskId);
		
		if (!task.getCaseId().equals(case_.getCaseId())) {
			throw new AccessDeniedException("Not a task for this case.");
		}
		
		return task;
	}
	
	public List<Task> findByCase(Case case_) {
		return taskDao.findByCaseId(case_.getCaseId());
	}
	
	public List<Task> findUnbilledByCase(Case case_) {
		return taskDao.findUnbilledByCaseId(case_.getCaseId());
	}
	
	public List<Task> findByInvoice(Invoice invoice) {
		return taskDao.findByBilledInvoiceId(invoice.getInvoiceId());
	}
	
	// Update
	
	@Transactional
	public Task update(Case case_, Task task, String title, Boolean documentingTask, Boolean schedulingTask, Boolean billingTask, List<MultipartFile> caseFiles, LocalDate date, LocalTime startTime, LocalTime endTime, String location, String notes, BigDecimal billableAmount) {
		caseService.requireOpen(case_);
		
		boolean isDocumentingTask = Boolean.TRUE.equals(documentingTask);
		boolean isSchedulingTask = Boolean.TRUE.equals(schedulingTask);
		boolean isInvoiced = task.getBilledInvoiceId() != null;
		boolean isBillingTask = isInvoiced || Boolean.TRUE.equals(billingTask);
		BigDecimal newBillableAmount = isInvoiced ? task.getBillableAmount() : billableAmount;
		
		if (!isDocumentingTask && !isSchedulingTask && !isBillingTask) {
			throw new NoTaskTypeSelectedException(title);
		}
		
		if (task.isSchedulingTask() && !isSchedulingTask) {
			throw new SchedulingTaskNotRemovableException(task.getTitle());
		}
		
		CaseEvent scheduledEvent = task.getScheduledEvent();
		boolean startsScheduling = isSchedulingTask && !task.isSchedulingTask();
		boolean hasScheduledEvent = scheduledEvent != null && scheduledEvent.getEventStatus() == EventStatus.SCHEDULED;
		
		if ((startsScheduling || hasScheduledEvent) && isMissingSchedule(date, startTime, endTime, location)) {
			throw new IncompleteSchedulingTaskException(title);
		}
		
		if (isBillingTask && newBillableAmount == null) {
			throw new IncompleteBillingTaskException(title);
		}
		
		boolean stopsDocumenting = task.isDocumentingTask() && !isDocumentingTask;
		task.setTitle(title);
		task.setDocumentingTask(isDocumentingTask);
		task.setSchedulingTask(isSchedulingTask);
		task.setBillingTask(isBillingTask);
		task.setBillableAmount(isBillingTask ? newBillableAmount : null);
		
		taskDao.update(task);
		
		if (stopsDocumenting) {
			documentService.unlinkFromTask(task);
		}
		
		if (isDocumentingTask && hasAtLeastOneFile(caseFiles)) {
			documentService.attach(case_, task.getTaskId(), caseFiles, LocalDateTime.now());
		}
		
		if (startsScheduling) {
			task.setScheduledEvent(eventService.schedule(case_, task, title, date, startTime, endTime, location, notes));
		} else if (hasScheduledEvent && isSameSchedule(scheduledEvent, date, startTime, endTime, location, notes)) {
			eventService.updateSubject(scheduledEvent, title);
		} else if (hasScheduledEvent) {
			eventService.reschedule(scheduledEvent, title, date, startTime, endTime, location, notes);
		}
		
		return task;
	}
	
	public Task updateBillableAmount(Task task, BigDecimal billableAmount) {
		
		if (task.getBillableAmount() == null || task.getBilledInvoiceId() != null) {
			throw new BillableAmountNotEditableException(task.getTaskId());
		}
		
		task.setBillableAmount(billableAmount);
		
		return taskDao.update(task);
	}
	
	@Transactional
	public void linkToInvoice(List<Task> tasks, Long billedInvoiceId) {
		
		for (Task task : tasks) {
			task.setBilledInvoiceId(billedInvoiceId);
			taskDao.update(task);
		}
		
	}
	
	public void unlinkFromInvoice(Long billedInvoiceId) {
		List<Task> tasks = taskDao.findByBilledInvoiceId(billedInvoiceId);
		
		for (Task task : tasks) {
			task.setBilledInvoiceId(null);
			taskDao.update(task);
		}
		
	}
	
	// Delete
	
	public void delete(Case case_, Task task) {
		caseService.requireOpen(case_);
		
		if (task.isSchedulingTask() || task.getBilledInvoiceId() != null || !documentService.findByTask(task).isEmpty()) {
			throw new TaskNotDeletableException(task.getTitle());
		}
		
		taskDao.deleteById(task.getTaskId());
	}
	
	// Private Helpers
	
	private boolean isMissingSchedule(LocalDate date, LocalTime startTime, LocalTime endTime, String location) {
		return date == null || startTime == null || endTime == null || location == null || location.isBlank();
	}
	
	private boolean hasAtLeastOneFile(List<MultipartFile> caseFiles) {
		
		if (caseFiles == null) {
			return false;
		}
		
		for (MultipartFile caseFile : caseFiles) {
			
			if (caseFile != null && !caseFile.isEmpty()) {
				return true;
			}
			
		}
		
		return false;
	}

	private boolean isSameSchedule(CaseEvent caseEvent, LocalDate date, LocalTime startTime, LocalTime endTime, String location, String notes) {
		String currentNotes = caseEvent.getNotes() == null || caseEvent.getNotes().isBlank() ? null : caseEvent.getNotes();
		String newNotes = notes == null || notes.isBlank() ? null : notes;
		
		return caseEvent.getDate().equals(date) && caseEvent.getStartTime().equals(startTime) && caseEvent.getEndTime().equals(endTime) && caseEvent.getLocation().equals(location) && Objects.equals(currentNotes, newNotes);
	}
	
}
