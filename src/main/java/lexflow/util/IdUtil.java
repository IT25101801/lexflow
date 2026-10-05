package lexflow.util;

import lexflow.entity.user.Role;

public class IdUtil {
	
	private static final long CODE_RANGE = 90_000_000L;
	private static final long MIN_CODE = 10_000_000L;
	private static final long MULTIPLIER = 71_234_567L;
	private static final long OFFSET = 12_345_691L;
	
	public static String generateUserCode(Role role, Long userId) {
		
		String prefix = switch (role) {
			case PARTNER -> "PRT";
			case ASSOCIATE -> "ASC";
			case PARALEGAL -> "PRL";
			case CLIENT -> "CLT";
			case CLIENT_RELATIONS_OFFICER -> "CRO";
			case FINANCE_OFFICER -> "FCO";
		};
		
		return prefix + "-" + userId + "-" + generateCode(userId);
	}
	
	public static String generateConsultationCode(Long consultationId) {
		return "CON-" + consultationId + "-" + generateCode(consultationId);
	}
	
	public static String generateCaseCode(Long caseId) {
		return "CAS-" + caseId + "-" + generateCode(caseId);
	}
	
	public static String generateInvoiceCode(Long invoiceId) {
		return "INV-" + invoiceId + "-" + generateCode(invoiceId);
	}
	
	// ---- Private Helpers ----
	
	// Keep in sync with dbo.ufn_generate_code in database/function.sql, which builds the transaction codes the same way
	private static long generateCode(long id) {
		return MIN_CODE + ((id % CODE_RANGE) * MULTIPLIER + OFFSET) % CODE_RANGE;
	}
	
}
