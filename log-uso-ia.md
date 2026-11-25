# Declaração de uso de Inteligência Artificial

Declaramos que foram utilizadas ferramentas de Inteligência Artificial na realização deste trabalho nos seguintes âmbitos:

- Código Java: auxílio na correção de erros e na identificação de problemas relacionados à implementação do Scanner 
  e do Parser.
- Especificação léxica: auxílio na formalização da escrita e na correção ortográfica do arquivo especificacao_lexica.
  md e gramatica-first-follow.md.
- Diagramas: auxílio na construção dos diagramas utilizando a linguagem Mermaid, incluindo a resolução de problemas 
  de sobreposição de elementos e limitações da própria linguagem.
- Classe Main: auxílio na implementação da leitura do arquivo de testes e na organização da impressão dos resultados 
  no terminal.
- Conceitos teóricos: auxílio na compreensão e esclarecimento de conceitos teóricos relacionados à disciplina.
- Conferir se os desenhos feitos estavam corretos e uso do Claude-Code para conferir se o código, seus testes e 
  outras partes do código implementadas pelo grupo estavam corretas.

## Checkpoint 5 — Verificador de Tipos

Neste checkpoint o uso de IA (Claude) foi **além** do permitido pela Seção 7 do enunciado. Registramos tudo:

- **Decisões de projeto:** a IA listou as alternativas de cada uma das 11 decisões e recomendou uma opção; o grupo escolheu entre elas (aceitou as recomendadas). O texto das decisões foi redigido pela IA a partir dessas escolhas.
- **Regras de tipagem e tabela de operadores:** redigidas pela IA em texto; o grupo as copiou à mão para o `tipos-a-mao.pdf`. As regras não foram escritas pelo grupo antes do código.
- **Código:** a IA escreveu `Tipo`, `TypeRules`, `TypeChecker`, `ReturnAnalyzer`, `SemanticAnalyzer`, o campo `inferredType` em `Expr`, os campos `tipo`/`paramTypes` em `Symbol`, as rotinas predefinidas `print`/`read` no `IdentificationVisitor` e a atualização do `Main`.
- **Testes:** a IA escreveu os programas de `test/programas/` do CP5 e as saídas de `test/esperado/` do CP5, derivadas das regras **antes** de rodar o verificador (não copiadas da saída dele), além de `Cp5SmokeTest`, `TypeCheckerTest` e `RegressaoTest`.
- **Documentação:** a IA escreveu o `README.md` do CP5.
- Motivo: falta de tempo para a entrega. O grupo se compromete a estudar as regras e o código gerado.
