package lexflow.strategy;

import lexflow.entity.user.Associate;
import lexflow.entity.user.Client;
import lexflow.entity.user.Paralegal;
import lexflow.entity.user.User;

public class ParalegalOwnershipAccessStrategy implements DocumentAccessStrategy {
	
	@Override
	public boolean canAccess(User requester, Client client, Associate associate, Paralegal paralegal) {
		return paralegal != null && paralegal.getUserId().equals(requester.getUserId());
	}
	
}
