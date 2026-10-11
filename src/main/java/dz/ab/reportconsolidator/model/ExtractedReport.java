package dz.ab.reportconsolidator.model;

import java.util.List;

public record ExtractedReport(String period, List<ApplicationActivity> activities, List<String> warnings) {
	public ExtractedReport {
		activities = List.copyOf(activities);
		warnings = List.copyOf(warnings);
	}
}