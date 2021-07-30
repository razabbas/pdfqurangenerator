# pdfqurangenerator
This java program generates quran from arabic text and corresponding translation.

# Usage
usage: java QuranPDFGenerator
 -a,--arabic-font-file <arg>         arabic font file
 -l,--translation-rtl <arg>          is translation right to left
 -o,--output-file-name <arg>         output pdf file
 -q,--quran-metadata-xmlfile <arg>   quran metadata xml file
 -r,--translation-font-file <arg>    translation font file
 -s,--arabic-font-size <arg>         arabic font size
 -t,--quran-text-file <arg>          quran text file
 -x,--translation-text-file <arg>    translation text file
 -z,--translation-font-size <arg>    translation font size
 
 The various file locations given above should be on classpath.
 
 The executable jar can be exported using maven or eclipse.
 
The following is an example of running the command on windows
c:\devenv\izms\tmp>java -cp pdfqurangenerator_lib/*;.;pdfqurangenerator.jar com.izms.pdfqurangenerator.QuranPDFGenerator

The content of the current directory are given below. 
c:\devenv\izms\tmp>dir
 Volume in drive C has no label.
 Volume Serial Number is 80D1-B11B

 Directory of c:\devenv\izms\tmp

07/30/2021  12:25 PM    <DIR>          .
07/30/2021  12:25 PM    <DIR>          ..
07/30/2021  12:25 PM    <DIR>          arabic (This contains the arabic text of the Holy Quran)
07/30/2021  12:25 PM    <DIR>          font (This contains the font file)
07/30/2021  12:25 PM         2,041,476 HolyQuran.pdf (This is the generated file after running the program)
07/30/2021  12:22 PM        25,763,315 pdfqurangenerator.jar (this is the exported executable jar file)
07/30/2021  12:22 PM    <DIR>          pdfqurangenerator_lib (this is the jar libraries)
07/30/2021  12:24 PM    <DIR>          quranmetadata (this contains the quran metadata xml file)
07/30/2021  10:00 AM    <DIR>          translations (this contains the quran translations)


               