package lexflow.config;

import lexflow.security.RoleBasedSuccessHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {
	
	private final RoleBasedSuccessHandler roleBasedSuccessHandler;
	
	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}
	
	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) {
		http
			.authorizeHttpRequests(authorize -> authorize
				.requestMatchers("/", "/favicon.ico", "/favicon.svg", "/auth/**", "/css/**", "/js/**", "/demo-files/**").permitAll()
				.requestMatchers("/client/**").hasRole("CLIENT")
				.requestMatchers("/partner/**").hasRole("PARTNER")
				.requestMatchers("/associate/**").hasRole("ASSOCIATE")
				.requestMatchers("/paralegal/**").hasRole("PARALEGAL")
				.requestMatchers("/client-relations-officer/**").hasRole("CLIENT_RELATIONS_OFFICER")
				.requestMatchers("/finance-officer/**").hasRole("FINANCE_OFFICER")
				.anyRequest().authenticated())
			.formLogin(form -> form
				.loginPage("/auth/login")
				.loginProcessingUrl("/auth/login")
				.successHandler(roleBasedSuccessHandler)
				.permitAll())
			.logout(logout -> logout
				.logoutSuccessUrl("/auth/login?logout")
				.permitAll());
		
		return http.build();
	}
	
}
