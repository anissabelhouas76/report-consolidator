package dz.ab.reportconsolidator.controller;
import dz.ab.reportconsolidator.service.ReportProcessingService;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
@Controller
public class ConsolidationController {
 private final ReportProcessingService processing;
 public ConsolidationController(ReportProcessingService processing) { this.processing = processing; }
 @GetMapping("/") public String index() { return "index"; }
 @PostMapping(value="/consolidate", consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
 public ResponseEntity<byte[]> consolidate(@RequestParam(value="files", required=false) MultipartFile[] files) {
  return ResponseEntity.ok()
   .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
   .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename("Rapport_Consolide.docx").build().toString())
   .cacheControl(CacheControl.noStore())
   .body(processing.process(files));
 }
}