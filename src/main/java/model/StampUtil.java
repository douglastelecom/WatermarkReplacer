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
import org.apache.pdfbox.pdmodel.graphics.form.PDFormXObject;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotation;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationRubberStamp;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAppearanceDictionary;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAppearanceStream;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;
import org.apache.pdfbox.util.Matrix;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class StampUtil {
    //****************************************Coordenadas****************************************
    private static final String SAVE_GRAPHICS_STATE = "q\n";
    private static final String RESTORE_GRAPHICS_STATE = "Q\n";
    private static final String CONCATENATE_MATRIX = "cm\n";
    private static final String XOBJECT_DO = "Do\n";
    private static final String SPACE = " ";
    private static final NumberFormat FORMATDECIMAL = NumberFormat.getNumberInstance( Locale.US );

    public static List<Object> getCoordinates(PDDocument document, String searchTerm, String startAt, String endAt, String exclude) throws IOException {
        List<Object> coordinates = new ArrayList<>();
        float participacoesY = 0;
        Integer participacoesPage = 0;
        for (Integer page = 1; page <= document.getNumberOfPages(); page++) {
            List<TextPositionSequence> hits = findWords(document, page, startAt, exclude);
            if (hits.size() >= 1) {
                participacoesY = hits.get(0).getY();
                participacoesPage = page;
                break;
            }
        }
        Integer discussoesPage = 0;
        for (Integer page = 1; page <= document.getNumberOfPages(); page++) {
            List<TextPositionSequence> hits = findWords(document, page, endAt, exclude);
            if (hits.size() >= 1) {
                discussoesPage = page;
                break;
            }
        }
        for (Integer page = participacoesPage; page <= discussoesPage; page++) {
            List<TextPositionSequence> hits = new ArrayList<>();
            hits = findWords(document, page, searchTerm, exclude);
            if (page.equals(participacoesPage)) {
                for (TextPositionSequence hit : hits) {
                    if (hits.size() >= 1 && hit.getY() >= participacoesY) {
                        coordinates.add(0, page);
                        float average = (hit.textPositionAt(hit.length() - 1).getY() + hit.textPositionAt(0).getY()) / 2.0f;
                        coordinates.add(1, average);
                        return coordinates;
                    }
                }
            } else {
                for (TextPositionSequence hit : hits) {
                    if (hits.size() >= 1) {
                        coordinates.add(0, page);
                        float average = (hit.textPositionAt(hit.length() - 1).getY() + hit.textPositionAt(0).getY()) / 2.0f;
                        coordinates.add(1, average);
                        return coordinates;
                    }
                }
            }

        }


        return coordinates;
    }

    static List<TextPositionSequence> findWords(PDDocument document, int page, String searchTerm, String exclude) throws IOException {
        final List<TextPosition> allTextPositions = new ArrayList<>();
        PDFTextStripper stripper = new PDFTextStripper() {
            @Override
            protected void writeString(String text, List<TextPosition> textPositions) throws IOException {
                if (!text.equals(exclude))
                    allTextPositions.addAll(textPositions);
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
                                new int[]{' '}, last.getFont(), last.getFontSize(), (int) last.getFontSizeInPt());
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

        final List<TextPositionSequence> hits = new ArrayList<TextPositionSequence>();
        TextPositionSequence word = new TextPositionSequence(allTextPositions);
        String string = word.toString();

        int fromIndex = 0;
        int index;
        while ((index = string.indexOf(searchTerm, fromIndex)) > -1) {
            hits.add(word.subSequence(index, index + searchTerm.length()));
            fromIndex = index + 1;
        }

        return hits;
    }

    //***********************************************RemoverSelo*******************************************

    public static PDDocument stampRemover(byte[] docByte) throws IOException {

        PDDocument doc = PDDocument.load(docByte);
        doc.setAllSecurityToBeRemoved(true);
        cleanLastAnnotation(doc.getPage(doc.getNumberOfPages() - 1));

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
        return doc;
    }

    public static void cleanLastAnnotation(PDPage page) throws IOException {
        List<PDAnnotation> annotations = page.getAnnotations();
        if (page.getAnnotations().size() > 0) {
            annotations.remove(page.getAnnotations().size() - 1);
            page.setAnnotations(annotations);
        }
    }

    //************************************************Colocar imagem****************************************************
    public static void stamping(PDDocument doc, String fullName, String pngPath, Integer pageNumber, float coordY ) throws IOException {

        String[] names = fullName.split(" ");
        String fullNameLowerCase = "";
        for (int i = 0; i < names.length; i++) {
            fullNameLowerCase = fullNameLowerCase + " " + names[i].substring(0, 1).toUpperCase() + names[i].substring(1).toLowerCase();
        }

            PDPage page = doc.getPage(pageNumber-1);
            page.getCOSObject().setNeedToBeUpdated(true);
            List<PDAnnotation> annotations = page.getAnnotations();
            PDAnnotationRubberStamp rubberStamp = new PDAnnotationRubberStamp();
            // create a PDXObjectImage with the given image file
            // if you already have the image in a BufferedImage,
            // call LosslessFactory.createFromImage() instead
            PDImageXObject ximage = PDImageXObject.createFromFile(pngPath, doc);
            // define and set the target rectangle
            float lowerLeftX = 350;
            float lowerLeftY = page.getMediaBox().getHeight() - coordY-16;
            float formWidth = 215;
            float formHeight = 39;
            float imgWidth = 215;
            float imgHeight = 39;

            PDRectangle rect = new PDRectangle();
            rect.setLowerLeftX(lowerLeftX);
            rect.setLowerLeftY(lowerLeftY);
            rect.setUpperRightX(lowerLeftX + formWidth);
            rect.setUpperRightY(lowerLeftY + formHeight);
            // Create a PDFormXObject
            PDFormXObject form = new PDFormXObject(doc);
            form.setResources(new PDResources());
            form.setBBox(rect);
            form.setFormType(1);
            // adjust the image to the target rectangle and add it to the stream
            try (OutputStream os = form.getStream().createOutputStream())
            {
                drawXObject(ximage, form.getResources(), os, lowerLeftX, lowerLeftY, imgWidth, imgHeight);
            }
            PDAppearanceStream myDic = new PDAppearanceStream(form.getCOSObject());
            PDAppearanceDictionary appearance = new PDAppearanceDictionary(new COSDictionary());
            appearance.setNormalAppearance(myDic);
            rubberStamp.setAppearance(appearance);
            rubberStamp.setRectangle(rect);
            // add the new RubberStamp to the document
            annotations.add(rubberStamp);


        OutputStream output = new FileOutputStream(new File("src/resource/ata55.pdf"));
        doc.saveIncremental(output);
        doc.close();
//
//        ByteArrayOutputStream docBytes = new ByteArrayOutputStream();
//        doc.saveIncremental(docBytes);
//        doc.close();
//        return docBytes.toByteArray();


//
//
//        //Retrieving the page
//        PDPage page = doc.getPage(pageNumber - 1);
//
//        //Creating PDImageXObject object
//        PDImageXObject pdImage = PDImageXObject.createFromFile(pngPath, doc);
//
//        //creating the PDPageContentStream object
//        PDPageContentStream contents = new PDPageContentStream(doc, page, PDPageContentStream.AppendMode.APPEND, true, true);
//
//        //Drawing the image in the PDF document
//        contents.drawImage(pdImage, 350, page.getMediaBox().getHeight() - coordY - 16);
//
//        contents.beginText();
//        PDFont font = PDType1Font.TIMES_ITALIC;
//        contents.setFont(font, 15);
//        contents.newLineAtOffset(350 + 15, page.getMediaBox().getHeight() - coordY - 16 + 24);
//        contents.showText(names[0].substring(0, 1).toUpperCase() + names[0].substring(1).toLowerCase());
//        contents.endText();
//
//        contents.beginText();
//        contents.setFont(font, 15);
//        contents.newLineAtOffset(350 + 15, page.getMediaBox().getHeight() - coordY - 16 + 7);
//        contents.showText(names[names.length - 1].substring(0, 1).toUpperCase() + names[names.length - 1].substring(1).toLowerCase());
//        contents.endText();
//
//        PDFont pdfFont = PDType1Font.HELVETICA;
//        float fontSize = 7.6f;
//        float leading = 1.15f * fontSize;
//
//        float width = 92;
//        float startX = 474;
//        float startY = page.getMediaBox().getHeight() - coordY - 16 + 30;
//        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
//        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
//        String text = "Digitalmente assinado por" + " " + fullNameLowerCase + " " + "às" + " " + LocalTime.now().format(timeFormatter) + " " + "em" + " " + LocalDate.now().format(dateFormatter);
//        List<String> lines = new ArrayList<String>();
//        int lastSpace = -1;
//        while (text.length() > 0) {
//            int spaceIndex = text.indexOf(' ', lastSpace + 1);
//            if (spaceIndex < 0)
//                spaceIndex = text.length();
//            String subString = text.substring(0, spaceIndex);
//            float size = fontSize * pdfFont.getStringWidth(subString) / 1000;
//            if (size > width) {
//                if (lastSpace < 0)
//                    lastSpace = spaceIndex;
//                subString = text.substring(0, lastSpace);
//                lines.add(subString);
//                text = text.substring(lastSpace).trim();
//                lastSpace = -1;
//            } else if (spaceIndex == text.length()) {
//                lines.add(text);
//                text = "";
//            } else {
//                lastSpace = spaceIndex;
//            }
//        }
//        if (lines.size() == 3) {
//            startY = startY - 3;
//            leading = 1.35f * fontSize;
//        } else if (lines.size() == 5) {
//            leading = 1f * fontSize;
//            startY = startY + 2;
//        }
//        contents.beginText();
//        contents.setFont(pdfFont, fontSize);
//        contents.newLineAtOffset(startX, startY);
//        for (String line : lines) {
//            contents.showText(line);
//            contents.newLineAtOffset(0, -leading);
//        }
//        contents.endText();
//        contents.close();
//        ByteArrayOutputStream docBytes = new ByteArrayOutputStream();
//        doc.saveIncremental(docBytes);
//        doc.close();
//        return docBytes.toByteArray();
    }
    private static void drawXObject( PDImageXObject xobject, PDResources resources, OutputStream os,
                              float x, float y, float width, float height ) throws IOException
    {
        // This is similar to PDPageContentStream.drawXObject()
        COSName xObjectId = resources.add(xobject);
        appendRawCommands( os, SAVE_GRAPHICS_STATE );
        appendRawCommands( os, FORMATDECIMAL.format( width ) );
        appendRawCommands( os, SPACE );
        appendRawCommands( os, FORMATDECIMAL.format( 0 ) );
        appendRawCommands( os, SPACE );
        appendRawCommands( os, FORMATDECIMAL.format( 0 ) );
        appendRawCommands( os, SPACE );
        appendRawCommands( os, FORMATDECIMAL.format( height ) );
        appendRawCommands( os, SPACE );
        appendRawCommands( os, FORMATDECIMAL.format( x ) );
        appendRawCommands( os, SPACE );
        appendRawCommands( os, FORMATDECIMAL.format( y ) );
        appendRawCommands( os, SPACE );
        appendRawCommands( os, CONCATENATE_MATRIX );
        appendRawCommands( os, SPACE );
        appendRawCommands( os, "/" );
        appendRawCommands( os, xObjectId.getName() );
        appendRawCommands( os, SPACE );
        appendRawCommands( os, XOBJECT_DO );
        appendRawCommands( os, SPACE );
        appendRawCommands( os, RESTORE_GRAPHICS_STATE );
    }

    private static void appendRawCommands(OutputStream os, String commands) throws IOException
    {
        os.write( commands.getBytes(StandardCharsets.ISO_8859_1));
    }
}

