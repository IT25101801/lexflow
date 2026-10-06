package lexflow.util;

import lexflow.entity.user.Role;

public class CodeUtil {
	
	private static final long CODE_RANGE = 90_000_000L;
	
	private static final long MIN_CODE = 10_000_000L;
	
	private static final long MULTIPLIER = 71_234_567L;
	
	private static final long OFFSET = 12_345_691L;
	
	public static String forUser(Role role, Long userId) {
		
		String prefix = switch (role) {
			case PARTNER -> "PRT";
			case ASSOCIATE -> "ASC";
			case PARALEGAL -> "PRL";
			case CLIENT -> "CLT";
			case CLIENT_RELATIONS_OFFICER -> "CRO";
			case FINANCE_OFFICER -> "FCO";
		};
		
		return prefix + "-" + userId + "-" + generate(userId);
	}
	
	public static String forConsultation(Long consultationId) {
		return "CON-" + consultationId + "-" + generate(consultationId);
	}
	
	public static String forCase(Long caseId) {
		return "CAS-" + caseId + "-" + generate(caseId);
	}
	
	public static String forInvoice(Long invoiceId) {
		return "INV-" + invoiceId + "-" + generate(invoiceId);
	}
	
	// Private Helpers
	private static long generate(long id) {
		return MIN_CODE + ((id % CODE_RANGE) * MULTIPLIER + OFFSET) % CODE_RANGE;
	}
	
}
