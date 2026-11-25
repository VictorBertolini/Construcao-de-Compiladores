# Checkpoint 5 — Verificador de Tipos

Grupo: Sthephanny Caroline e Victor Bertolini.
As regras e as decisões estão no papel (`tipos-a-mao.pdf`); este README diz onde
cada uma está no código.

## Como executar

```bash
mvn test                      # JUnit: CP2, CP3, CP4 e CP5
```

Sem Maven (Java 17+), a partir da pasta do projeto:

```bash
mkdir -p out
javac -encoding UTF-8 -d out $(find src/main/java -name '*.java') \
      src/test/java/Cp3SmokeTest.java src/test/java/Cp4SmokeTest.java src/test/java/Cp5SmokeTest.java
java -ea -cp out Cp5SmokeTest     # OK: 114 verificações CP5.
java -ea -cp out Cp4SmokeTest     # regressão CP4
java -ea -cp out Cp3SmokeTest     # regressão CP3
java -cp out com.compiladores.Main   # analisa code/main.txt
```

## Pipeline

`Parser` → (AST válida) → `IdentificationVisitor` (CP4) → `TypeChecker` (CP5).
`SemanticAnalyzer` roda as duas passadas, junta os erros e diz se pode seguir
para a geração de código (`canGenerateCode()`): **com qualquer erro, não segue**.
Avisos não impedem a geração.

## Como os tipos estão representados

| Onde | O quê |
|---|---|
| `types/Tipo.java` | `enum` com `int`, `long`, `float`, `double`, `boolean`, `string`, `void`, além de `ERROR` (tipo de erro) e `ANY` (só no parâmetro de `print`). Os numéricos têm um posto: int < long < float < double. |
| `Ast.Expr.inferredType` | Tipo calculado de cada expressão. É **a única mudança na hierarquia** de nós. |
| `Symbol.tipo` | Tipo da variável/parâmetro, ou tipo de retorno da rotina (`void` em procedimento). |
| `Symbol.paramTypes` | Assinatura: tipos dos parâmetros (`null` em variáveis). Preenchido pelo CP4. |

O CP4 passou a declarar no nível 0, antes do programa, as rotinas predefinidas
`print(qualquer)` (procedimento) e `read()` (função que devolve `string`).
Elas aparecem nas resoluções com linha 0.

## Onde fica cada regra

| Regra do papel | Código |
|---|---|
| [LIT-INT], [LIT-REAL], [LIT-STR], [LIT-BOOL] | `TypeChecker.visitIntLiteral` … `visitBoolLiteral` |
| [ID] | `TypeChecker.visitId` |
| [ARIT], [CONCAT], [MOD], [REL], [IGUAL], [LOG] | `TypeRules.binary` (um `case` por grupo), chamado por `visitBin` |
| [NOT], [NEG] | `TypeRules.unary`, chamado por `visitUnary` |
| [ALARGA] e [ATRIB] | `TypeRules.assignable`; usados em `visitAssign`, `visitVarDecl`, `visitReturn` e nos argumentos |
| [DECL] | `TypeChecker.visitVarDecl` |
| [IF], [WHILE], [FOR] | `TypeChecker.condition`, chamado por `visitBranch`, `visitWhile` e `visitFor` |
| [RET], [RET-VOID] | `TypeChecker.visitReturn` (rotina corrente guardada em `routineName`/`routineReturn`) |
| [RET-CAMINHOS] | `ReturnAnalyzer` (visitor próprio), chamado em `TypeChecker.routine` |
| [CHAMADA], [CHAMADA-PROC], [CHAMADA-CMD] | `TypeChecker.call` (com `asStatement`) |
| Sem cascata | `ERROR` é aceito por `assignable`/`binary`/`unary` sem nova mensagem |
| Variável sem valor (aviso) | conjunto `assigned` no `TypeChecker` (`visitIf`, `visitWhile`, `visitFor`, `visitId`) |

## Onde fica cada decisão

