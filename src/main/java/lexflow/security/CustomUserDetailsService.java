package lexflow.security;

import lexflow.dao.UserDao;
import lexflow.entity.user.User;
import lexflow.exception.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {
	
	private final UserDao userDao;
	
	@Override
	public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
		
		try {
			User user = userDao.findByEmail(email);
			
			return new CustomUserPrincipal(user);
		} catch (EntityNotFoundException e) {
			throw new UsernameNotFoundException("No user with that email");
		}
		
	}
	
}
