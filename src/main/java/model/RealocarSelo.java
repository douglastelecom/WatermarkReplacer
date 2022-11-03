package model;

import org.apache.pdfbox.cos.COSArray;
import org.apache.pdfbox.cos.COSBase;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdfparser.PDFStreamParser;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;

import java.io.*;
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

        for (Integer page = participacoesPage; page <= document.getNumberOfPages(); page++) {
            List<TextPositionSequence> hits = findWords(document, page, searchTerm);
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

    public static byte[] removerSelo(String path) throws IOException {

        String name = new File(path).getName();
        name = name.substring(0, name.lastIndexOf("."));

        System.out.println("Title: " + name);

        PDDocument doc = PDDocument.load(new File(path));
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
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        doc.saveIncremental(byteArrayOutputStream);
        doc.close();
        byte[] pdfbytes = byteArrayOutputStream.toByteArray();
        return pdfbytes;
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
    public static void colocarImagem(String PDFPath, String PNGPath, Integer pageNumber, float y) throws IOException {
        //Loading an existing document
        File file = new File(PDFPath);
        PDDocument doc = PDDocument.load(file);

        //Retrieving the page
        PDPage page = doc.getPage(pageNumber);

        //Creating PDImageXObject object
        PDImageXObject pdImage = PDImageXObject.createFromFile(PNGPath,doc);

        //creating the PDPageContentStream object
        PDPageContentStream contents = new PDPageContentStream(doc, page, PDPageContentStream.AppendMode.APPEND, true, true);

        //Drawing the image in the PDF document
        contents.drawImage(pdImage, 300, y-60);

        System.out.println("Image inserted");

        //Closing the PDPageContentStream object
        contents.close();

        //Saving the document
        doc.save("/home/residencia/Documentos/ataPicture.pdf");

        //Closing the document
        doc.close();
    }
}
