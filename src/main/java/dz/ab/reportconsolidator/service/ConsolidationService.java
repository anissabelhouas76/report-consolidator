package dz.ab.reportconsolidator.service;

import dz.ab.reportconsolidator.model.*;
import org.slf4j.*;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class ConsolidationService {
	private static final Logger log = LoggerFactory.getLogger(ConsolidationService.class);

	public String normalizeApplicationName(String value) {
		return TextNormalizer.normalizeApplicationName(value);
	}

	public ConsolidatedReport consolidate(List<ExtractedReport> reports) {
		Map<String, String> names = new LinkedHashMap<>();
		Map<String, LinkedHashSet<String>> grouped = new LinkedHashMap<>();
		Set<String> periods = new LinkedHashSet<>();
		List<String> warnings = new ArrayList<>();
		int duplicates = 0;
		for (ExtractedReport report : reports) {
			if (!report.period().isBlank())
				periods.add(TextNormalizer.clean(report.period()));
			warnings.addAll(report.warnings());
			for (ApplicationActivity activity : report.activities()) {
				String key = normalizeApplicationName(activity.application());
				if (key.isBlank())
					continue;
				names.putIfAbsent(key, TextNormalizer.clean(activity.application()));
				Set<String> tasks = grouped.computeIfAbsent(key, ignored -> new LinkedHashSet<>());
				for (String raw : activity.tasks()) {
					String task = TextNormalizer.clean(raw);
					if (!task.isBlank() && !tasks.add(task))
						duplicates++;
				}
			}
		}
		List<ApplicationActivity> activities = grouped.entrySet().stream().filter(entry -> !entry.getValue().isEmpty())
				.map(entry -> new ApplicationActivity(names.get(entry.getKey()), new ArrayList<>(entry.getValue())))
				.toList();
		log.info("{} documents analysés ; {} applications ; {} travaux ; {} doublons supprimés.", reports.size(),
				activities.size(), activities.stream().mapToInt(a -> a.tasks().size()).sum(), duplicates);
		return new ConsolidatedReport(periods.isEmpty() ? "Non précisée" : String.join(" – ", periods), activities,
				warnings, reports.size(), duplicates);
	}
}