package org.example;

import model.RealocarSelo;
import org.apache.pdfbox.pdmodel.PDDocument;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class Main {
    public static void main(String[] args) throws IOException {
        String fileName = "/home/residencia/Documentos/ata.pdf";
        PDDocument document = PDDocument.load(new File(fileName));
        List<Object> coordinates = RealocarSelo.getCoordinates(document, "NOÉ");
        System.out.println("Na página " + coordinates.get(0) + " e coordenada y:" + coordinates.get(1));
        RealocarSelo.removerSelo("/home/residencia/Documentos/ata.pdf");
        RealocarSelo.colocarImagem("/home/residencia/Documentos/ata.pdf", "/home/residencia/Imagens/point.png",(Integer) coordinates.get(0) - 1, (float) coordinates.get(1));
    }
}