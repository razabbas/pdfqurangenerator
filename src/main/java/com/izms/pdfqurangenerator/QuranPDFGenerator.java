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

public class QuranPDFGenerator {
	
	private static String outputFileName = "HolyQuran.pdf";
	private static String quranMetadataFileXml = "quranmetadata/quran-data.xml";
	private static String arabicFontFile = "font/ar/NotoNaskhArabic-hinted/NotoNaskhArabicUI-Bold.ttf";
	
	private static float arabicFontSize = 18;
	private static String englishFontFile="font/en/NotoSans-hinted/NotoSans-Medium.ttf";
	private static float englishFontSize=10;
	
	private static String quranTextFile="arabic/quran-simple.txt";
	
	private static String translationTextFile="translations/fr.hamidullah.txt";
	private static String translationFontFile="font/fr/NotoSansDisplay-hinted/NotoSansDisplay-Medium.ttf";
	private static float translationFontSize=10;
	private static boolean translationRtl= false;
	
	private static String BISMILLAH = "ِسْمِ اللَّهِ الرَّحْمَـٰنِ الرَّحِيمِ";

	public static void main(String... arg) throws JAXBException, IOException, URISyntaxException {
		
		Quran quranMetaData = readQuranMetadataJaxB(quranMetadataFileXml);
		PDFBook quranPdf = new PDFBook(outputFileName, PageType.LETTER, 20, 20);
		// add the fontfiles to the
		PDFont arabicFont = addFontFile(quranPdf, arabicFontFile);
		PDFont englishFont = addFontFile(quranPdf, englishFontFile);
		PDFont translationFont = addFontFile(quranPdf, translationFontFile);
		quranPdf.setHeaderFooterFont(translationFont, translationFontSize);
		quranPdf.setSpaceFont(arabicFont, arabicFontSize);
		

		BufferedReader arabicQuranReader = getFileBufferedReader(quranTextFile);
		BufferedReader translationQuranReader = getFileBufferedReader(translationTextFile);

		String[] arabicLineParts = arabicQuranReader.readLine().trim().split("\\u007C");
		String[] translationLineParts = translationQuranReader.readLine().trim().split("\\u007C");

		int previousSuraNumber = 0;
		while (arabicLineParts != null) {
			if (arabicLineParts.length < 2) {
				arabicQuranReader.readLine();
				translationQuranReader.readLine();
				arabicLineParts = null;
				translationLineParts = null;
				continue;
			}

			int suraNumber = Integer.parseInt(arabicLineParts[0]);
			int ayatNumber = Integer.parseInt(arabicLineParts[1]);

			String ayat = arabicLineParts[2];
			String translation = translationLineParts[2];
			Sura sura = quranMetaData.getSuras().getSura().get(suraNumber - 1);
			if (suraNumber != previousSuraNumber) {
				if (suraNumber > 1) {
					quranPdf.addPage();
				}
				quranPdf.writeLine(sura.getName(), arabicFont, arabicFontSize, TextAlignment.CENTER,true);
				quranPdf.writeLine(sura.getEname(), englishFont, englishFontSize, TextAlignment.CENTER);
				quranPdf.writeLine(
						"Number:" + sura.getIndex() + " Type:" + sura.getType() + " Num of Ayat:" + sura.getAyas(),
						englishFont, englishFontSize, TextAlignment.CENTER);
				quranPdf.writeBlankLine(1);
				previousSuraNumber = suraNumber;
				
				if (suraNumber> 1 && ayat.contains(BISMILLAH)) {
					ayat = ayat.substring(BISMILLAH.length());
					quranPdf.writeLine(BISMILLAH, arabicFont, arabicFontSize, TextAlignment.CENTER,true);
				}
				
			}
			
			
			quranPdf.writeLine(ayat+"\uFD3F"+ayatNumber+"\uFD3E", arabicFont, arabicFontSize, TextAlignment.CENTER,true);
			quranPdf.writeLine(translation, translationFont, translationFontSize, TextAlignment.CENTER,translationRtl);
			quranPdf.writeBlankLine(1);
			arabicLineParts = arabicQuranReader.readLine().trim().split("\\u007C");
			translationLineParts = translationQuranReader.readLine().trim().split("\\u007C");
		
		}

		quranPdf.closeDocument();
		arabicQuranReader.close();
		translationQuranReader.close();

	}

	private static BufferedReader getFileBufferedReader(String fileName) throws URISyntaxException, IOException {
		URL resource = POCApp.class.getClassLoader().getResource(fileName);
		File file = new File(resource.toURI());
		BufferedReader bufferedReader = new BufferedReader(new FileReader(file, StandardCharsets.UTF_8));
		return bufferedReader;
	}

	private static Quran readQuranMetadataJaxB(String xmlMetadataFile)
			throws JAXBException, IOException, URISyntaxException {
		JAXBContext context = JAXBContext.newInstance(Quran.class);
		URL quranMetaDataXML = POCApp.class.getClassLoader().getResource(xmlMetadataFile);
		File quranMetaDataFile = new File(quranMetaDataXML.toURI());
		return (Quran) context.createUnmarshaller()
				.unmarshal(new FileReader(quranMetaDataFile, StandardCharsets.UTF_8));

	}

	private static PDType0Font addFontFile(PDFBook book, String fontFileName) throws URISyntaxException, IOException {
		URL fontResource = POCApp.class.getClassLoader().getResource(fontFileName);
		File fontFile = new File(fontResource.toURI());
		return book.addFontFile(fontFile);
	}

}