| # | Decisão | Código | Teste |
|---|---|---|---|
| 1 | Alargamento int→long→float→double; o inverso é erro | `TypeRules.assignable` | `d01_alargamento` |
| 2 | Não há `char` na linguagem | `TypeChecker.visitCharLiteral` (e o parser recusa `char`) | `d02_sem_char` |
| 3 | `boolean` não entra em aritmética; condição só aceita `boolean` | `TypeRules.binary`/`unary`, `TypeChecker.condition` | `d03_bool_aritmetica`, `cond_int` |
| 4 | `string`: `equal` e `+` (string+string); sem `<` | `TypeRules.binary` | `d04_string` |
| 5 | `equal`: numéricos entre si; os demais tipos só com o mesmo tipo | `TypeRules.binary` (`case "equal"`) | `d05_equal_tipos` |
| 6 | Função como comando: valor descartado | `TypeChecker.visitCallStmt` | `d06_funcao_comando` |
| 7 | Sem recursão mútua (decisão do CP4) | `IdentificationVisitor` (declara antes de usar) | `d07_recursao_mutua` |
| 8 | Argumento aceita o mesmo alargamento da atribuição | `TypeRules.acceptsArgument` | `d08_argumentos` |
| 9 | Análise estrutural do `return` | `ReturnAnalyzer` | `d09_return_caminhos` |
| 10 | Sem sobrecarga (redeclaração, CP4) | `IdentificationVisitor.declare` | `d10_sobrecarga` |
| 11 | Nome de rotina sem parênteses é erro | `TypeChecker.visitId`, `visitAssign` | `d11_sem_parenteses` |

**Representação na TAM (só registrada, sem implementar):** `int` e `boolean` ocupam
1 palavra. `long`, `float` e `double` também ficam em 1 palavra cada; a conversão
implícita vira uma instrução de conversão antes do uso. `string` é um endereço
(1 palavra) que aponta para os caracteres no heap.

## Formato das mensagens

Saída de `SemanticAnalyzer.report()`, a mesma de `test/esperado/`:

```
ERROS
linha L: <mensagem>
AVISOS
linha L: aviso: <mensagem>
```

Uma lista vazia aparece como `(nenhum)`. Os erros do CP4 e do CP5 vêm juntos,
ordenados por linha; na mesma linha, valem a ordem em que foram encontrados.
Se o parser rejeita o programa, a saída é `REJEITADO NA ANÁLISE SINTÁTICA`
seguida dos erros do parser.

| Situação | Mensagem |
|---|---|
| Operador binário | `operador '+' não aceita int e string` |
| Operador unário | `operador 'not' não aceita int` |
| Condição | `condição de 'if' deve ser boolean, mas é int` (`if`, `elif`, `while`, `for`) |
| Atribuição | `atribuição a 'x': esperado int, veio double` |
| Inicialização | `inicialização de 'x': esperado long, veio float` |
| Return com tipo errado | `'return' em 'f': esperado int, veio string` |
| Return sem valor | `'return' sem valor na função 'g', que retorna double` |
| Return em procedimento | `'return' com valor no procedimento 'p'` |
| Caminho sem return | `função 'f' pode terminar sem 'return'` (linha da função) |
| Aridade | `'fat' espera 1 argumento(s), recebeu 2` |
| Argumento | `argumento 1 de 'mostra': esperado string, veio int` |
| Não é rotina | `'b' não é função nem procedimento` |
| Procedimento em expressão | `procedimento 'p' usado como expressão` |
| Rotina sem parênteses | `'um' é função e foi usado sem parênteses` |
| Atribuir a rotina | `'um' é função e não pode receber atribuição` |
| Aviso | `aviso: 'y' pode ser usada sem valor` (uma vez por variável) |

## Variáveis sem valor (análise conservadora)

Uma variável só "tem valor" se foi inicializada ou recebeu atribuição em
**todos** os caminhos: depois de um `if`, só conta o que foi atribuído em todos os
ramos (e só se houver `else`); o corpo de `while`/`for` pode não executar, então não
conta. Parâmetros sempre têm valor. Globais só contam se tiverem inicializador. O
aviso sai no primeiro uso suspeito de cada variável. Isso também cobre
`int k = k;`: o CP4 resolve o `k` da direita para a própria declaração, e o CP5
avisa que ele ainda não tem valor.

## Testes

- `test/programas/*.txt` + `test/esperado/*.txt`: programas obrigatórios
  (`tipos_valido_completo`, `tipos_erro_expressao`, `cond_int`, `return_incompativel`,
  `chamada_errada`, `procedimento_expressao`, `varios_erros`), funções
  (`recursiva`), aviso (`sem_valor`) e um por decisão (`d01`…`d11`).
- `Cp5SmokeTest.testRules`: cada linha da tabela de operadores, aceita e recusada,
  sem passar pelo parser.
- `Cp5SmokeTest.testDecorationAndNested`: tipo gravado nos nós e função
  **aninhada** usando variável do nível acima. Como o parser não aceita funções
  aninhadas, a AST é montada à mão.
- `TypeCheckerTest` e `RegressaoTest` (JUnit), além das suítes antigas do CP2, CP3 e CP4.
