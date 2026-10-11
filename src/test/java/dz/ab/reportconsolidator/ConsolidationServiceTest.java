package dz.ab.reportconsolidator;
import dz.ab.reportconsolidator.model.*;
import dz.ab.reportconsolidator.service.*;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
class ConsolidationServiceTest {
 private final ConsolidationService service = new ConsolidationService();
 private ExtractedReport report(String app, String... tasks) {
  return new ExtractedReport("Août 2026", List.of(new ApplicationActivity(app, List.of(tasks))), List.of());
 }
 @Test void normalizesCaseWhitespaceAndUnicode() {
  assertThat(service.normalizeApplicationName(" EPF ")).isEqualTo("EPF");
  assertThat(service.normalizeApplicationName("epf")).isEqualTo("EPF");
  assertThat(service.normalizeApplicationName(" Veille\u00a0  Réglementaire ")).isEqualTo("VEILLE RÉGLEMENTAIRE");
 }
 @Test void consolidatesOnlyByApplicationAndPreservesOrder() {
  var result = service.consolidate(List.of(report(" EPF ", "Travail A"), report("epf", "Travail B"),
    report("Ecomp", "Travail C"), report("Evaluation Compétence", "Travail D")));
  assertThat(result.activities()).hasSize(3);
  assertThat(result.activities().get(0).application()).isEqualTo("EPF");
  assertThat(result.activities().get(0).tasks()).containsExactly("Travail A", "Travail B");
 }
 @Test void removesOnlySafeDuplicates() {
  var result = service.consolidate(List.of(report("EPF", "Mise à jour  des données.", "Mise à jour des offres."),
    report("epf", " Mise à jour des données. ", "mise à jour des données.")));
  assertThat(result.duplicates()).isEqualTo(1);
  assertThat(result.activities().get(0).tasks()).hasSize(3);
 }
 @Test void retainsPeriodsAndNonBlockingWarnings() {
  var result = service.consolidate(List.of(report("EPF", "A"),
    new ExtractedReport("", List.of(new ApplicationActivity("EPF", List.of("B"))), List.of("Période absente"))));
  assertThat(result.period()).isEqualTo("Août 2026");
  assertThat(result.warnings()).containsExactly("Période absente");
 }
}