package dz.ab.reportconsolidator.service;

import dz.ab.reportconsolidator.exception.InvalidWordDocumentException;
import dz.ab.reportconsolidator.model.*;
import org.apache.poi.xwpf.usermodel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.io.*;
import java.util.*;
import java.util.regex.Pattern;

@Service
public class WordExtractionService {
	private static final Logger log = LoggerFactory.getLogger(WordExtractionService.class);
	private static final Pattern PERIOD = Pattern.compile("(?iu)p[ée]riode\\s*[:：]\\s*([^\\r\\n]+)");

	public ExtractedReport extract(InputStream input, String filename) {
		log.info("Lecture du fichier : {}", filename);
		try (XWPFDocument document = new XWPFDocument(input)) {
			List<ApplicationActivity> activities = new ArrayList<>();
			List<String> warnings = new ArrayList<>();
			int found = scan(document.getBodyElements(), activities, warnings);
			if (found == 0)
				throw invalid(filename, "ne contient pas de tableau d’activités identifiable.");
			if (activities.isEmpty())
				throw invalid(filename, "ne contient aucun travail exploitable.");
			String period = findPeriod(document.getBodyElements());
			if (period.isBlank())
				warnings.add("Période non détectée dans " + filename + ".");
			log.info("{} : {} entrées d’applications extraites.", filename, activities.size());
			return new ExtractedReport(period, activities, warnings);
		} catch (InvalidWordDocumentException e) {
			throw e;
		} catch (IOException | RuntimeException e) {
			log.warn("Document illisible : {} ({})", filename, e.getClass().getSimpleName());
			throw invalid(filename,
					"n’est pas un document Word .docx exploitable (corrompu, chiffré ou autre format).");
		}
	}

	private int scan(List<IBodyElement> elements, List<ApplicationActivity> activities, List<String> warnings) {
		int found = 0;
		for (IBodyElement element : elements) {
			if (!(element instanceof XWPFTable table))
				continue;
			int appColumn = -1, taskColumn = -1;
			String previousApplication = "";
			for (XWPFTableRow row : table.getRows()) {
				List<XWPFTableCell> cells = row.getTableCells();
				int appHeader = -1, taskHeader = -1;
				for (int i = 0; i < cells.size(); i++) {
					String header = TextNormalizer.header(cells.get(i).getText());
					if (header.equals("application"))
						appHeader = i;
					if (header.equals("travaux effectues"))
						taskHeader = i;
				}
				if (appHeader >= 0 && taskHeader >= 0) {
					appColumn = appHeader;
					taskColumn = taskHeader;
					found++;
					log.debug("Tableau d’activités trouvé.");
					continue;
				}
				if (appColumn >= 0 && cells.size() > Math.max(appColumn, taskColumn)) {
					String app = TextNormalizer.clean(cells.get(appColumn).getText());
					List<String> tasks = extractTasks(cells.get(taskColumn));
					// Une cellule fusionnée verticalement poursuit l’application précédente.
					var properties = cells.get(appColumn).getCTTc().getTcPr();
					boolean mergedContinuation = properties != null && properties.isSetVMerge()
							&& (properties.getVMerge().getVal() == null
									|| "continue".equals(properties.getVMerge().getVal().toString()));
					if (app.isEmpty() && mergedContinuation)
						app = previousApplication;
					if (!app.isBlank())
						previousApplication = app;
					if (!app.isBlank() && !tasks.isEmpty())
						activities.add(new ApplicationActivity(app, tasks));
					else if (app.isBlank() && !tasks.isEmpty())
						warnings.add("Une ligne sans application a été ignorée.");
				}
				for (XWPFTableCell cell : cells)
					found += scan(cell.getBodyElements(), activities, warnings);
			}
		}
		return found;
	}

	public List<String> extractTasks(XWPFTableCell cell) {
		List<String> tasks = new ArrayList<>();
		for (IBodyElement element : cell.getBodyElements()) {
			if (element instanceof XWPFParagraph paragraph) {
				for (String line : paragraph.getText().split("\\R")) {
					String task = TextNormalizer.task(line);
					if (!task.isBlank())
						tasks.add(task);
				}
			} else if (element instanceof XWPFTable table) {
				for (XWPFTableRow row : table.getRows())
					for (XWPFTableCell nested : row.getTableCells())
						tasks.addAll(extractTasks(nested));
			}
		}
		return tasks;
	}

	private String findPeriod(List<IBodyElement> elements) {
		for (IBodyElement element : elements) {
			if (element instanceof XWPFParagraph paragraph) {
				var matcher = PERIOD.matcher(paragraph.getText());
				if (matcher.find())
					return TextNormalizer.clean(matcher.group(1));
			} else if (element instanceof XWPFTable table) {
				for (XWPFTableRow row : table.getRows()) {
					for (XWPFTableCell cell : row.getTableCells()) {
						String period = findPeriod(cell.getBodyElements());
						if (!period.isEmpty())
							return period;
					}
					for (int i = 0; i + 1 < row.getTableCells().size(); i++) {
						if (TextNormalizer.header(row.getCell(i).getText()).matches("periode\\s*:?")) {
							String value = TextNormalizer.clean(row.getCell(i + 1).getText());
							if (!value.isBlank())
								return value;
						}
					}
				}
			}
		}
		return "";
	}

	private InvalidWordDocumentException invalid(String name, String reason) {
		return new InvalidWordDocumentException("Le fichier " + name + " " + reason);
	}
}