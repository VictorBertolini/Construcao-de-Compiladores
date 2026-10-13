# Gramática da linguagem, FIRST/FOLLOW e verificação LL(1)

<details><summary>Glossário para consulta</summary>

| Símbolo ou termo | Significado | Exemplo na gramática |
|---|---|---|
| **BNF** | Notação que descreve uma linguagem por meio de regras de produção. | `<params_opt> ::= <params> \| ε` |
| **Não-terminal** | Categoria sintática definida por outras regras; aparece entre `<` e `>`. | `<expr>`, `<tipo>`, `<params_opt>` |
| **Terminal** | Símbolo reconhecido na entrada, sem novas expansões pela gramática. | `"if"`, `"("`, `ID`, `LIT_INT` |
| **`::=`** | “É definido por” ou “pode ser substituído por”. | `<param> ::= <tipo> ID` |
| **`\|`** | Separa alternativas: significa “ou”. | `<params_opt>` pode produzir `<params>` **ou** `ε`. |
| **`ε` (épsilon)** | Sequência vazia: a regra pode não consumir nenhum token. Não é um caractere digitado no programa. | `<params_opt> ::= <params> \| ε` |
| **`<programa>`** | Símbolo inicial da gramática; é por ele que o reconhecimento começa. | `<programa> ::= <itens>` |
| **`ID`** | Classe de token de identificadores (nomes de variáveis e funções), conforme a convenção descrita na Seção 1. | `ID` em `<param> ::= <tipo> ID` |
| **`LIT_INT`, `LIT_REAL`, `LIT_STRING`** | Classes de tokens para literais inteiros, reais e strings. | `<primario> ::= LIT_INT \| ...` |
| **`EOF`** | Fim da entrada: indica que não há mais tokens após o programa. | O reconhecimento de `<programa>` deve chegar ao `EOF`. |
| **Derivação** | Aplicação sucessiva das regras até obter apenas terminais (ou a sequência vazia). | `<params_opt> ⇒ ε` |
| **Anulável** | Não-terminal que pode derivar `ε`. | `<params_opt>` é anulável. |
| **FIRST(A)** | Conjunto de terminais que podem iniciar cadeias derivadas do não-terminal `A`; inclui `ε` quando `A` é anulável. | `FIRST(<params_opt>)` ajuda a decidir se há parâmetros. |
| **FOLLOW(A)** | Conjunto de terminais que podem aparecer imediatamente depois de `A` numa derivação válida; pode incluir `EOF`. | `")"` pode vir depois de `<params_opt>`. |
| **LL(1)** | Gramática analisável da esquerda para a direita, construindo derivação mais à esquerda, com **um** token de antecipação. | O próximo token determina a alternativa válida. |
| **Lookahead** | Próximo token examinado pelo parser, antes de decidir a produção. | `")"` permite escolher `ε` em `<params_opt>`. |
| **Recursão à esquerda** | Situação em que um não-terminal pode produzir uma sequência iniciada por ele mesmo; precisa ser eliminada para descida recursiva preditiva. | `<expr_soma> ::= <expr_soma> "+" <expr_prod> \| ...` (forma não adotada). |
| **Fatoração à esquerda** | Extração de um prefixo comum a alternativas para adiar a decisão do parser. | `ID` é consumido antes de decidir entre atribuição e chamada. |
| **Interseção (`∩`) e conjunto vazio (`∅`)** | `∩` indica elementos comuns entre conjuntos; `∅` significa ausência de elementos comuns. | `FIRST(α) ∩ FIRST(β) = ∅` expressa alternativas sem conflito FIRST/FIRST. |


</details>

<br><br>

Os tipos aceitos são `int`, `long`, `float`, `double`, `boolean` e `string`, além de `void` **somente como tipo de 
retorno**. Os tipos `long`, `float` e `boolean` preservam as escolhas do CP1.

| Terminal empregado                                              | Representação no scanner do CP1                                                      |
|-----------------------------------------------------------------|--------------------------------------------------------------------------------------|
| `ID`                                                            | `IDENTIFIER`                                                                         |
| `LIT_INT`, `LIT_REAL`, `LIT_STRING`                             | `INT`, `REAL`, `STRING`                                                              |
| `"int"`, `"float"`, `"double"`, `"string"`, `"true"`, `"false"` | `RESERVED_WORD`, distinguido por `getText()`                                         |
| Outras palavras reservadas                                      | Seus tipos específicos (`FUNCTION`, `IF`, `BOOLEAN` etc.) ou o próprio lexema        |
| Operadores e delimitadores entre aspas                          | `OPERATOR` ou `DELIMITER`, distinguido por `getText()`                               |
| `EOF`                                                           | Fim da sequência, detectado quando `nextToken()` retorna `null`; não é token emitido |

