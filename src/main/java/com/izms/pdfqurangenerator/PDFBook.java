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
	private float pageWidth;
	private float pageHeight;
	private float currentX;
	private float currentY;
	private int lineCounter;
	private int pageCounter;
	private PDFont headerFooterFont;
	private float headerFooterFontSize;
	private PDFont spaceFont;
	private float spaceFontSize;

	public PDFBook(String fileName, PageType pageType, float xMargin, float yMargin) throws IOException {
		this.fileName = fileName;
		this.xMargin = xMargin;
		this.yMargin = yMargin;
		this.lineCounter = 0;
		this.pageCounter = 1;
		this.pageType = getPDRectangleFromPageType(pageType);
		this.pageWidth = this.pageType.getWidth() - 2f * xMargin;
		this.pageHeight = this.pageType.getWidth() - 2f * yMargin;
		this.doc = new PDDocument();
		this.currentPage = new PDPage(this.pageType);
		this.doc.addPage(currentPage);
		this.currentContentStream = new PDPageContentStream(doc, currentPage);
		this.currentContentStream.beginText();
		resetPageCoordinates();
	}
	
	public void setHeaderFooterFont(PDFont font, float fontSize) throws IOException {
		headerFooterFont = font;
		headerFooterFontSize = fontSize;
	}
	
	public void setSpaceFont(PDFont font, float fontSize) throws IOException {
		spaceFont = font;
		spaceFontSize = fontSize;
	}
	
	private void resetPageCoordinates() throws IOException {
		this.currentY = this.pageType.getUpperRightY() - yMargin;
		this.currentX = this.pageType.getLowerLeftX() + xMargin;
		this.currentContentStream.newLineAtOffset(this.currentX, this.currentY);
	}

	private PDRectangle getPDRectangleFromPageType(PageType pageType) {
		switch (pageType) {
		case LETTER: 
			return PDRectangle.LETTER;	
		case LEGAL: 
			return PDRectangle.LEGAL;
		case A0:
			return PDRectangle.A0;
		case A1:
			return PDRectangle.A1;
		case A2:
			return PDRectangle.A2;
		case A3:
			return PDRectangle.A3;
		case A4:
			return PDRectangle.A4;
		case A5:
			return PDRectangle.A5;
		case A6:
			return PDRectangle.A6;
		default:
			throw new IllegalArgumentException("Unexpected value: " + pageType);
		}
	}

	public PDType0Font addFontFile(File fontFile) throws IOException {

		return PDType0Font.load(doc, fontFile);

	}

	public void addPage() throws IOException {
		if (currentContentStream != null) {
			float xoffset = (pageWidth - Integer.toString(pageCounter).length())/2;
			currentContentStream.newLineAtOffset(xoffset,-currentY+yMargin );
			currentContentStream.setFont(headerFooterFont, headerFooterFontSize);
			currentContentStream.showText(""+pageCounter);
			currentContentStream.newLineAtOffset(-xoffset, 0);
			currentContentStream.endText();
			currentContentStream.close();
		}
		
		currentPage = new PDPage(PDRectangle.A4);
		doc.addPage(currentPage);
		pageCounter++;
		currentContentStream = new PDPageContentStream(doc, currentPage);
		currentContentStream.beginText();
		resetPageCoordinates();

	}

	public void closeDocument() throws IOException {
		currentContentStream.endText();
		currentContentStream.close();
		doc.save(new File(fileName));
		doc.close();
	}
	
	public void writeBlankLine(int numberOfBlankLines) throws IOException {
		List<String> lines = new ArrayList<>();
		for (int i=0; i<numberOfBlankLines; i++) {
			lines.add(" ");
		}
		writeLine(lines,spaceFont,spaceFontSize,TextAlignment.LEFT);
	}
	
	public void writeLine(String text, PDFont font, float fontSize, TextAlignment alignment) throws IOException {
		List<String> lines = parseLines(text, font, fontSize);
		writeLine(lines,font,fontSize,alignment);
	}
	
	public void writeLine(List<String> lines, PDFont font, float fontSize, TextAlignment alignment) throws IOException {
		float lineLength = 0;
		float xoffset = 0;
		
		for (String line : lines) {
			lineLength = getLineWidth(font, fontSize, line);
			
			if (currentY-2*yMargin + calcLeading(fontSize) <= 0) {
				addPage();
				lineCounter = 0;
			}
			if (lineCounter > 0) {
				currentContentStream.newLineAtOffset(0, calcLeading(fontSize));
			}
			
			if (alignment == TextAlignment.CENTER) {
				xoffset = (pageWidth - lineLength)/2;
				currentContentStream.newLineAtOffset(xoffset, 0);
			}
			
			if (alignment == TextAlignment.RIGHT) {
				xoffset = pageWidth - lineLength;
				currentContentStream.newLineAtOffset(xoffset, 0);
			}
			
			currentContentStream.setFont(font, fontSize);
			currentContentStream.showText(line);
			lineCounter++;
			currentY = currentY + calcLeading(fontSize);
			currentContentStream.newLineAtOffset(-xoffset, 0);
		}
	}

	private float calcLeading(float fontSize) {
		return -1.5f * fontSize;
	}
	
	private float getLineWidth(PDFont font, float fontSize, String text) throws IOException {
		return fontSize * font.getStringWidth(text)/1000;
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
			String reorderedString = bidiReorder(subString);
			float reorderedDiff = Math.abs(getLineWidth(font, fontSize, reorderedString) - getLineWidth(font, fontSize, subString));
			float size = getLineWidth(font, fontSize,reorderedString);
			if (size > (pageWidth-reorderedDiff)) {
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
