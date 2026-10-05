package lexflow.controller;

import jakarta.servlet.http.HttpServletResponse;
import lexflow.exception.EntityNotFoundException;
import lexflow.exception.LexFlowException;
import lexflow.exception.PdfRenderingException;
import lexflow.exception.PersistenceException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {
	
	@ExceptionHandler(EntityNotFoundException.class)
	public String handleNotFound(EntityNotFoundException e, HttpServletResponse response, Model model) {
		response.setStatus(HttpServletResponse.SC_NOT_FOUND);
		model.addAttribute("heading", "Not found");
		model.addAttribute("message", e.getMessage());
		return "error/generic";
	}
	
	@ExceptionHandler(AccessDeniedException.class)
	public String handleAccessDenied(AccessDeniedException e, HttpServletResponse response, Model model) {
		response.setStatus(HttpServletResponse.SC_FORBIDDEN);
		model.addAttribute("heading", "Access denied");
		model.addAttribute("message", "You are not authorized to do that.");
		return "error/generic";
	}
	
	@ExceptionHandler(PersistenceException.class)
	public String handlePersistence(PersistenceException e, HttpServletResponse response, Model model) {
		log.error("Persistence failure during {}", e.getOperation(), e);
		response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		model.addAttribute("heading", "Something went wrong");
		model.addAttribute("message", "The system could not complete that request. Please try again.");
		return "error/generic";
	}
	
	@ExceptionHandler(PdfRenderingException.class)
	public String handlePdfRendering(PdfRenderingException e, HttpServletResponse response, Model model) {
		log.error("Could not render {} as a PDF", e.getDocumentName(), e);
		response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		model.addAttribute("heading", "Something went wrong");
		model.addAttribute("message", "The PDF could not be created. Please try again.");
		return "error/generic";
	}
	
	@ExceptionHandler(LexFlowException.class)
	public String handleLexFlowException(LexFlowException e, HttpServletResponse response, Model model) {
		response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
		model.addAttribute("heading", "Request not allowed");
		model.addAttribute("message", e.getMessage());
		return "error/generic";
	}
	
	@ExceptionHandler(Exception.class)
	public String handleGenericException(Exception e, HttpServletResponse response, Model model) {
		log.error("Unhandled exception", e);
		response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		model.addAttribute("heading", "Something went wrong");
		model.addAttribute("message", "Something went wrong. Please try again.");
		return "error/generic";
	}
	
}
