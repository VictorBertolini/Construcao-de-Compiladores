# Checkpoint 3 — Projeto da AST (padrões Interpreter e Visitor)

**Grupo:** Sthephanny Caroline e Victor Bertolini  
**Linguagem:** a do CP1/CP2, conforme `gramatica-first-follow.md`  
**Data:** 03/11/2026

## 1. Critérios de projeto

A AST contém **o significado sintático**, não a sequência completa das produções.
O módulo `com.compiladores.ast` tem `Ast.Node` (classe abstrata), subclasses por
categoria significativa e `Visitor<T>`. Cada classe concreta implementa
`<T> T accept(Visitor<T> v)` delegando ao método `visitNomeDessaClasse`.
Os dois visitors concretos são `TreePrinter` e `SourcePrinter`, em
`com.compiladores.visitor`. Não há `instanceof` ou seleção por tipo de nó.

Todos os nós guardam `line` e `column`, herdados de `Node`. A posição corresponde
ao primeiro token significativo que originou o nó; em `Bin` é a posição do
operando esquerdo, e `Program` usa o primeiro token do arquivo (ou 1:1 se vazio).
Parênteses adicionais não substituem a posição do operando que envolvem.
Por enquanto os campos são finais; em uma etapa futura pode-se acrescentar
`symbol` em `Id` (e nas declarações) e `inferredType` em `Expr`, sem alterar a
estrutura de filhos, os visitors ou as produções. **Não há resolução de símbolos
nem inferência de tipos neste checkpoint.**

Os tipos declaráveis na gramática (`<tipo>`) são `int`, `long`, `float`,
`double`, `boolean` e `string`, além de `void` somente em retornos de
procedimentos. Para os literais, a AST cobre os **cinco tipos básicos** do
escopo do curso, com um nó para cada:

| Tipo básico | Nó da AST | Token do scanner CP1 |
|---|---|---|
| inteiro | `IntLiteral` | `INT` |
| real | `RealLiteral` | `REAL` |
| booleano | `BoolLiteral` | `true` / `false` |
| caractere | `CharLiteral` | — (ainda não existe) |
| string | `StringLiteral` | `STRING` |

`CharLiteral` já faz parte da hierarquia, do `Visitor<T>` e dos dois visitors,
mas **o parser ainda não o constrói**: o scanner do CP1 não tem token de
literal de caractere (aspas simples), e o enunciado proíbe modificá-lo (ver a
observação sobre `char` em `gramatica-first-follow.md`). Quando esse token
existir, basta acrescentar uma alternativa em `<primario>` que crie
`CharLiteral`; nenhuma outra classe de nó e nenhum visitor precisará mudar.

A distinção entre `int`/`long` e `float`/`double` não gera literais separados:
o scanner produz o mesmo token para ambos, e decidir o tipo exato pertence à
**análise semântica**, que ainda não existe. Não foram inventados tokens
`LIT_LONG`, `LIT_FLOAT` ou `LIT_DOUBLE`.

## 2. Hierarquia de nós

Os campos a seguir têm tipos Java; `?` indica campo que pode ser `null`.
`List` são listas imutáveis. Cada linha é um **tipo concreto**; classes-base
abstratas são `Node`, `Item`, `Stmt` e `Expr`.

