package lexflow.security;

import lexflow.entity.user.AccountStatus;
import lexflow.entity.user.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

public record CustomUserPrincipal(User user) implements UserDetails {
	
	@Override
	public String getUsername() {
		return user.getEmail();
	}
	
	@Override
	public String getPassword() {
		return user.getPassword();
	}
	
	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		return List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole()));
	}
	
	@Override
	public boolean isAccountNonExpired() {
		return true;
	}
	
	@Override
	public boolean isAccountNonLocked() {
		return true;
	}
	
	@Override
	public boolean isCredentialsNonExpired() {
		return true;
	}
	
	@Override
	public boolean isEnabled() {
		return user.getAccountStatus() == AccountStatus.ACTIVE;
	}
	
}
