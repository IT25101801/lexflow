package lexflow.util;

import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ValidationUtil {
	
	private static final List<String> REQUIRED_CODES = List.of("NotNull", "NotBlank", "NotEmpty");

	// Read

	public static List<String> allErrors(BindingResult bindingResult) {
		List<String> errors = new ArrayList<>();
		Object form = bindingResult.getTarget();
		
		if (form != null) {
			
			for (Field field : form.getClass().getDeclaredFields()) {
				String fieldError = fieldErrorFor(bindingResult, field.getName());
				
				if (fieldError != null && !errors.contains(fieldError)) {
					errors.add(fieldError);
				}
				
			}
			
		}
		
		for (ObjectError objectError : bindingResult.getGlobalErrors()) {
			
			if (!errors.contains(objectError.getDefaultMessage())) {
				errors.add(objectError.getDefaultMessage());
			}
			
		}
		
		return errors;
	}
	
	public static boolean hasNoRepeats(List<String> values) {

		if (values == null) {
			return true;
		}

		Set<String> seen = new HashSet<>();

		for (String value : values) {

			if (value != null && !value.isBlank() && !seen.add(value.trim().toLowerCase())) {
				return false;
			}

		}

		return true;
	}
	
	// Private Helpers
	
	private static String fieldErrorFor(BindingResult bindingResult, String name) {
		String firstMessage = null;
		
		for (FieldError fieldError : bindingResult.getFieldErrors()) {
			String errorField = fieldError.getField();
			
			if (!errorField.equals(name) && !errorField.startsWith(name + "[") && !errorField.equals(name + "Unique")) {
				continue;
			}
			
			if (REQUIRED_CODES.contains(fieldError.getCode())) {
				return fieldError.getDefaultMessage();
			}
			
			if (firstMessage == null) {
				firstMessage = fieldError.getDefaultMessage();
			}
			
		}
		
		return firstMessage;
	}
	
}
