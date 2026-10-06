package lexflow.service;

import lexflow.dao.TaskDao;
import lexflow.entity.case_.Case;
import lexflow.entity.case_.CaseStatus;
import lexflow.entity.case_.Task;
import lexflow.entity.scheduling.CaseEvent;
import lexflow.exception.case_.IncompleteBillingTaskException;
import lexflow.exception.case_.IncompleteDocumentingTaskException;
import lexflow.exception.case_.IncompleteSchedulingTaskException;
import lexflow.exception.case_.InvalidCaseStateException;
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

@Service
@RequiredArgsConstructor
public class TaskService {
	
	private final TaskDao taskDao;
	private final DocumentService documentService;
	private final EventService eventService;
	
	// Create
	
	@Transactional
	public Task add(Case case_, String title, Boolean documentingTask, Boolean schedulingTask, Boolean billingTask, List<MultipartFile> caseFiles, LocalDate date, LocalTime startTime, LocalTime endTime, String location, String notes, BigDecimal billableAmount) {
		
		if (case_.getCaseStatus() != CaseStatus.OPEN) {
			throw new InvalidCaseStateException(case_.getCaseId(), case_.getCaseStatus(), CaseStatus.OPEN);
		}
		
		boolean isDocumentingTask = Boolean.TRUE.equals(documentingTask);
		
		if (isDocumentingTask && !hasAtLeastOneFile(caseFiles)) {
			throw new IncompleteDocumentingTaskException(title);
		}
		
		boolean isSchedulingTask = Boolean.TRUE.equals(schedulingTask);
		
		if (isSchedulingTask && (date == null || startTime == null || endTime == null || location == null || location.isBlank())) {
			throw new IncompleteSchedulingTaskException(title);
		}
		
		boolean isBillingTask = Boolean.TRUE.equals(billingTask);
		
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
		
		if (isDocumentingTask && caseFiles != null && !caseFiles.isEmpty()) {
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
	
	// Update
	
	public Task updateTitle(Task task, String title) {
		task.setTitle(title);
		
		return taskDao.update(task);
	}
	
	public Task updateBillableAmount(Task task, BigDecimal billableAmount) {
		
		if (task.getBillableAmount() == null || task.getBilledInvoiceId() != null) {
			throw new BillableAmountNotEditableException(task.getTaskId());
		}
		
		task.setBillableAmount(billableAmount);
		
		return taskDao.update(task);
	}
	
	public CaseEvent rescheduleEvent(Task task, String subject, LocalDate date, LocalTime startTime, LocalTime endTime, String location, String notes) {
		
		if (!task.isSchedulingTask() || task.getScheduledEvent() == null) {
			throw new AccessDeniedException("Task has no scheduled event.");
		}
		
		return eventService.reschedule(task.getScheduledEvent(), subject, date, startTime, endTime, location, notes);
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
	
	// Private Helpers
	
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
	
}
