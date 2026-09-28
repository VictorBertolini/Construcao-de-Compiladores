package com.compiladores;

import com.compiladores.entity.Scanner;
import com.compiladores.entity.Token;

public class Main {


    public static void main(String[] args) {
        String filepath = "code/main.txt";
        String codigo = lerArquivo(filepath);


        System.out.println("Código-fonte:\n" + codigo + "\n");
        System.out.println("Tokens reconhecidos:");

        Scanner scanner = new Scanner(codigo);
        Token token;
        while ((token = scanner.nextToken()) != null) {
            System.out.println("  " + token);
        }

        System.out.println("\nErros léxicos encontrados: " + scanner.getErrors().size());
        for (String erro : scanner.getErrors()) {
            System.out.println("  " + erro);
        }
    }

    private static String lerArquivo(String caminho) {
        try {
            return new String(java.nio.file.Files.readAllBytes(java.nio.file.Paths.get(caminho)));
        } catch (java.io.IOException e) {
            System.err.println("Não foi possível ler o arquivo: " + caminho);
            System.exit(1);
            return null;
        }
    }
}