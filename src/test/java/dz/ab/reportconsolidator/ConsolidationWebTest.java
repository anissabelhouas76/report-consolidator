package dz.ab.reportconsolidator;
import org.apache.poi.xwpf.usermodel.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.*;
import java.io.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT) @AutoConfigureMockMvc
class ConsolidationWebTest {
 @Autowired MockMvc mvc;
 @Autowired org.springframework.boot.test.web.client.TestRestTemplate http;
 private MockMultipartFile report(int index) throws IOException {
  try (var document = new XWPFDocument(); var output = new ByteArrayOutputStream()) {
   document.createParagraph().createRun().setText("Période : " + new String[]{"Août", "Septembre", "Octobre"}[index % 3] + " 2026");
   document.createParagraph().createRun().setText("Élaboré par : Développeur " + index);
   var table = document.createTable(3, 2);
   table.getRow(0).getCell(0).setText("Application"); table.getRow(0).getCell(1).setText("Travaux effectués");
   table.getRow(1).getCell(0).setText(index % 2 == 0 ? "EPF" : " epf ");
   table.getRow(1).getCell(1).setText("Travail " + index);
   table.getRow(1).getCell(1).addParagraph().createRun().setText("Travail commun");
   table.getRow(2).getCell(0).setText("Veille Réglementaire"); table.getRow(2).getCell(1).setText("Workflow");
   document.write(output);
   return new MockMultipartFile("files", "rapport" + index + ".docx", "application/octet-stream", output.toByteArray());
  }
 }
 @Test void consolidatesNineDeveloperReportsIntoOneValidWord() throws Exception {
  var request = multipart("/consolidate");
  for (int i = 0; i < 9; i++) request.file(report(i));
  byte[] bytes = mvc.perform(request).andExpect(status().isOk())
   .andExpect(header().string("Content-Disposition", "attachment; filename=\"Rapport_Consolide.docx\""))
   .andExpect(content().contentType("application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
   .andReturn().getResponse().getContentAsByteArray();
  java.nio.file.Files.write(java.nio.file.Path.of("target/Rapport_Consolide_Exemple.docx"), bytes);
  try (var document = new XWPFDocument(new ByteArrayInputStream(bytes))) {
   var table = document.getTables().get(0);
   assertThat(table.getRows()).hasSize(3);
   assertThat(table.getRow(1).getCell(0).getText()).isEqualTo("EPF");
   assertThat(table.getRow(1).getCell(1).getParagraphs()).hasSize(10);
   assertThat(table.getRow(1).getCell(1).getParagraphs()).allSatisfy(p -> assertThat(p.getNumID()).isNotNull());
   assertThat(table.getText()).doesNotContain("Développeur");
   assertThat(document.getParagraphs().get(1).getText()).contains("Août 2026", "Septembre 2026", "Octobre 2026");
  }
 }
 @Test void servesUploadPage() throws Exception { mvc.perform(get("/")).andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("multiple"))); }
 @Test void rejectsMissingFiles() throws Exception { mvc.perform(multipart("/consolidate")).andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value("Sélectionnez au moins un fichier .docx.")); }
 @Test void explainsDocConversion() throws Exception {
  mvc.perform(multipart("/consolidate").file(new MockMultipartFile("files", "ancien.doc", "", new byte[]{1})))
   .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("converti")));
 }
 @Test void rejectsEmptyAndRenamedFiles() throws Exception {
  mvc.perform(multipart("/consolidate").file(new MockMultipartFile("files", "vide.docx", "", new byte[0])))
   .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("vide")));
  mvc.perform(multipart("/consolidate").file(new MockMultipartFile("files", "faux.docx", "", new byte[]{1})))
   .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("exploitable")));
 }
 @Test void rejectsWholeBatchWhenOneFileIsInvalid() throws Exception {
  mvc.perform(multipart("/consolidate").file(report(0)).file(new MockMultipartFile("files", "faux.docx", "", new byte[]{1})))
   .andExpect(status().isBadRequest());
 }
 @Test void supportsThirtyReports() throws Exception {
  var request = multipart("/consolidate");
  for (int i = 0; i < 30; i++) request.file(report(i));
  mvc.perform(request).andExpect(status().isOk());
 }
 @Test void acceptsThirtyPartsThroughRealTomcat() throws Exception {
  var parts = new org.springframework.util.LinkedMultiValueMap<String, Object>();
  for (int i = 0; i < 30; i++) {
   var file = report(i);
   parts.add("files", new org.springframework.core.io.ByteArrayResource(file.getBytes()) {
    @Override public String getFilename() { return file.getOriginalFilename(); }
   });
  }
  var headers = new org.springframework.http.HttpHeaders();
  headers.setContentType(org.springframework.http.MediaType.MULTIPART_FORM_DATA);
  var result = http.postForEntity("/consolidate", new org.springframework.http.HttpEntity<>(parts, headers), byte[].class);
  assertThat(result.getStatusCode().value()).isEqualTo(200);
  try (var document = new XWPFDocument(new ByteArrayInputStream(result.getBody()))) {
   assertThat(document.getTables().get(0).getRows()).hasSize(3);
  }
 }
}