Comentários e espaços em branco são ignorados pelo scanner. `=` significa atribuição, enquanto `equal` é igualdade. Não há `==`, `!=`, `++` ou `--`. O sinal `-` é sempre um operador, não parte do literal numérico. O tipo `char` não introduz literal com aspas simples, pois o scanner do CP1 não possui esse token; uma string de um caractere, por exemplo `"a"`, pode ser usada em uma declaração, e a compatibilidade do valor com o tipo fica para uma eventual análise semântica.

## 1. Gramática final (BNF)

Não-terminais são escritos entre `< >`; terminais literais, entre aspas; `ID` e `LIT_*` são classes de tokens; `ε` representa sequência vazia. O símbolo inicial é `<programa>` e o reconhecimento só é concluído quando se alcança `EOF` após sua derivação.

```bnf
<programa> ::= <itens>
<itens> ::= <item> <itens> | ε
<item> ::= <funcao> | <decl_var>
<funcao> ::= "function" <tipo_retorno> ID "(" <params_opt> ")" <bloco>
<tipo_retorno> ::= <tipo> | "void"
<tipo> ::= "int" | "long" | "float" | "double" | "boolean" | "string"
<params_opt> ::= <params> | ε
<params> ::= <param> <params_rest>
<params_rest> ::= "," <param> <params_rest> | ε
<param> ::= <tipo> ID
<decl_var> ::= <tipo> ID <inicializador> ";"
<inicializador> ::= "=" <expr> | ε
<bloco> ::= "{" <comandos> "}"
<comandos> ::= <comando> <comandos> | ε
<comando> ::= <decl_var> | <comando_id> | <condicional> | <enquanto> | <para> | <retorno> | <bloco>
<comando_id> ::= ID <resto_comando_id> ";"
<resto_comando_id> ::= "=" <expr> | "(" <args_opt> ")"
<condicional> ::= "if" "(" <expr> ")" <bloco> <cauda_cond>
<cauda_cond> ::= "elif" "(" <expr> ")" <bloco> <cauda_cond> | "else" <bloco> | ε
<enquanto> ::= "while" "(" <expr> ")" <bloco>
<para> ::= "for" "(" <init_for> ";" <cond_for> ";" <passo_for> ")" <bloco>
<init_for> ::= <tipo> ID <inicializador> | ID "=" <expr> | ε
<cond_for> ::= <expr> | ε
<passo_for> ::= ID "=" <expr> | ε
<retorno> ::= "return" <retorno_opt> ";"
<retorno_opt> ::= <expr> | ε
<expr> ::= <expr_and> <cauda_or>
<cauda_or> ::= "or" <expr_and> <cauda_or> | ε
<expr_and> ::= <expr_igual> <cauda_and>
<cauda_and> ::= "and" <expr_igual> <cauda_and> | ε
<expr_igual> ::= <expr_rel> <cauda_igual>
<cauda_igual> ::= "equal" <expr_rel> | ε
<expr_rel> ::= <expr_soma> <cauda_rel>
<cauda_rel> ::= "<" <expr_soma> | ">" <expr_soma> | "<=" <expr_soma> | ">=" <expr_soma> | ε
<expr_soma> ::= <expr_prod> <cauda_soma>
<cauda_soma> ::= "+" <expr_prod> <cauda_soma> | "-" <expr_prod> <cauda_soma> | ε
<expr_prod> ::= <expr_unaria> <cauda_prod>
<cauda_prod> ::= "*" <expr_unaria> <cauda_prod> | "/" <expr_unaria> <cauda_prod> | "%" <expr_unaria> <cauda_prod> | ε
<expr_unaria> ::= "not" <expr_unaria> | "-" <expr_unaria> | "+" <expr_unaria> | <primario>
<primario> ::= LIT_INT | LIT_REAL | LIT_STRING | "true" | "false" | ID <sufixo_primario> | "(" <expr> ")"
<sufixo_primario> ::= "(" <args_opt> ")" | ε
<args_opt> ::= <args> | ε
<args> ::= <expr> <args_rest>
<args_rest> ::= "," <expr> <args_rest> | ε
```

## 2. Conjuntos FIRST e FOLLOW

