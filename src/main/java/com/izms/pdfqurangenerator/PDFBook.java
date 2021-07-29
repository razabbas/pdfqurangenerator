package com.izms.pdfqurangenerator;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;

import com.ibm.icu.text.ArabicShaping;
import com.ibm.icu.text.ArabicShapingException;
import com.ibm.icu.text.Bidi;

public class PDFBook {
	private PDDocument doc;
	private PDPage currentPage;
	private PDPageContentStream currentContentStream;
	private String fileName;
	private float xMargin;
	private float yMargin;
	private PDRectangle pageType;
	private float width;
	private float currentY;
	
	public PDFBook(String fileName, PDRectangle pageType, float xMargin, float yMargin) throws IOException {
		this.fileName = fileName;
		this.xMargin = xMargin;
		this.yMargin = yMargin;
		this.pageType = pageType;
		this.width= pageType.getWidth() - 2f * xMargin;
		this.doc = new PDDocument();
		this.currentPage = new PDPage(pageType);
		
		this.doc.addPage(currentPage);
		this.currentContentStream = new PDPageContentStream(doc, currentPage);
		this.currentContentStream.beginText();
		currentY = pageType.getUpperRightY() - yMargin;
		currentContentStream.newLineAtOffset(pageType.getLowerLeftX() + xMargin, pageType.getUpperRightY() - yMargin);
	}
	
	
	public PDType0Font addFontFile(File fontFile) throws IOException {
		
		return PDType0Font.load(doc, fontFile);
		
	}
	
	public void addPage() throws IOException {
		if (currentContentStream != null) {
			currentContentStream.endText();
			currentContentStream.close();
		}
		
		currentPage = new PDPage(PDRectangle.A4);
		doc.addPage(currentPage);
		currentContentStream = new PDPageContentStream(doc, currentPage);
		currentContentStream.beginText();
		currentContentStream.newLineAtOffset(pageType.getLowerLeftX() + xMargin, pageType.getUpperRightY() - yMargin);
		currentY = pageType.getUpperRightY() - yMargin;
		
	}
	
	public void closeDocument() throws IOException {
		currentContentStream.endText();
		currentContentStream.close();
		doc.save(new File(fileName));
		doc.close();
	}
	
	public void writeLine(String text, PDFont font, float fontSize, String alignment) throws IOException {
		List<String> lines = parseLines(text, font, fontSize);
		for (String line : lines) {
			if (currentY+calcLeading(fontSize) <= 0 ){
				addPage();
			}
			currentContentStream.setFont(font, fontSize);
			currentContentStream.showText(line);
			currentContentStream.newLineAtOffset(0,calcLeading(fontSize));
			currentY = currentY+calcLeading(fontSize);
			
		}
	}
	
	private float calcLeading(float fontSize) {
		return -1.5f * fontSize;
	}
	
	
	private List<String> parseLines(String text, PDFont font, float fontSize) throws IOException {
		List<String> lines = new ArrayList<String>();
		int lastSpace = -1;
		while (text.length() > 0) {
			int spaceIndex = text.indexOf(' ', lastSpace + 1);
			if (spaceIndex < 0) {
				spaceIndex = text.length();
			}
			String subString = text.substring(0, spaceIndex);
			float size = fontSize * font.getStringWidth(subString) / 1000;
			if (size > width) {
				if (lastSpace < 0) {
					lastSpace = spaceIndex;
				}
				subString = text.substring(0, lastSpace);
				lines.add(bidiReorder(subString));
				text = text.substring(lastSpace).trim();
				lastSpace = -1;
			} else if (spaceIndex == text.length()) {
				lines.add(bidiReorder(text));
				text = "";
			} else {
				lastSpace = spaceIndex;
			}
		}
		return lines;
	}
	
	private String bidiReorder(String text) {
		try {
			Bidi bidi = new Bidi((new ArabicShaping(ArabicShaping.LETTERS_SHAPE)).shape(text), 127);
			bidi.setReorderingMode(0);
			return bidi.writeReordered(2);
		} catch (ArabicShapingException ase3) {
			return text;
		}
	}
	
	
	
}
