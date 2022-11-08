package model;

import org.apache.pdfbox.cos.COSArray;
import org.apache.pdfbox.cos.COSBase;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdfparser.PDFStreamParser;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;
import org.apache.pdfbox.util.Matrix;

import java.io.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class Stamp {
    //****************************************Coordenadas****************************************
    public static List<Object> getCoordinates(PDDocument document, String searchTerm, String startAt, String endAt, String exclude) throws IOException {
        List<Object> coordinates = new ArrayList<>();
        float participacoesY = 0;
        Integer participacoesPage = 0;
        for (Integer page = 1; page <= document.getNumberOfPages(); page++) {
            List<StampHelper> hits = findWords(document, page, startAt, exclude);
            if (hits.size() >= 1) {
                participacoesY = hits.get(0).getY();
                participacoesPage = page;
                break;
            }
        }
        Integer discussoesPage = 0;
        for (Integer page = 1; page <= document.getNumberOfPages(); page++) {
            List<StampHelper> hits = findWords(document, page, endAt, exclude);
            if (hits.size() >= 1) {
                discussoesPage = page;
                break;
            }
        }
            for (Integer page = participacoesPage; page <= discussoesPage; page++) {
                List<StampHelper> hits = new ArrayList<>();
                hits = findWords(document, page, searchTerm, exclude);
                if (page.equals(participacoesPage)) {
                    for (StampHelper hit : hits) {
                        if (hits.size() >= 1 && hit.getY() >= participacoesY) {
                            coordinates.add(0, page);
                            float average = (hit.textPositionAt(hit.length()-1).getY()+hit.textPositionAt(0).getY())/2.0f;
                            coordinates.add(1, average);
                            return coordinates;
                        }
                    }
                } else {
                    for (StampHelper hit : hits) {
                        if (hits.size() >= 1) {
                            coordinates.add(0, page);
                            float average = (hit.textPositionAt(hit.length()-1).getY()+hit.textPositionAt(0).getY())/2.0f;
                            coordinates.add(1, average);
                            return coordinates;
                        }
                    }
                }

            }



        return coordinates;
    }

    static List<StampHelper> findWords(PDDocument document, int page, String searchTerm, String exclude) throws IOException
    {
        final List<TextPosition> allTextPositions = new ArrayList<>();
        PDFTextStripper stripper = new PDFTextStripper()
        {
            @Override
            protected void writeString(String text, List<TextPosition> textPositions) throws IOException
            {
                if(!text.equals(exclude))
                allTextPositions.addAll(textPositions);
                System.out.println(text);
                super.writeString(text, textPositions);
            }

            @Override
            protected void writeLineSeparator() throws IOException {
                if (!allTextPositions.isEmpty()) {
                    TextPosition last = allTextPositions.get(allTextPositions.size() - 1);
                    if (!" ".equals(last.getUnicode())) {
                        Matrix textMatrix = last.getTextMatrix().clone();
                        textMatrix.setValue(2, 0, last.getEndX());
                        textMatrix.setValue(2, 1, last.getEndY());
                        TextPosition separatorSpace = new TextPosition(last.getRotation(), last.getPageWidth(), last.getPageHeight(),
                                textMatrix, last.getEndX(), last.getEndY(), last.getHeight(), 0, last.getWidthOfSpace(), " ",
                                new int[] {' '}, last.getFont(), last.getFontSize(), (int) last.getFontSizeInPt());
                        allTextPositions.add(separatorSpace);
                    }
                }
                super.writeLineSeparator();
            }
        };

        stripper.setSortByPosition(true);
        stripper.setStartPage(page);
        stripper.setEndPage(page);
        stripper.getText(document);

        final List<StampHelper> hits = new ArrayList<StampHelper>();
        StampHelper word = new StampHelper(allTextPositions);
        String string = word.toString();

        int fromIndex = 0;
        int index;
        while ((index = string.indexOf(searchTerm, fromIndex)) > -1)
        {
            hits.add(word.subSequence(index, index + searchTerm.length()));
            fromIndex = index + 1;
        }

        return hits;
    }

    //***********************************************RemoverSelo*******************************************

    public static PDDocument stampRemover(String pdfPath) throws IOException {

        PDDocument doc = PDDocument.load(new File(pdfPath));
        doc.setAllSecurityToBeRemoved(true);

        PDDocumentCatalog catalog = doc.getDocumentCatalog();
        PDDocumentNameDictionary names = catalog.getNames();
        if (names != null) {
            names.setEmbeddedFiles(null);
            names.setJavascript(null);
        }

        for (int i = 0; i < doc.getNumberOfPages(); i++) {
            cleanPage(doc.getPage(i));
        }
//        List<Object> coordinates = getCoordinates(doc, fullName);
//        doc = stamping(doc, fullName, pngPath, (Integer) coordinates.get(0), (float) coordinates.get(1));
        COSDictionary dictionary = doc.getDocumentCatalog().getCOSObject();
        dictionary.setNeedToBeUpdated(true);
        dictionary = (COSDictionary) dictionary.getDictionaryObject(COSName.ACRO_FORM);
        dictionary.setNeedToBeUpdated(true);
        COSArray array = (COSArray) dictionary.getDictionaryObject(COSName.FIELDS);
        array.setNeedToBeUpdated(true);

        for (int i = 0; i < doc.getNumberOfPages(); i++) {
            PDPage page = doc.getPage(i);
            // for each changed page
            COSDictionary item = page.getCOSObject();

            while (item.containsKey(COSName.PARENT)) {
                COSBase parent = item.getDictionaryObject(COSName.PARENT);
                if (parent instanceof COSDictionary) {
                    item = (COSDictionary) parent;
                    item.setNeedToBeUpdated(true);
                }
            }
            page.getCOSObject().setNeedToBeUpdated(true);
        }
//        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
//        doc.saveIncremental(byteArrayOutputStream);
//        doc.close();
//        byte[] pdfbytes = byteArrayOutputStream.toByteArray();
//        return pdfbytes;
        return doc;
    }

    public static void cleanPage(PDPage page) throws IOException {
        page.setAnnotations(new ArrayList<>());
        PDFStreamParser parser = new PDFStreamParser(page);
        parser.parse();
        ArrayList<Object> tokens = new ArrayList<>(parser.getTokens());
        ArrayList<Integer> remove = new ArrayList<>();
        remove.sort((o, t1) -> t1 - o);
        for (Integer i : remove) {
            tokens.remove((int) i);
        }
    }

    //************************************************Colocar imagem****************************************************
    public static void stamping(PDDocument doc, String fullName, String pngPath, Integer pageNumber, float coordY) throws IOException {

        String[] names = fullName.split(" ");
        String fullNameLowerCase = "";
        for (int i = 0; i < names.length; i++) {
            fullNameLowerCase = fullNameLowerCase + " " + names[i].substring(0, 1).toUpperCase() + names[i].substring(1).toLowerCase();
        }
        //Retrieving the page
        PDPage page = doc.getPage(pageNumber - 1);

        //Creating PDImageXObject object
        PDImageXObject pdImage = PDImageXObject.createFromFile(pngPath, doc);

        //creating the PDPageContentStream object
        PDPageContentStream contents = new PDPageContentStream(doc, page, PDPageContentStream.AppendMode.APPEND, true, true);

        //Drawing the image in the PDF document
        contents.drawImage(pdImage, 350, page.getMediaBox().getHeight() - coordY - 16);

        contents.beginText();
        PDFont font = PDType1Font.TIMES_ITALIC;
        contents.setFont(font, 15);
        contents.newLineAtOffset(350 + 15, page.getMediaBox().getHeight() - coordY - 16 + 24);
        contents.showText(names[0].substring(0, 1).toUpperCase() + names[0].substring(1).toLowerCase());
        contents.endText();

        contents.beginText();
        contents.setFont(font, 15);
        contents.newLineAtOffset(350 + 15, page.getMediaBox().getHeight() - coordY - 16 + 7);
        contents.showText(names[names.length - 1].substring(0, 1).toUpperCase() + names[names.length - 1].substring(1).toLowerCase());
        contents.endText();

        PDFont pdfFont = PDType1Font.HELVETICA;
        float fontSize = 7.6f;
        float leading = 1.15f * fontSize;

        float width = 92;
        float startX = 474;
        float startY = page.getMediaBox().getHeight() - coordY - 16 + 30;
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        String text = "Digitalmente assinado por" + " " + fullNameLowerCase + " " + "às" + " " + LocalTime.now().format(timeFormatter) + " " + "em" + " " + LocalDate.now().format(dateFormatter);
        List<String> lines = new ArrayList<String>();
        int lastSpace = -1;
        while (text.length() > 0) {
            int spaceIndex = text.indexOf(' ', lastSpace + 1);
            if (spaceIndex < 0)
                spaceIndex = text.length();
            String subString = text.substring(0, spaceIndex);
            float size = fontSize * pdfFont.getStringWidth(subString) / 1000;
            if (size > width) {
                if (lastSpace < 0)
                    lastSpace = spaceIndex;
                subString = text.substring(0, lastSpace);
                lines.add(subString);
                text = text.substring(lastSpace).trim();
                lastSpace = -1;
            } else if (spaceIndex == text.length()) {
                lines.add(text);
                text = "";
            } else {
                lastSpace = spaceIndex;
            }
        }
        if (lines.size() == 3) {
            startY = startY - 3;
            leading = 1.35f * fontSize;
        } else if (lines.size() == 5) {
            leading = 1f * fontSize;
            startY = startY + 2;
        }
        contents.beginText();
        contents.setFont(pdfFont, fontSize);
        contents.newLineAtOffset(startX, startY);
        for (String line : lines) {
            contents.showText(line);
            contents.newLineAtOffset(0, -leading);
        }
        contents.endText();

        contents.close();

        OutputStream outPath = new FileOutputStream(new File("src/resource/ataSelada.pdf"));
        doc.saveIncremental(outPath);
        doc.close();
    }
}

