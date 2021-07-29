package com.izms.pdfqurangenerator;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;

import com.izms.pdfqurangenerator.quranmetadata.Quran;
import com.izms.pdfqurangenerator.quranmetadata.Quran.Suras.Sura;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;

public class App1 {

	public static void main(String... arg) throws JAXBException, IOException, URISyntaxException {

		float arabicFontSize = 12;
		float translationFontSize = 8;
		String bismillah = "ِسْمِ اللَّهِ الرَّحْمَـٰنِ الرَّحِيمِ";
		
		Quran quranMetaData = readQuranMetadataJaxB("quran-data.xml");
		PDFBook quranPdf = new PDFBook("HolyQuran.pdf", PageType.A4, 40, 40);
		// add the fontfiles to the
		PDFont arabicFont = addFontFile(quranPdf, "font/NotoNaskhArabicUI-Bold.ttf");
		PDFont englishFont = addFontFile(quranPdf, "font/en/NotoSans-hinted/NotoSans-Regular.ttf");
		quranPdf.setHeaderFooterFont(englishFont, translationFontSize);
		quranPdf.setSpaceFont(arabicFont, arabicFontSize);
		

		BufferedReader arabicQuranReader = getFileBufferedReader("quran-simple.txt");
		BufferedReader englishQuranReader = getFileBufferedReader("en.ahmedali.txt");

		String[] arabicLineParts = arabicQuranReader.readLine().trim().split("\\u007C");
		String[] englishLineParts = englishQuranReader.readLine().trim().split("\\u007C");

		int previousSuraNumber = 0;
		int i=0;
		while (arabicLineParts != null) {
			/*
			 * if (i > 120) { break; }
			 */
			if (arabicLineParts.length < 2) {
				String arabicLine = arabicQuranReader.readLine();
				String englishLine = englishQuranReader.readLine();
				arabicLineParts = null;
				englishLineParts = null;
				continue;
			}

			int suraNumber = Integer.parseInt(arabicLineParts[0]);
			int ayatNumber = Integer.parseInt(arabicLineParts[1]);

			String ayat = arabicLineParts[2];
			String translation = englishLineParts[2];
			Sura sura = quranMetaData.getSuras().getSura().get(suraNumber - 1);
			if (suraNumber != previousSuraNumber) {
				if (suraNumber > 1) {
					quranPdf.addPage();
				}
				quranPdf.writeLine(sura.getName(), arabicFont, 12, TextAlignment.CENTER);
				quranPdf.writeLine(sura.getEname(), englishFont, 8, TextAlignment.CENTER);
				quranPdf.writeLine(
						"Number:" + sura.getIndex() + " Type:" + sura.getType() + " Num of Ayat:" + sura.getAyas(),
						englishFont, 8, TextAlignment.CENTER);
				quranPdf.writeBlankLine(1);
				previousSuraNumber = suraNumber;
				
				if (suraNumber> 1 && ayat.contains(bismillah)) {
					ayat = ayat.substring(bismillah.length());
					quranPdf.writeLine(bismillah, arabicFont, 12, TextAlignment.CENTER);
				}
				
			}
			
			
			quranPdf.writeLine(ayat+"\uFD3F"+ayatNumber+"\uFD3E", arabicFont, 12, TextAlignment.CENTER);
			quranPdf.writeLine(translation, englishFont, 8, TextAlignment.CENTER);
			quranPdf.writeBlankLine(1);
			arabicLineParts = arabicQuranReader.readLine().trim().split("\\u007C");
			englishLineParts = englishQuranReader.readLine().trim().split("\\u007C");
			i++;
		}

		quranPdf.closeDocument();
		arabicQuranReader.close();
		englishQuranReader.close();

	}

	private static BufferedReader getFileBufferedReader(String fileName) throws URISyntaxException, IOException {
		URL resource = App.class.getClassLoader().getResource(fileName);
		File file = new File(resource.toURI());
		BufferedReader bufferedReader = new BufferedReader(new FileReader(file, StandardCharsets.UTF_8));
		return bufferedReader;
	}

	private static Quran readQuranMetadataJaxB(String xmlMetadataFile)
			throws JAXBException, IOException, URISyntaxException {
		JAXBContext context = JAXBContext.newInstance(Quran.class);
		URL quranMetaDataXML = App.class.getClassLoader().getResource(xmlMetadataFile);
		File quranMetaDataFile = new File(quranMetaDataXML.toURI());
		return (Quran) context.createUnmarshaller()
				.unmarshal(new FileReader(quranMetaDataFile, StandardCharsets.UTF_8));

	}

	private static PDType0Font addFontFile(PDFBook book, String fontFileName) throws URISyntaxException, IOException {
		URL fontResource = App.class.getClassLoader().getResource(fontFileName);
		File fontFile = new File(fontResource.toURI());
		return book.addFontFile(fontFile);
	}

}
