package com.compiladores;

import com.compiladores.entity.Parser;
import com.compiladores.entity.Scanner;
import com.compiladores.entity.Token;
import com.compiladores.visitor.TreePrinter;
import com.compiladores.visitor.SourcePrinter;
import com.compiladores.visitor.IdentificationVisitor;
import com.compiladores.visitor.SemanticAnalyzer;

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

        Parser parser = new Parser(codigo);
        Parser.ParseResult resultado = parser.parse();
        System.out.println("\nAceito: " + resultado.accepted());
        for (String erro : resultado.errors) {
            System.out.println(erro);
        }
        if (resultado.accepted()) {
            System.out.println("\nAST:\n" + new TreePrinter().print(resultado.ast));
            System.out.println("\nCódigo reimpresso:\n" + new SourcePrinter().print(resultado.ast));
            IdentificationVisitor identification = new IdentificationVisitor();
            identification.analyze(resultado.ast);
            System.out.println("\nIdentificação (CP4):\n" + identification.report());
            SemanticAnalyzer semantic = new SemanticAnalyzer();
            semantic.analyze(resultado.ast);
            System.out.println("\nAnálise contextual (CP4 + CP5):\n" + semantic.report());
            System.out.println(semantic.canGenerateCode()
                    ? "Sem erros: pode seguir para a geração de código."
                    : "Há erros: a geração de código não será executada.");
        }
    }

    private static String lerArquivo(String caminho) {
        try {
            return java.nio.file.Files.readString(java.nio.file.Paths.get(caminho), java.nio.charset.StandardCharsets.UTF_8);
        } catch (java.io.IOException e) {
            System.err.println("Não foi possível ler o arquivo: " + caminho);
            System.exit(1);
            return null;
        }
    }
}
