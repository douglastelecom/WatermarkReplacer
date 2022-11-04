package org.example;

import model.RealocarSelo;
import org.apache.pdfbox.pdmodel.PDDocument;

import java.io.File;
import java.io.IOException;


public class Main {
    public static void main(String[] args) throws IOException {
        RealocarSelo.substituirSelo("/home/douglas/Documentos/ata.pdf", "/home/douglas/Imagens/selo5.png", "NOÉ");
    }
}