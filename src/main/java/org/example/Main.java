package org.example;
import model.Stamp;
import org.apache.pdfbox.pdmodel.PDDocument;

import java.io.IOException;
import java.util.List;


public class Main {
    public static void main(String[] args) throws IOException {
        String fullName = "PRISCILA DE FATIMA FERNANDES DANTAS";
        String startLimit = "4. Participações";
        String finalLimit = "5. Itens de discussão";
        String exclude = "SRF";
        PDDocument doc = Stamp.stampRemover("src/resource/ataSelada.pdf");
        List<Object> coordinates = Stamp.getCoordinates(doc,fullName,startLimit,finalLimit, exclude);
        Stamp.stamping(doc, fullName,"src/resource/selo.png",(Integer) coordinates.get(0), (float) coordinates.get(1));

    }
}