| Não-terminal | FIRST | FOLLOW |
|---|---|---|
| `<programa>` | `I ∪ {ε}` | `{EOF}` |
| `<itens>` | `I ∪ {ε}` | `{EOF}` |
| `<item>` | `I` | `I ∪ {EOF}` |
| `<funcao>` | `{function}` | `I ∪ {EOF}` |
| `<tipo_retorno>` | `T ∪ {void}` | `{ID}` |
| `<tipo>` | `T` | `{ID}` |
| `<params_opt>` | `T ∪ {ε}` | `{")"}` |
| `<params>` | `T` | `{")"}` |
| `<params_rest>` | `{ε, ","}` | `{")"}` |
| `<param>` | `T` | `{")", ","}` |
| `<decl_var>` | `T` | `C ∪ {EOF, function, "}"}` |
| `<inicializador>` | `{ε, "="}` | `{";"}` |
| `<bloco>` | `{"{"}` | `C ∪ {EOF, function, elif, else, "}"}` |
| `<comandos>` | `C ∪ {ε}` | `{"}"}` |
| `<comando>` | `C` | `C ∪ {"}"}` |
| `<comando_id>` | `{ID}` | `C ∪ {"}"}` |
| `<resto_comando_id>` | `{"=", "("}` | `{";"}` |
| `<condicional>` | `{if}` | `C ∪ {"}"}` |
| `<cauda_cond>` | `{ε, elif, else}` | `C ∪ {"}"}` |
| `<enquanto>` | `{while}` | `C ∪ {"}"}` |
| `<para>` | `{for}` | `C ∪ {"}"}` |
| `<init_for>` | `T ∪ {ε, ID}` | `{";"}` |
| `<cond_for>` | `E ∪ {ε}` | `{";"}` |
| `<passo_for>` | `{ε, ID}` | `{")"}` |
| `<retorno>` | `{return}` | `C ∪ {"}"}` |
| `<retorno_opt>` | `E ∪ {ε}` | `{";"}` |
| `<expr>` | `E` | `{")", ",", ";"}` |
| `<cauda_or>` | `{ε, or}` | `{")", ",", ";"}` |
| `<expr_and>` | `E` | `{or, ")", ",", ";"}` |
| `<cauda_and>` | `{ε, and}` | `{or, ")", ",", ";"}` |
| `<expr_igual>` | `E` | `{and, or, ")", ",", ";"}` |
| `<cauda_igual>` | `{ε, equal}` | `{and, or, ")", ",", ";"}` |
| `<expr_rel>` | `E` | `{equal, and, or, ")", ",", ";"}` |
| `<cauda_rel>` | `{ε, "<", "<=", ">", ">="}` | `{equal, and, or, ")", ",", ";"}` |
| `<expr_soma>` | `E` | `{equal, and, or, "<", "<=", ">", ">=", ")", ",", ";"}` |
| `<cauda_soma>` | `{ε, "+", "-"}` | `{equal, and, or, "<", "<=", ">", ">=", ")", ",", ";"}` |
| `<expr_prod>` | `E` | `{"+", "-", equal, and, or, "<", "<=", ">", ">=", ")", ",", ";"}` |
| `<cauda_prod>` | `{ε, "*", "/", "%"}` | `{"+", "-", equal, and, or, "<", "<=", ">", ">=", ")", ",", ";"}` |
| `<expr_unaria>` | `E` | `{"+", "-", "*", "/", "%", equal, and, or, "<", "<=", ">", ">=", ")", ",", ";"}` |
| `<primario>` | `{ID, LIT_INT, LIT_REAL, LIT_STRING, true, false, "("}` | `{"+", "-", "*", "/", "%", equal, and, or, "<", "<=", ">", ">=", ")", ",", ";"}` |
| `<sufixo_primario>` | `{ε, "("}` | `{"+", "-", "*", "/", "%", equal, and, or, "<", "<=", ">", ">=", ")", ",", ";"}` |
| `<args_opt>` | `E ∪ {ε}` | `{")"}` |
| `<args>` | `E` | `{")"}` |
| `<args_rest>` | `{ε, ","}` | `{")"}` |

## 3. Verificação explícita da condição LL(1)

Para cada não-terminal `A`, foram conferidas as três condições solicitadas: 

- **(a)** interseção dos FIRST das alternativas (ignorando `ε`),
- **(b)** quantidade de alternativas anuláveis, que 
deve ser no máximo uma, e 
- **(c)** interseção de FIRST das alternativas não anuláveis com FOLLOW(`A`) quando existe uma alternativa anulável. 