| Nó | Campos específicos (além de `line:int`, `column:int`) | Origem na gramática CP2 |
|---|---|---|
| `Program` | `items:List<Item>` | `<programa>`, `<itens>` |
| `Type` | `name:String` | `<tipo>`, `<tipo_retorno>` |
| `Param` | `type:Type`, `name:String` | `<param>` |
| `FunctionDecl` | `returnType:Type`, `name:String`, `params:List<Param>`, `body:Block` | `<funcao>` com tipo não `void` |
| `ProcedureDecl` | `name:String`, `params:List<Param>`, `body:Block` | `<funcao>` com `void` |
| `VarDecl` | `type:Type`, `name:String`, `initial:Expr?` | `<decl_var>`, inicialização declarativa no `<init_for>` |
| `Block` | `statements:List<Node>` | `<bloco>`, `<comandos>` |
| `Assign` | `target:Id`, `value:Expr` | `<comando_id>` ou `<init_for>`/`<passo_for>` |
| `CallStmt` | `call:CallExpr` | `<comando_id>` com chamada |
| `Branch` | `condition:Expr`, `body:Block` | partes `if`/`elif` de `<condicional>`/`<cauda_cond>` |
| `If` | `branches:List<Branch>`, `elseBody:Block?` | `<condicional>`, `<cauda_cond>` |
| `While` | `condition:Expr`, `body:Block` | `<enquanto>` |
| `For` | `init:Node?`, `condition:Expr?`, `step:Assign?`, `body:Block` | `<para>`, `<init_for>`, `<cond_for>`, `<passo_for>` |
| `Return` | `value:Expr?` | `<retorno>`, `<retorno_opt>` |
| `Id` | `name:String` | `ID` em expressão ou alvo de atribuição |
| `IntLiteral` | `value:String` | `LIT_INT` |
| `RealLiteral` | `value:String` | `LIT_REAL` |
| `StringLiteral` | `value:String` (lexema com aspas e escapes) | `LIT_STRING` |
| `BoolLiteral` | `value:boolean` | `true`/`false` |
| `CharLiteral` | `value:String` (lexema com aspas simples) | nenhuma no CP2: reservado para um futuro `LIT_CHAR`, que o scanner do CP1 não emite |
| `Unary` | `op:String`, `operand:Expr` | `<expr_unaria>` com `not`, `-` ou `+` |
| `Bin` | `op:String`, `left:Expr`, `right:Expr` | operadores nas caudas de expressões |
| `CallExpr` | `name:String`, `args:List<Expr>` | `ID <sufixo_primario>` com `(...)` |

**Representações escolhidas:**

- `function void` vira `ProcedureDecl`; outros `function` viram `FunctionDecl`.
  Assim, `void` fica implícito no nó de procedimento e seu reimpressor o recompõe.
- `VarDecl` estende `Item`, mas pode aparecer no corpo de `Block`, cujo tipo
  `List<Node>` admite declarações e comandos. Uma atribuição em cabeçalho de `for`
  também reutiliza `Assign`. Não criamos variantes artificiais dos mesmos nós.
- Ausências legítimas (ε) usam campos nulos: valor de retorno, inicializador,
  partes do `for` ou corpo `else`. Listas vazias representam parâmetros,
  argumentos e comandos ausentes.
- `If.branches` guarda a primeira condição e todos os `elif` em ordem, e
  `elseBody` distingue o `else` opcional. Não criamos um `If` aninhado artificial.

## 3. Mapeamento de **todos** os não-terminais da BNF para a AST

Legenda: **N** = nó concreto; **A** = atributo/lista/elemento de outro nó;
**D** = descartado como categoria separada. Quando a categoria é descartada,
o conteúdo significativo ainda pode ser incorporado nos filhos.

