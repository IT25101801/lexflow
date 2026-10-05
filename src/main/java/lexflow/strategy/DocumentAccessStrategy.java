package lexflow.strategy;

import lexflow.entity.user.Associate;
import lexflow.entity.user.Client;
import lexflow.entity.user.Paralegal;
import lexflow.entity.user.User;

public interface DocumentAccessStrategy {
	
	boolean canAccess(User requester, Client client, Associate associate, Paralegal paralegal);
	
}
