package dz.ab.reportconsolidator.controller;
import dz.ab.reportconsolidator.exception.*;
import org.slf4j.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.*;
@RestControllerAdvice
public class GlobalExceptionHandler {
 private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
 public record ErrorResponse(String message) {}
 @ExceptionHandler(InvalidWordDocumentException.class)
 public ResponseEntity<ErrorResponse> invalid(InvalidWordDocumentException e) {
  return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
 }
 @ExceptionHandler(MaxUploadSizeExceededException.class)
 public ResponseEntity<ErrorResponse> tooLarge(MaxUploadSizeExceededException e) {
  return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(new ErrorResponse("Import trop volumineux : 10 Mo par fichier et 100 Mo au total."));
 }
 @ExceptionHandler(MultipartException.class)
 public ResponseEntity<ErrorResponse> multipart(MultipartException e) {
  return ResponseEntity.badRequest().body(new ErrorResponse("L’import des fichiers a échoué. Vérifiez leur nombre et leur taille."));
 }
 @ExceptionHandler(Exception.class)
 public ResponseEntity<ErrorResponse> unexpected(Exception e) {
  log.error("Échec du traitement ({})", e.getClass().getSimpleName());
  return ResponseEntity.internalServerError().body(new ErrorResponse("Le traitement a échoué. Réessayez ou contactez l’administrateur."));
 }
}