| Não-terminal | Mapeamento e justificativa |
|---|---|
| `<programa>` | **N:** `Program` |
| `<itens>` | **A:** `Program.items`; lista recursiva não precisa ser nó |
| `<item>` | **D:** escolhe `FunctionDecl`, `ProcedureDecl` ou `VarDecl` |
| `<funcao>` | **N:** `FunctionDecl` ou `ProcedureDecl` pelo tipo de retorno |
| `<tipo_retorno>` | **N/A:** `Type` em função; `void` indicado por `ProcedureDecl` |
| `<tipo>` | **N:** `Type` |
| `<params_opt>` | **A:** `params`, vazia em ε |
| `<params>` | **A:** lista `params`; o agrupamento não é categoria semântica |
| `<params_rest>` | **A:** demais elementos da mesma lista; remove recursão de lista |
| `<param>` | **N:** `Param` |
| `<decl_var>` | **N:** `VarDecl` |
| `<inicializador>` | **A:** `VarDecl.initial`, ou `null` em ε; o `=` é pontuação de atribuição declarativa |
| `<bloco>` | **N:** `Block` |
| `<comandos>` | **A:** `Block.statements`; cada comando é um elemento |
| `<comando>` | **D:** apenas seleciona qual classe de comando construir |
| `<comando_id>` | **N:** `Assign` ou `CallStmt` |
| `<resto_comando_id>` | **D (fatoração):** após `ID`, `=` seleciona `Assign`, `(` seleciona `CallStmt`; não precisa de wrapper |
| `<condicional>` | **N:** `If` e primeira `Branch` |
| `<cauda_cond>` | **A:** demais `If.branches` e `If.elseBody`; `elif` não acrescenta nó de lista recursiva |
| `<enquanto>` | **N:** `While` |
| `<para>` | **N:** `For` |
| `<init_for>` | **A:** `For.init` (`VarDecl`, `Assign` ou `null`) |
| `<cond_for>` | **A:** `For.condition` (`Expr` ou `null`) |
| `<passo_for>` | **A:** `For.step` (`Assign` ou `null`) |
| `<retorno>` | **N:** `Return` |
| `<retorno_opt>` | **A:** `Return.value` (`Expr` ou `null`) |
| `<expr>` | **D (precedência):** devolve a mesma raiz `Expr` sem `Expr`-wrapper |
| `<cauda_or>` | **D (eliminação da recursão à esquerda):** faz `Bin(or)` com operando esquerdo acumulado |
| `<expr_and>` | **D (precedência):** devolve diretamente a subárvore correspondente |
| `<cauda_and>` | **D (eliminação da recursão à esquerda):** faz `Bin(and)` à esquerda |
| `<expr_igual>` | **D (precedência):** devolve subárvore; igualdade não permite encadeamento |
| `<cauda_igual>` | **D:** cria um `Bin(equal)` ou ε, sem nó auxiliar |
| `<expr_rel>` | **D (precedência):** devolve subárvore; relacional não permite encadeamento |
| `<cauda_rel>` | **D:** cria um `Bin(<, >, <=, >=)` ou ε |
| `<expr_soma>` | **D (precedência):** devolve a raiz das somas/subtrações |
| `<cauda_soma>` | **D (eliminação da recursão à esquerda):** dobra à esquerda criando `Bin(+/-)` |
| `<expr_prod>` | **D (precedência):** devolve a raiz dos produtos/divisões/módulos |
| `<cauda_prod>` | **D (eliminação da recursão à esquerda):** dobra à esquerda criando `Bin(*, /, %)` |
| `<expr_unaria>` | **N/D:** `Unary` quando tem operador; caso contrário, transmite `<primario>` |
| `<primario>` | **N/D:** produz `Id`, literal, `CallExpr`; em `(expr)` transmite a expressão sem nó de parênteses |
| `<sufixo_primario>` | **D (fatoração):** distingue `Id` e `CallExpr` após consumir `ID` |
| `<args_opt>` | **A:** `CallExpr.args` (lista vazia em ε) |
| `<args>` | **A:** elementos da lista `args` |
| `<args_rest>` | **A:** demais argumentos da lista; vírgula não vira nó |

Os três casos da Aula 7 estão nomeados explicitamente: **não-terminais de
precedência** (`<expr_soma>`, `<expr_prod>`, etc.), **caudas criadas para retirar
recursão à esquerda** (`<cauda_soma>`, `<cauda_prod>`, etc.) e **fatoração**
(`ID <resto_comando_id>`, `ID <sufixo_primario>`). `(`, `)`, `{`, `}`, `,`, `;`
e os próprios separadores da gramática **não geram nós**. Chaves são
representadas pelo nó `Block`, e os operadores são atributos de `Bin`/`Unary`.

## 4. Árvore desenhada em texto (exemplo com mais de 15 nós)

Programa curto na linguagem real do grupo:

```text
function int conta(int n) {
    int x = 1;
    if (n < 3) { x = x + n; }
    while (x < 10) { x = x + 1; }
    return x;
}
```

AST esquemática desenhada em texto, com **32 nós** (exclui tokens e auxiliares):

