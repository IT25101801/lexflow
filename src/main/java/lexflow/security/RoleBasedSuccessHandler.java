package lexflow.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lexflow.entity.user.Role;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class RoleBasedSuccessHandler implements AuthenticationSuccessHandler {
	
	@Override
	public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException {
		CustomUserPrincipal principal = (CustomUserPrincipal) authentication.getPrincipal();
		response.sendRedirect(dashboardUrlFor(principal.user().getRole()));
	}
	
	private String dashboardUrlFor(Role role) {
		return switch (role) {
			case PARTNER -> "/partner/dashboard";
			case ASSOCIATE -> "/associate/dashboard";
			case PARALEGAL -> "/paralegal/dashboard";
			case CLIENT -> "/client/dashboard";
			case CLIENT_RELATIONS_OFFICER -> "/client-relations-officer/dashboard";
			case FINANCE_OFFICER -> "/finance-officer/dashboard";
		};
	}
	
}
