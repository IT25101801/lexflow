package lexflow.controller;

import jakarta.validation.Valid;
import lexflow.dto.user.RegisterClientRequest;
import lexflow.entity.user.Gender;
import lexflow.exception.BusinessRuleViolationException;
import lexflow.exception.EntityAlreadyExistsException;
import lexflow.exception.PersistenceException;
import lexflow.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
	
	private final UserService userService;
	
	@GetMapping("/login")
	public String login() {
		return "auth/login";
	}
	
	@GetMapping("/register-client")
	public String showRegisterClientForm(Model model) {
		model.addAttribute("genders", Gender.values());
		return "auth/register-client";
	}
	
	@PostMapping("/register-client")
	public String registerClient(@Valid @ModelAttribute RegisterClientRequest form, BindingResult bindingResult, Model model, RedirectAttributes redirectAttributes) {
		if (bindingResult.hasErrors()) {
			model.addAttribute("error", bindingResult.getAllErrors().getFirst().getDefaultMessage());
			model.addAttribute("genders", Gender.values());
			return "auth/register-client";
		}
		
		try {
			userService.register(form.getEmail(), form.getPassword(), form.getName(), form.getDateOfBirth(),
								 form.getGender(), form.getAddresses(), form.getPhoneNumbers(),
								 form.getNicNumber(), form.getNicFile());
		} catch (EntityAlreadyExistsException | BusinessRuleViolationException e) {
			model.addAttribute("genders", Gender.values());
			model.addAttribute("error", e.getMessage());
			return "auth/register-client";
		} catch (PersistenceException e) {
			model.addAttribute("genders", Gender.values());
			model.addAttribute("error", "Could not create your account. Please try again.");
			return "auth/register-client";
		}
		
		redirectAttributes.addFlashAttribute("success", "Registration successful. Please log in.");
		return "redirect:/auth/login";
	}
	
}
