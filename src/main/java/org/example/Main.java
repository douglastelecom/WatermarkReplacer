package org.example;
import model.RealocarSelo;
import java.io.IOException;


public class Main {
    public static void main(String[] args) throws IOException {
        RealocarSelo.substituirSelo("/home/douglas/Documentos/ata3.pdf", "/home/douglas/Imagens/selo.png", "DOUGLAS FERNANDES DOS SANTOS");
    }
}