package org.example;
import model.RealocarSelo;
import java.io.IOException;


public class Main {
    public static void main(String[] args) throws IOException {
        RealocarSelo.substituirSelo("/home/residencia/Documentos/ata.pdf", "/home/residencia/Imagens/selo.png", "GELLY SABRINA HONORIO DE MELO REGES");
    }
}