Em (a) e (c), `∅` significa ausência de conflito; `—` indica condição não aplicável. A ordem dos FIRST por alternativa acompanha a ordem das produções na Seção 2.

| Não-terminal | FIRST de cada alternativa | (a) Interseções | (b) Anuláveis | (c) FIRST × FOLLOW |
|---|---|---|---|---|
| `<programa>` | `I ∪ {ε}` | `—` | `1` | `∅` |
| `<itens>` | `I / {ε}` | `∅` | `1` | `∅` |
| `<item>` | `{function} / T` | `∅` | `0` | `—` |
| `<funcao>` | `{function}` | `—` | `0` | `—` |
| `<tipo_retorno>` | `T / {void}` | `∅` | `0` | `—` |
| `<tipo>` | `{int} / {long} / {float} / {double} / {boolean} / {string}` | `∅` | `0` | `—` |
| `<params_opt>` | `T / {ε}` | `∅` | `1` | `∅` |
| `<params>` | `T` | `—` | `0` | `—` |
| `<params_rest>` | `{","} / {ε}` | `∅` | `1` | `∅` |
| `<param>` | `T` | `—` | `0` | `—` |
| `<decl_var>` | `T` | `—` | `0` | `—` |
| `<inicializador>` | `{"="} / {ε}` | `∅` | `1` | `∅` |
| `<bloco>` | `{"{"}` | `—` | `0` | `—` |
| `<comandos>` | `C / {ε}` | `∅` | `1` | `∅` |
| `<comando>` | `T / {ID} / {if} / {while} / {for} / {return} / {"{"}` | `∅` | `0` | `—` |
| `<comando_id>` | `{ID}` | `—` | `0` | `—` |
| `<resto_comando_id>` | `{"="} / {"("}` | `∅` | `0` | `—` |
| `<condicional>` | `{if}` | `—` | `0` | `—` |
| `<cauda_cond>` | `{elif} / {else} / {ε}` | `∅` | `1` | `∅` |
| `<enquanto>` | `{while}` | `—` | `0` | `—` |
| `<para>` | `{for}` | `—` | `0` | `—` |
| `<init_for>` | `T / {ID} / {ε}` | `∅` | `1` | `∅` |
| `<cond_for>` | `E / {ε}` | `∅` | `1` | `∅` |
| `<passo_for>` | `{ID} / {ε}` | `∅` | `1` | `∅` |
| `<retorno>` | `{return}` | `—` | `0` | `—` |
| `<retorno_opt>` | `E / {ε}` | `∅` | `1` | `∅` |
| `<expr>` | `E` | `—` | `0` | `—` |
| `<cauda_or>` | `{or} / {ε}` | `∅` | `1` | `∅` |
| `<expr_and>` | `E` | `—` | `0` | `—` |
| `<cauda_and>` | `{and} / {ε}` | `∅` | `1` | `∅` |
| `<expr_igual>` | `E` | `—` | `0` | `—` |
| `<cauda_igual>` | `{equal} / {ε}` | `∅` | `1` | `∅` |
| `<expr_rel>` | `E` | `—` | `0` | `—` |
| `<cauda_rel>` | `{"<"} / {">"} / {"<="} / {">="} / {ε}` | `∅` | `1` | `∅` |
| `<expr_soma>` | `E` | `—` | `0` | `—` |
| `<cauda_soma>` | `{"+"} / {"-"} / {ε}` | `∅` | `1` | `∅` |
| `<expr_prod>` | `E` | `—` | `0` | `—` |
| `<cauda_prod>` | `{"*"} / {"/"} / {"%"} / {ε}` | `∅` | `1` | `∅` |
| `<expr_unaria>` | `{not} / {"-"} / {"+"} / {ID, LIT_INT, LIT_REAL, LIT_STRING, true, false, "("}` | `∅` | `0` | `—` |
| `<primario>` | `{LIT_INT} / {LIT_REAL} / {LIT_STRING} / {true} / {false} / {ID} / {"("}` | `∅` | `0` | `—` |
| `<sufixo_primario>` | `{"("} / {ε}` | `∅` | `1` | `∅` |
| `<args_opt>` | `E / {ε}` | `∅` | `1` | `∅` |
| `<args>` | `E` | `—` | `0` | `—` |
| `<args_rest>` | `{","} / {ε}` | `∅` | `1` | `∅` |