```text
Program
└── Function(conta)
    ├── Type(int)
    ├── Param(n)
    │   └── Type(int)
    └── Block
        ├── VarDecl(x)
        │   ├── Type(int)
        │   └── Int(1)
        ├── If
        │   └── Branch
        │       ├── Bin(<)
        │       │   ├── Id(n)
        │       │   └── Int(3)
        │       └── Block
        │           └── Assign
        │               ├── Id(x)
        │               └── Bin(+)
        │                   ├── Id(x)
        │                   └── Id(n)
        ├── While
        │   ├── Bin(<)
        │   │   ├── Id(x)
        │   │   └── Int(10)
        │   └── Block
        │       └── Assign
        │           ├── Id(x)
        │           └── Bin(+)
        │               ├── Id(x)
        │               └── Int(1)
        └── Return
            └── Id(x)
```

Exemplos de **derivação descartada**, ao lado do desenho:

1. `expr → expr_and → expr_igual → expr_rel → expr_soma → expr_prod → expr_unaria → primario → ID(n)` é um único `Id(n)` na AST.
2. `expr_soma → expr_prod cauda_soma`, depois `cauda_soma → "+" expr_prod cauda_soma`, resulta em um único `Bin(+)`, sem nó `cauda_soma`.
3. `comando_id → ID resto_comando_id ";"`, depois `resto_comando_id → "=" expr`, vira `Assign` com alvo `Id(x)`; nem `resto_comando_id` nem `;` são nós.
4. `primario → "(" expr ")"` produz o mesmo nó que `expr` sozinho; os parênteses são descartados.

## 5. Formato da impressão e reimpressão

`TreePrinter.print(root)` faz percurso **pré-ordem**, um nó por linha, e
indenta com **dois espaços por nível**. Os atributos relevantes aparecem no
rótulo (`Bin(+)`, `Function(soma)`, `String("x")`, `Char('x')`, `Type(int)`, etc.). Filhos
aparecem em ordem fixa. Campos opcionais vazios são indicados por
`Initializer(empty)`, `Value(empty)`, `Init(empty)`, `Condition(empty)`,
`Step(empty)`; a presença de `else` tem rótulo `Else`. A posição de origem
não é impressa para permitir comparar ASTs antes e depois da reimpressão,
pois os números de linha podem mudar. Esse formato é determinístico e
distingue operações, identidades, valores, presença de filhos e ordem.

`SourcePrinter.print(program)` gera código concreto canônico:

- sempre escreve `function`, tipos, parâmetros e chamadas na ordem original;
- coloca parênteses em **todas as operações binárias e unárias**, preservando
  precedência e associação mesmo em casos como `a - (b - c)`;
- emite os pontos e vírgulas necessários (exceto após bloco) e os elementos
  do `for` com exatamente dois `;` internos;
- mantém `if`/`elif`/`else`, `while`, `return` e literais de string intactos;
- normaliza espaços e indentação com quatro espaços, de maneira estável.

Adicionar `SourcePrinter` **não exigiu alterar nenhuma classe de nó**: os dados
necessários já estavam no desenho da hierarquia. A gramática e o Scanner do CP1
não foram modificados; por isso **não há revisão** de `gramatica-first-follow.md`.

## 6. Política de erro e validação

`Parser.parse()` devolve `ParseResult` com `ast` e `errors`. Se existir erro
léxico ou sintático, `ast == null` e `accepted() == false`, impedindo uso de
árvore parcial. Durante reconhecimento, o parser recolhe nós temporários e
sincroniza nos mesmos FIRST de item/comando do CP2, com avanço garantido quando
o erro ocorre sem consumir token. Não se fazem decisões por tipo de AST.

Testes com **árvores esperadas escritas independentemente do impressor** estão
em `test/esperado/`; entradas, em `test/programas/`. `Cp3SmokeTest` cobre os
três programas, `a+b*c`, `a-b-c`, `a-(b-c)`, `(((a)))`, round-trip da árvore e
do texto, além dos casos inválidos e múltiplos erros do CP2. `AstTest` roda a
suíte pelo JUnit; `ParserTest` e `ScannerTest` preservam a regressão anterior.
