package org.example;
import model.StampUtil;
import org.apache.pdfbox.pdmodel.PDDocument;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;


public class Main {
    public static void main(String[] args) throws IOException {
        String fullName = "GELLY SABRINA HONORIO DE MELO REGES";
        String startLimit = "4. Participações";
        String finalLimit = "5. Itens de discussão";
        String exclude = "SGB";
        byte[] pdfByte = Files.readAllBytes(new File("src/resource/ata.pdf").toPath());
        PDDocument doc = StampUtil.stampRemover(pdfByte);
        List<Object> coordinates = StampUtil.getCoordinates(doc,fullName,startLimit,finalLimit, exclude);
        StampUtil.stamping(doc, fullName,"src/resource/selo.png",(Integer) coordinates.get(0), (float) coordinates.get(1));
    }
}