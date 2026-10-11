package dz.ab.reportconsolidator.model;

import java.util.List;

public record ConsolidatedReport(String period, List<ApplicationActivity> activities, List<String> warnings,
		int documents, int duplicates) {
	public ConsolidatedReport {
		activities = List.copyOf(activities);
		warnings = List.copyOf(warnings);
	}
}