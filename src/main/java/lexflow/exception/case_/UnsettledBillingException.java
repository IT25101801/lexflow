package lexflow.exception.case_;

import lexflow.exception.BusinessRuleViolationException;
import lombok.Getter;

@Getter
public class UnsettledBillingException extends BusinessRuleViolationException {
	
	private final Long caseId;
	private final int unbilledTaskCount;
	private final int unpaidInvoiceCount;
	
	public UnsettledBillingException(Long caseId, int unbilledTaskCount, int unpaidInvoiceCount) {
		super(String.format("This case cannot be closed until its billing is settled: %d unbilled billing task(s) and %d unpaid invoice(s) remain.", unbilledTaskCount, unpaidInvoiceCount));
		this.caseId = caseId;
		this.unbilledTaskCount = unbilledTaskCount;
		this.unpaidInvoiceCount = unpaidInvoiceCount;
	}
	
}
