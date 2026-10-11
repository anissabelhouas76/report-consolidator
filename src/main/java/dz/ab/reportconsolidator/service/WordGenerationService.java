package dz.ab.reportconsolidator.service;

import dz.ab.reportconsolidator.exception.ConsolidationException;
import dz.ab.reportconsolidator.model.*;
import org.apache.poi.xwpf.usermodel.*;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.*;
import org.springframework.stereotype.Service;
import java.io.*;
import java.math.BigInteger;

@Service
public class WordGenerationService {
	public byte[] generate(ConsolidatedReport report) {
		try (XWPFDocument document = new XWPFDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
			var section = document.getDocument().getBody().addNewSectPr();
			var page = section.addNewPgSz();
			page.setW(BigInteger.valueOf(12240));
			page.setH(BigInteger.valueOf(15840));
			var margins = section.addNewPgMar();
			margins.setTop(BigInteger.valueOf(1080));
			margins.setBottom(BigInteger.valueOf(1080));
			margins.setLeft(BigInteger.valueOf(1080));
			margins.setRight(BigInteger.valueOf(1080));
			XWPFParagraph title = document.createParagraph();
			title.setStyle("Title");
			title.setAlignment(ParagraphAlignment.CENTER);
			style(title.createRun(), "RAPPORT D’ACTIVITÉ CONSOLIDÉ", true, 19, "000000");
			style(document.createParagraph().createRun(), "Période : " + report.period(), false, 11, "344054");
			XWPFTable table = document.createTable(1, 2);
			table.setWidth("100%");
			table.getRow(0).setRepeatHeader(true);
			for (int i = 0; i < 2; i++) {
				XWPFTableCell cell = table.getRow(0).getCell(i);
				cell.setColor("17365D");
				cell.setWidth(i == 0 ? "25%" : "75%");
				style(cell.getParagraphs().get(0).createRun(), i == 0 ? "Application" : "Travaux effectués", true,
						11, "FFFFFF");
			}
			BigInteger bullet = createBullet(document);
			for (ApplicationActivity activity : report.activities()) {
				XWPFTableRow row = table.createRow();
				row.setCantSplitRow(false);
				row.getCell(0).setWidth("25%");
				row.getCell(1).setWidth("75%");
				style(row.getCell(0).getParagraphs().get(0).createRun(), activity.application(), true, 11,
						"17365D");
				XWPFTableCell cell = row.getCell(1);
				for (int i = 0; i < activity.tasks().size(); i++) {
					XWPFParagraph paragraph = i == 0 ? cell.getParagraphs().get(0) : cell.addParagraph();
					paragraph.setNumID(bullet);
					paragraph.setSpacingAfter(80);
					style(paragraph.createRun(), activity.tasks().get(i), false, 11, "202939");
				}
			}
			if (!report.warnings().isEmpty()) {
				style(document.createParagraph().createRun(), "Avertissements", true, 11, "000000");
				for (String warning : report.warnings())
					style(document.createParagraph().createRun(), warning, false, 10, "667085");
			}
			document.write(output);
			return output.toByteArray();
		} catch (IOException | RuntimeException e) {
			throw new ConsolidationException("La génération du rapport Word a échoué.", e);
		}
	}

	private BigInteger createBullet(XWPFDocument document) {
		CTAbstractNum abstractNum = CTAbstractNum.Factory.newInstance();
		abstractNum.setAbstractNumId(BigInteger.ZERO);
		CTLvl level = abstractNum.addNewLvl();
		level.setIlvl(BigInteger.ZERO);
		level.addNewStart().setVal(BigInteger.ONE);
		level.addNewNumFmt().setVal(STNumberFormat.BULLET);
		level.addNewLvlText().setVal("•");
		var indent = level.addNewPPr().addNewInd();
		indent.setLeft(BigInteger.valueOf(360));
		indent.setHanging(BigInteger.valueOf(180));
		XWPFNumbering numbering = document.createNumbering();
		return numbering.addNum(numbering.addAbstractNum(new XWPFAbstractNum(abstractNum)));
	}

	private void style(XWPFRun run, String text, boolean bold, int size, String color) {
		run.setText(text);
		run.setBold(bold);
		run.setFontFamily("Calibri");
		run.setFontSize(size);
		run.setColor(color);
	}
}