package org.example;
import model.Stamp;
import org.apache.pdfbox.pdmodel.PDDocument;

import java.io.IOException;
import java.util.List;


public class Main {
    public static void main(String[] args) throws IOException {
        String fullName = "DOUGLAS FERNANDES DOS SANTOS";
        PDDocument doc = Stamp.stampRemover("src/resource/ata.pdf");
        List<Object> coordinates = Stamp.getCoordinates(doc,fullName,"4. Participações","5. Itens de discussão");
        System.out.println((Integer) coordinates.get(0));
        Stamp.stamping(doc, fullName,"src/resource/selo.png",(Integer) coordinates.get(0), (float) coordinates.get(1));

    }
}