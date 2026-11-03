# Checkpoint 3 — Como executar

Projeto em Java 17+; scanner **inalterado** em relação ao CP1 e gramática do
CP2 preservada. Não há necessidade de baixar outro parser nem de alterar
`gramatica-first-follow.md`.

## Estrutura

- `ast-design.md`: hierarquia, mapeamento completo da gramática, AST desenhada
  em texto, decisões sobre posição, impressor e reimpressor.
- `log-uso-ia.md`: histórico mais registro específico do CP3.
- `src/main/java/com/compiladores/entity/Scanner.java`: scanner original.
- `src/main/java/com/compiladores/entity/Parser.java`: parser LL(1), recuperação
  por modo pânico e `ParseResult` com árvore nula em caso de erro.
- `src/main/java/com/compiladores/ast`: hierarquia de nós e `Visitor<T>`.
- `src/main/java/com/compiladores/visitor`: `TreePrinter` e `SourcePrinter`.
- `test/programas`: três exemplos válidos.
- `test/esperado`: seis árvores esperadas, escritas explicitamente.
- `src/test/java`: testes do CP1/CP2 e testes do CP3.

## Compilar e testar pelo Maven

Na pasta onde está `pom.xml`:

```bash
mvn test
```

O JUnit executa `ScannerTest`, `ParserTest` e `AstTest`. Este último aciona
`Cp3SmokeTest`, que compara árvores esperadas, testa os seis round-trips,
parênteses redundantes e várias entradas inválidas.

## Rodar sem Maven (Java 17+ com JDK)

```bash
mkdir -p out
javac -encoding UTF-8 -d out $(find src/main/java -name '*.java') src/test/java/Cp3SmokeTest.java
java -ea -cp out Cp3SmokeTest
```

Saída esperada: `OK: 67 verificações CP3 e regressão CP2.` (o scanner do CP1
também pode imprimir uma mensagem referente ao caractere inválido `@` de um
teste; isso é esperado).

Para testar o arquivo de exemplo `code/main.txt` e ver a AST:

```bash
java -cp out com.compiladores.Main
```

## API do parser

```java
Parser.ParseResult resultado = new Parser(codigo).parse();
if (resultado.accepted()) {
    String arvore = new TreePrinter().print(resultado.ast);
    String fonte = new SourcePrinter().print(resultado.ast);
} else {
    System.out.println(resultado.errors); // resultado.ast == null
}
```

Os consumidores antigos de veredito booleano podem usar `parseAceito()`.
O retorno de `parse()` foi intencionalmente alterado, como exige o CP3.
