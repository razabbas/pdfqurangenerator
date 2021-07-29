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
	
	public static void main(String...arg) throws JAXBException, IOException, URISyntaxException {
		
		Quran quranMetaData = readQuranMetadataJaxB("quran-data.xml");
		PDFBook quranPdf = new PDFBook("HolyQuran.pdf", PDRectangle.A4, 40, 40);
		//add the fontfiles to the 
		PDFont arabicFont = addFontFile(quranPdf,"font/NotoNaskhArabicUI-Bold.ttf");
		PDFont englishFont = addFontFile(quranPdf,"font/en/NotoSans-hinted/NotoSans-Regular.ttf");
	
		BufferedReader arabicQuranReader = getFileBufferedReader("quran-simple.txt");
		BufferedReader englishQuranReader = getFileBufferedReader("en.ahmedali.txt");

		String[] arabicLineParts = arabicQuranReader.readLine().trim().split("\\u007C");
		String[] englishLineParts = englishQuranReader.readLine().trim().split("\\u007C");
		
		int i=0;
		int previousSuraNumber = 0;
		
		while (arabicLineParts != null && i < 60) {
			if (arabicLineParts.length < 2) {
				continue;
			}

			int suraNumber = Integer.parseInt(arabicLineParts[0]);
			String ayatNumber = arabicLineParts[1];
			String ayat = arabicLineParts[2];
			String translation = englishLineParts[2];
			Sura sura = quranMetaData.getSuras().getSura().get(suraNumber-1);
			if (suraNumber != previousSuraNumber) {
				quranPdf.writeLine(sura.getName(), arabicFont, 12, "CENTER");
				quranPdf.writeLine(sura.getEname(), englishFont, 12, "CENTER");
				quranPdf.writeLine("Number:"+sura.getIndex()+" Type:"+sura.getType()+" Num of Ayat:"+sura.getAyas(), englishFont, 12, "CENTER");
				previousSuraNumber = suraNumber;
			}
			quranPdf.writeLine(ayat, arabicFont, 12, "RIGHT");
			quranPdf.writeLine(translation, englishFont, 12, "LEFT");
			
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
