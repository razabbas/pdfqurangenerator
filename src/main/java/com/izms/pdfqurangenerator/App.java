package com.izms.pdfqurangenerator;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.font.PDType1Font;

import com.ibm.icu.text.ArabicShaping;
import com.ibm.icu.text.ArabicShapingException;
import com.ibm.icu.text.Bidi;

public class App {

	public static void main(String[] args) throws IOException, ArabicShapingException, URISyntaxException {
		PDDocument doc = new PDDocument();
		URL arabicFontResource = App.class.getClassLoader().getResource("font/NotoNaskhArabicUI-Regular.ttf");
		URL quranArabicResource = App.class.getClassLoader().getResource("quran-simple.txt");
		URL quranEnglishResource = App.class.getClassLoader().getResource("en.ahmedali.txt");
		
		File arabicFontFile = new File(arabicFontResource.toURI());
		File arabicQuranFile = new File(quranArabicResource.toURI());
		File englishQuranFile = new File(quranEnglishResource.toURI());
		
		PDFont englishFont = PDType1Font.HELVETICA;
		float englishFontSize = 12f;
		PDFont arabicFont = PDType0Font.load(doc, arabicFontFile);
		float arabicFontSize = 12f;

		PDPage page = new PDPage(PDRectangle.A4);
		doc.addPage(page);

		PDPageContentStream writer = new PDPageContentStream(doc, page);
		writer.beginText();
		PDRectangle mediaBox = page.getMediaBox();
		float marginY = 80;
		float marginX = 60;
		float width = mediaBox.getWidth() - 2.5f * marginX;
		float startX = mediaBox.getLowerLeftX() + marginX;
		float startY = mediaBox.getUpperRightY() - marginY;

		BufferedReader arabicQuranReader = new BufferedReader(new FileReader(arabicQuranFile, StandardCharsets.UTF_8));
		BufferedReader englishQuranReader = new BufferedReader(new FileReader(englishQuranFile, StandardCharsets.UTF_8));
		
		String[] arabicLineParts = arabicQuranReader.readLine().trim().split("\\u007C");
		String[] englishLineParts = englishQuranReader.readLine().trim().split("\\u007C");
		
		int i = 1;
		while (arabicLineParts != null && i < 8) {
			if (arabicLineParts.length < 2) {
				continue;
			}

			String suraNumber = arabicLineParts[0];
			String ayatNumber = arabicLineParts[1];
			String ayat = arabicLineParts[2];
			String translation = englishLineParts[2];
			

			System.out.println(ayat);

			addParagraph(writer, width, startX, startY, ayat, true, arabicFont, arabicFontSize,ayatNumber,"ar");
			startY = 0;
			addParagraph(writer, width, startX, startY, translation, true, englishFont, englishFontSize,ayatNumber,"en");
			writer.newLineAtOffset(0, calcLeading(englishFontSize));
			startY = 0;
			arabicLineParts = arabicQuranReader.readLine().trim().split("\\u007C");
			englishLineParts = englishQuranReader.readLine().trim().split("\\u007C");
			i++;
		}

		writer.endText();
		writer.close();
		doc.save(new File("File_Test.pdf"));
		doc.close();
	}

	private static String bidiReorder(String text) {
		try {
			Bidi bidi = new Bidi((new ArabicShaping(ArabicShaping.LETTERS_SHAPE)).shape(text), 127);
			bidi.setReorderingMode(0);
			return bidi.writeReordered(2);
		} catch (ArabicShapingException ase3) {
			return text;
		}
	}

	private static float calcLeading(float fontSize) {
		return -1.5f * fontSize;
	}

	private static void addParagraph(PDPageContentStream contentStream, float width, float sx, float sy, String text,
			PDFont font, float fontSize, String ayatNumber,String lang) throws IOException {
		addParagraph(contentStream, width, sx, sy, text, false, font, fontSize, ayatNumber,lang);
	}

	private static void addParagraph(PDPageContentStream contentStream, float width, float sx, float sy, String text,
			boolean justify, PDFont font, float fontSize, String ayatNumber,String lang) throws IOException {
		List<String> lines = parseLines(text, width, font, fontSize);
		contentStream.setFont(font, fontSize);
		contentStream.newLineAtOffset(sx, sy);
		float xoffset = 0;
		for (String line : lines) {
			float lineLength = fontSize * font.getStringWidth(line) / 1000;
			xoffset = sx + (width - lineLength);
			contentStream.newLineAtOffset(xoffset, calcLeading(fontSize));
			float charSpacing = 0;
			if (justify) {
				if (line.length() > 1) {
					float size = fontSize * font.getStringWidth(line) / 1000;
					float free = width - size;
					if (free > 0 && !lines.get(lines.size() - 1).equals(line)) {
						charSpacing = free / (line.length() - 1);
					}
				}
			}
			contentStream.setCharacterSpacing(charSpacing);
			if (lang.equalsIgnoreCase("ar")) {
				contentStream.showText("\ufd3e"+ayatNumber+"\ufd3f"+line);
			} else {
				contentStream.showText(line);
			}
			contentStream.newLineAtOffset(-xoffset, 0);
		}
		contentStream.newLineAtOffset(-sx, 0);

	}

	private static List<String> parseLines(String text, float width, PDFont font, float fontSize) throws IOException {
		List<String> lines = new ArrayList<String>();
		int lastSpace = -1;
		while (text.length() > 0) {
			int spaceIndex = text.indexOf(' ', lastSpace + 1);
			if (spaceIndex < 0)
				spaceIndex = text.length();
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

}