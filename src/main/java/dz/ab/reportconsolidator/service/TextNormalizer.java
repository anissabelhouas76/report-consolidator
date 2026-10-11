package dz.ab.reportconsolidator.service;

import java.text.Normalizer;
import java.util.Locale;

public final class TextNormalizer {
	private TextNormalizer() {
	}

	public static String clean(String text) {
		return text == null ? ""
				: Normalizer.normalize(text, Normalizer.Form.NFC).replaceAll("[\\s\\p{Z}]+", " ").trim();
	}

	public static String normalizeApplicationName(String value) {
		return clean(value).toUpperCase(Locale.ROOT);
	}

	public static String header(String value) {
		return Normalizer.normalize(clean(value), Normalizer.Form.NFD).replaceAll("\\p{M}", "")
				.toLowerCase(Locale.ROOT);
	}

	public static String task(String value) {
		return clean(value.replaceFirst("^\\s*[•●▪◦‣–-]\\s*", ""));
	}
}