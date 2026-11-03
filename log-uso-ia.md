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

## Checkpoint 3 — registro específico (03/11/2026)

Foi utilizado **ChatGPT (OpenAI)** para desenvolver a primeira versão desta
entrega a partir do repositório e do enunciado fornecidos. A assistência
incluiu: desenho da hierarquia de AST; implementação das classes `Ast` e
`Visitor<T>`; visitors de impressão e reimpressão; refatoração de `Parser`
para `ParseResult`, construção de AST e recuperação; testes de precedência,
associatividade, parênteses, ida e volta e regressão do CP2; revisão de `Main`
e documentação `ast-design.md`.

As árvores esperadas dos arquivos `test/esperado` foram redigidas como
expectativas explícitas a partir das construções da linguagem, antes das
comparações, e foram corrigidas pontualmente em detalhes de indentação após
as verificações. **Não** se usou uma cópia automática da saída do impressor
para gerar esses arquivos.

A compilação e a suíte independente foram executadas no ambiente de
desenvolvimento. A revisão final pelos integrantes do grupo e a execução
em máquinas da disciplina ficam sob responsabilidade do grupo antes da entrega.
