package org.example;
import model.RealocarSelo;
import java.io.IOException;


public class Main {
    public static void main(String[] args) throws IOException {
        RealocarSelo.substituirSelo("src/resource/ataSelada.pdf", "src/resource/selo.png", "DOUGLAS FERNANDES BLA BLA");
    }
}