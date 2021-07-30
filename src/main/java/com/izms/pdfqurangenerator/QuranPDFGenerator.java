package com.izms.pdfqurangenerator;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;

import com.izms.pdfqurangenerator.quranmetadata.Quran;
import com.izms.pdfqurangenerator.quranmetadata.Quran.Suras.Sura;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;

public class QuranPDFGenerator {

	private static String outputFileName;
	private static String quranMetadataFileXml;
	private static String quranTextFile;

	private static String arabicFontFile;
	private static float arabicFontSize;

	private static String englishFontFile = "font/en/NotoSans-hinted/NotoSans-Medium.ttf";
	private static float englishFontSize = 10;

	private static String translationTextFile;
	private static String translationFontFile;
	private static float translationFontSize;
	private static boolean translationRtl;

	private static String BISMILLAH = "ِسْمِ اللَّهِ الرَّحْمَـٰنِ الرَّحِيمِ";

	public static void main(String... args) {
		try {
			processCommandLine(args);
			Quran quranMetaData;

			quranMetaData = readQuranMetadataJaxB(quranMetadataFileXml);

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
					quranPdf.writeLine(sura.getName(), arabicFont, arabicFontSize, TextAlignment.CENTER, true);
					quranPdf.writeLine(sura.getEname(), englishFont, englishFontSize, TextAlignment.CENTER);
					quranPdf.writeLine(
							"Number:" + sura.getIndex() + " Type:" + sura.getType() + " Num of Ayat:" + sura.getAyas(),
							englishFont, englishFontSize, TextAlignment.CENTER);
					quranPdf.writeBlankLine(1);
					previousSuraNumber = suraNumber;

					if (suraNumber > 1 && ayat.contains(BISMILLAH)) {
						ayat = ayat.substring(BISMILLAH.length());
						quranPdf.writeLine(BISMILLAH, arabicFont, arabicFontSize, TextAlignment.CENTER, true);
					}

				}

				quranPdf.writeLine(ayat + "\uFD3F" + ayatNumber + "\uFD3E", arabicFont, arabicFontSize,
						TextAlignment.CENTER, true);
				quranPdf.writeLine(translation, translationFont, translationFontSize, TextAlignment.CENTER,
						translationRtl);
				quranPdf.writeBlankLine(1);
				arabicLineParts = arabicQuranReader.readLine().trim().split("\\u007C");
				translationLineParts = translationQuranReader.readLine().trim().split("\\u007C");

			}

			quranPdf.closeDocument();
			arabicQuranReader.close();
			translationQuranReader.close();
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}

	}

	private static BufferedReader getFileBufferedReader(String fileName) throws URISyntaxException, IOException {
		URL resource = QuranPDFGenerator.class.getClassLoader().getResource(fileName);
		if (resource == null) {
			System.err.println("The following file was not found:"+fileName);
			throw new IOException("File Not found  "+fileName+" not fount");
		}
		File file = new File(resource.toURI());
		BufferedReader bufferedReader = new BufferedReader(new FileReader(file, StandardCharsets.UTF_8));
		return bufferedReader;
	}

	private static Quran readQuranMetadataJaxB(String xmlMetadataFile)
			throws JAXBException, IOException, URISyntaxException {
		JAXBContext context = JAXBContext.newInstance(Quran.class);
		URL quranMetaDataXML = QuranPDFGenerator.class.getClassLoader().getResource(xmlMetadataFile);
		if (quranMetaDataXML == null) {
			System.err.println("The quran metadata file was not found:"+quranMetaDataXML);
			throw new IOException("Quran metadata  "+quranMetaDataXML+" not fount");
		}
		File quranMetaDataFile = new File(quranMetaDataXML.toURI());
		return (Quran) context.createUnmarshaller()
				.unmarshal(new FileReader(quranMetaDataFile, StandardCharsets.UTF_8));

	}

	private static PDType0Font addFontFile(PDFBook book, String fontFileName) throws URISyntaxException, IOException {
		URL fontResource = QuranPDFGenerator.class.getClassLoader().getResource(fontFileName);
		if (fontResource == null) {
			System.err.println("The following font file was not found:"+fontFileName);
			throw new IOException("Font file "+fontFileName+" not fount");
		}
		File fontFile = new File(fontResource.toURI());
		return book.addFontFile(fontFile);
	}

	private static void processCommandLine(String[] args) {
		// create the command line parser
		CommandLineParser parser = new DefaultParser();
		// create the Options
		Options options = new Options();
		options.addOption("o", "output-file-name", true, "output pdf file");
		options.addRequiredOption("q", "quran-metadata-xmlfile", true, "quran metadata xml file");
		options.addOption("a", "arabic-font-file", true, "arabic font file");
		options.addOption("s", "arabic-font-size", true, "arabic font size");
		options.addOption("t", "quran-text-file", true, "quran text file");
		options.addOption("r", "translation-font-file", true, "translation font file");
		options.addOption("z", "translation-font-size", true, "translation font size");
		options.addOption("x", "translation-text-file", true, "translation text file");
		options.addOption("l", "translation-rtl", true, "is translation right to left");
		try {
			CommandLine cmd = parser.parse(options, args);
			if (cmd.hasOption("o")) {
				outputFileName = cmd.getOptionValue("o");
			} else {
				outputFileName = "HolyQuran.pdf";
			}

			if (cmd.hasOption("q")) {
				quranMetadataFileXml = cmd.getOptionValue("q");
			} else {
				quranMetadataFileXml = "quranmetadata/quran-data.xml";
			}

			if (cmd.hasOption("a")) {
				arabicFontFile = cmd.getOptionValue("a");
			} else {
				arabicFontFile = "font/ar/NotoNaskhArabic-hinted/NotoNaskhArabicUI-Bold.ttf";
			}

			if (cmd.hasOption("s")) {
				arabicFontSize = Float.parseFloat(cmd.getOptionValue("s"));
			} else {
				arabicFontSize = 18f;
			}

			if (cmd.hasOption("t")) {
				quranTextFile = cmd.getOptionValue("t");
			} else {
				quranTextFile = "arabic/quran-simple.txt";
			}

			if (cmd.hasOption("r")) {
				translationFontFile = cmd.getOptionValue("r");
			} else {
				translationFontFile = "font/en/NotoSans-hinted/NotoSans-Medium.ttf";
			}

			if (cmd.hasOption("z")) {
				translationFontSize = Float.parseFloat(cmd.getOptionValue("z"));
			} else {
				translationFontSize = 10f;
			}

			if (cmd.hasOption("x")) {
				translationTextFile = cmd.getOptionValue("x");
			} else {
				translationTextFile = "translations/en.ahmedali.txt";
			}

			if (cmd.hasOption("l")) {
				translationRtl = Boolean.parseBoolean(cmd.getOptionValue("l"));
			} else {
				translationRtl = false;
			}

		} catch (ParseException e) {
			System.out.println("Error in command line arguments");
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp("java QuranPDFGenerator", options);
			System.exit(1);
		}
	}

}
