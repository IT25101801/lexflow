package lexflow.util;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import lexflow.exception.PdfRenderingException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class PdfUtil {
	
	public static byte[] render(String html, String documentName) {
		
		try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
			PdfRendererBuilder builder = new PdfRendererBuilder();
			
			builder.useFastMode();
			builder.withHtmlContent(html, null);
			builder.toStream(outputStream);
			builder.run();
			
			return outputStream.toByteArray();
		} catch (IOException e) {
			throw new PdfRenderingException(documentName, e);
		}
		
	}
	
}
