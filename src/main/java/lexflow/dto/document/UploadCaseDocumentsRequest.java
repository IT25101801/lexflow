package lexflow.dto.document;

import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Getter
@Setter
public class UploadCaseDocumentsRequest {
	
	private List<MultipartFile> caseFiles;
	
}
