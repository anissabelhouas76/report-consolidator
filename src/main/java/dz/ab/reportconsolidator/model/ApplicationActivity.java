package dz.ab.reportconsolidator.model;
import java.util.List;
public record ApplicationActivity(String application, List<String> tasks) {
 public ApplicationActivity { tasks = List.copyOf(tasks); }
}