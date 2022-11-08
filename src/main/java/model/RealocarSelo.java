package model;

import org.apache.pdfbox.cos.COSArray;
import org.apache.pdfbox.cos.COSBase;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdfparser.PDFStreamParser;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;

import java.io.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class RealocarSelo {
    //****************************************Coordenadas****************************************
    public static List<Object> getCoordinates(PDDocument document, String searchTerm) throws IOException {
        List<Object> coordinates = new ArrayList<>();
        float participacoesY = 0;
        Integer participacoesPage = 0;
        for (Integer page = 1; page <= document.getNumberOfPages(); page++) {
            List<TextPositionSequence> hits = findWords(document, page, "4. Participações");
            if (hits.size() >= 1) {
                participacoesY = hits.get(0).getY();
                participacoesPage = page;
                break;
            }
        }
        Integer discussoesPage = 0;
        for (Integer page = 1; page <= document.getNumberOfPages(); page++) {
            List<TextPositionSequence> hits = findWords(document, page, "5. Itens de discussão");
            if (hits.size() >= 1) {
                discussoesPage = page;
                break;
            }
        }
        List<TextPositionSequence> hits = new ArrayList<>();
        String searchTermSplitted = searchTerm;
        while (hits.size() <= 0) {
            for (Integer page = participacoesPage; page <= discussoesPage; page++) {
                hits = findWords(document, page, searchTermSplitted);
                if (page.equals(participacoesPage)) {
                    for (TextPositionSequence hit : hits) {
                        if (hits.size() >= 1 && hit.getY() >= participacoesY) {
                            coordinates.add(0, page);
                            coordinates.add(1, hit.getY());
                            return coordinates;
                        }
                    }
                } else {
                    for (TextPositionSequence hit : hits) {
                        if (hits.size() >= 1) {
                            coordinates.add(0, page);
                            coordinates.add(1, hit.getY());
                            return coordinates;
                        }
                    }
                }

                String searchTermSplitted2 = "";
                String[] searchTermList = searchTermSplitted.split(" ");
                for (int i = 0; i < searchTermList.length - 1; i++) {
                    if (i == 0) {
                        searchTermSplitted2 = searchTermSplitted2 + searchTermList[i];
                    } else {
                        searchTermSplitted2 = searchTermSplitted2 + " " + searchTermList[i];
                    }
                }
                searchTermSplitted = searchTermSplitted2;

            }

        }

        return coordinates;
    }

    static List<TextPositionSequence> findWords(PDDocument document, int page, String searchTerm) throws IOException {
        final List<TextPositionSequence> hits = new ArrayList<TextPositionSequence>();
        PDFTextStripper stripper = new PDFTextStripper() {
            @Override
            protected void writeString(String text, List<TextPosition> textPositions) throws IOException {
                TextPositionSequence word = new TextPositionSequence(textPositions);
                String string = word.toString();

                int fromIndex = 0;
                int index;
                while ((index = string.indexOf(searchTerm, fromIndex)) > -1) {
                    hits.add(word.subSequence(index, index + searchTerm.length()));
                    fromIndex = index + 1;
                }
                super.writeString(text, textPositions);
            }
        };

        stripper.setSortByPosition(true);
        stripper.setStartPage(page);
        stripper.setEndPage(page);
        stripper.getText(document);
        return hits;
    }

    //***********************************************RemoverSelo*******************************************

    public static void substituirSelo(String pdfPath, String pngPath, String fullName) throws IOException {

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
        List<Object> coordinates = getCoordinates(doc, fullName);
        doc = colocarImagem(doc, fullName, pngPath, (Integer) coordinates.get(0), (float) coordinates.get(1));
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
        OutputStream outPath = new FileOutputStream(new File("src/resource/ataSelada.pdf"));
        doc.saveIncremental(outPath);
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
    public static PDDocument colocarImagem(PDDocument doc, String fullName, String pngPath, Integer pageNumber, float coordY) throws IOException {

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

        return doc;
    }
}

