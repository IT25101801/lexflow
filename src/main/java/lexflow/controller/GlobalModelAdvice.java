package lexflow.controller;

import lexflow.entity.user.User;
import lexflow.security.CustomUserPrincipal;
import org.springframework.beans.propertyeditors.CustomCollectionEditor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.List;

@ControllerAdvice
public class GlobalModelAdvice {
	
	private static final String[] TEXT_LIST_FIELDS = {"addresses", "phoneNumbers", "emails", "opposingParties"};
	
	@ModelAttribute("currentUser")
	public User currentUser(Authentication authentication) {
		if (authentication != null && authentication.getPrincipal() instanceof CustomUserPrincipal(User user)) {
			return user;
		}
		return null;
	}
	
	@InitBinder
	public void keepTextListValuesWhole(WebDataBinder binder) {
		for (String field : TEXT_LIST_FIELDS) {
			binder.registerCustomEditor(List.class, field, new WholeValueListEditor());
		}
	}
	
	private static class WholeValueListEditor extends CustomCollectionEditor {
		
		WholeValueListEditor() {
			super(List.class, true);
		}
		
		@Override
		public void setAsText(String text) {
			
			if (text == null || text.isBlank()) {
				setValue(null);
				return;
			}
			
			setValue(text.trim());
		}
		
	}
	
}
