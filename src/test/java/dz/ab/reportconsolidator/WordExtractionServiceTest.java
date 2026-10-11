package dz.ab.reportconsolidator;
import dz.ab.reportconsolidator.exception.InvalidWordDocumentException;
import dz.ab.reportconsolidator.service.*;
import org.apache.poi.xwpf.usermodel.*;
import org.junit.jupiter.api.Test;
import java.io.*;
import static org.assertj.core.api.Assertions.*;
class WordExtractionServiceTest {
 private final WordExtractionService service = new WordExtractionService();
 private byte[] bytes(XWPFDocument document) throws IOException {
  ByteArrayOutputStream output = new ByteArrayOutputStream(); document.write(output); return output.toByteArray();
 }
 @Test void detectsReorderedColumnsMultipleTablesRepeatedHeadersAndLines() throws Exception {
  try (var document = new XWPFDocument()) {
   document.createParagraph().createRun().setText("Période : Août 2026");
   document.createParagraph().createRun().setText("Élaboré par : Développeur A");
   document.createTable(1, 2).getRow(0).getCell(0).setText("Informations générales");
   var table = document.createTable(1, 3);
   table.getRow(0).getCell(0).setText(" TRAVAUX   EFFECTUÉS ");
   table.getRow(0).getCell(1).setText("Auteur");
   table.getRow(0).getCell(2).setText(" application ");
   var row = table.createRow(); row.getCell(2).setText(" EPF ");
   var run = row.getCell(0).getParagraphs().get(0).createRun();
   run.setText("• Travail A"); run.addBreak(); run.setText("Travail B");
   row.getCell(0).addParagraph();
   row.getCell(0).addParagraph().createRun().setText("Travail C");
   var header = table.createRow(); header.getCell(0).setText("Travaux effectués"); header.getCell(2).setText("Application");
   table.createRow();
   var other = document.createTable(2, 2);
   other.getRow(0).getCell(0).setText("Application"); other.getRow(0).getCell(1).setText("Travaux effectués");
   other.getRow(1).getCell(0).setText("Ecomp"); other.getRow(1).getCell(1).setText("Autre travail");
   var result = service.extract(new ByteArrayInputStream(bytes(document)), "rapport.docx");
   assertThat(result.period()).isEqualTo("Août 2026");
   assertThat(result.activities()).hasSize(2);
   assertThat(result.activities().get(0).tasks()).containsExactly("Travail A", "Travail B", "Travail C");
  }
 }
 @Test void rejectsMissingTable() throws Exception {
  try (var document = new XWPFDocument()) {
   assertThatThrownBy(() -> service.extract(new ByteArrayInputStream(bytes(document)), "vide.docx"))
    .isInstanceOf(InvalidWordDocumentException.class).hasMessageContaining("vide.docx").hasMessageContaining("tableau");
  }
 }
 @Test void rejectsCorruptContent() {
  assertThatThrownBy(() -> service.extract(new ByteArrayInputStream(new byte[]{1, 2, 3}), "faux.docx"))
   .isInstanceOf(InvalidWordDocumentException.class).hasMessageContaining("exploitable");
 }
 @Test void missingPeriodIsNonBlocking() throws Exception {
  try (var document = new XWPFDocument()) {
   var table = document.createTable(2, 2);
   table.getRow(0).getCell(0).setText("Application"); table.getRow(0).getCell(1).setText("Travaux effectués");
   table.getRow(1).getCell(0).setText("EPF"); table.getRow(1).getCell(1).setText("Travail");
   var result = service.extract(new ByteArrayInputStream(bytes(document)), "rapport.docx");
   assertThat(result.activities()).hasSize(1); assertThat(result.warnings()).hasSize(1);
  }
 }
}