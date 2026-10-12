# Gramática da linguagem, FIRST/FOLLOW e verificação LL(1)

Esta gramática descreve a sintaxe da linguagem definida em `especificacao_lexica.md`, utilizando os tokens fornecidos pelo `Scanner` do CP1. Ela contempla declarações globais e locais, atribuições, expressões, chamadas, funções com parâmetros, blocos, `return` com ou sem expressão, `if`/`elif`/`else`, `while` e `for`. Funções com retorno `void` também permitem representar procedimentos.

Os tipos aceitos são `int`, `long`, `float`, `double`, `boolean`, `string`, `bool` e `char`, além de `void` **somente como tipo de retorno**. Os tipos `long`, `float` e `boolean` preservam as escolhas do CP1; `bool` e `char` atendem ao escopo fixo do CP2.

**Compatibilidade léxica:** o scanner atual classifica `bool` e `char` como `IDENTIFIER`, pois não constam de sua tabela de palavras reservadas. Para utilizá-los sem modificar o CP1, o parser deve distinguir esses dois lexemas no *lookahead*: `IDENTIFIER` com texto `"bool"` equivale ao terminal `"bool"`, e analogamente para `"char"`. Nos demais casos, `IDENTIFIER` corresponde a `ID`. Assim, `bool` e `char` não podem ser nomes de variáveis ou funções na sintaxe final. Essa normalização é determinística usando **apenas o token corrente** (`tipo` e `texto`), preservando as decisões LL(1).

| Terminal empregado | Representação no scanner do CP1 |
|---|---|
| `ID` | `IDENTIFIER`, exceto os lexemas contextuais `bool` e `char` |
| `LIT_INT`, `LIT_REAL`, `LIT_STRING` | `INT`, `REAL`, `STRING` |
| `"int"`, `"float"`, `"double"`, `"string"`, `"true"`, `"false"` | `RESERVED_WORD`, distinguido por `getText()` |
| Outras palavras reservadas | Seus tipos específicos (`FUNCTION`, `IF`, `BOOLEAN` etc.) ou o próprio lexema |
| Operadores e delimitadores entre aspas | `OPERATOR` ou `DELIMITER`, distinguido por `getText()` |
| `EOF` | Fim da sequência, detectado quando `nextToken()` retorna `null`; não é token emitido |

Comentários e espaços em branco são ignorados pelo scanner. `=` significa atribuição, enquanto `equal` é igualdade. Não há `==`, `!=`, `++` ou `--`. O sinal `-` é sempre um operador, não parte do literal numérico. O tipo `char` não introduz literal com aspas simples, pois o scanner do CP1 não possui esse token; uma string de um caractere, por exemplo `"a"`, pode ser usada em uma declaração, e a compatibilidade do valor com o tipo fica para uma eventual análise semântica.

## 1. Gramática final (BNF)

Não-terminais são escritos entre `< >`; terminais literais, entre aspas; `ID` e `LIT_*` são classes de tokens; `ε` representa sequência vazia. O símbolo inicial é `<programa>` e o reconhecimento só é concluído quando se alcança `EOF` após sua derivação.

```bnf
<programa> ::= <itens>
<itens> ::= <item> <itens> | ε
<item> ::= <funcao> | <decl_var>
<funcao> ::= "function" <tipo_retorno> ID "(" <params_opt> ")" <bloco>
<tipo_retorno> ::= <tipo> | "void"
<tipo> ::= "int" | "long" | "float" | "double" | "boolean" | "string" | "bool" | "char"
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
<cauda_igual> ::= "equal" <expr_rel> <cauda_igual> | ε
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

Em `for`, a inicialização admite declaração, atribuição ou vazio; a condição e a atualização também são opcionais, mas a atualização, quando presente, usa atribuição. Uma chamada isolada é aceita como comando (`nome(...);`), e uma chamada pode aparecer dentro de expressões (`x = nome(...);`). `if`/`elif`/`else`, `while` e `for` exigem blocos `{ ... }`, evitando ambiguidade de associação do `else`.

## 2. Eliminação da recursão à esquerda e fatoração

O ZIP do CP1 não continha uma versão anterior da gramática. Os casos abaixo mostram as formulações diretas que foram evitadas na construção desta versão; não representam um histórico de alterações já existente no repositório.

**Recursão à esquerda na soma — forma direta:**

```bnf
<expr_soma> ::= <expr_soma> "+" <expr_prod> | <expr_soma> "-" <expr_prod> | <expr_prod>
```

**Forma adotada, sem recursão à esquerda:**

```bnf
<expr_soma> ::= <expr_prod> <cauda_soma>
<cauda_soma> ::= "+" <expr_prod> <cauda_soma> | "-" <expr_prod> <cauda_soma> | ε
```

**Prefixo comum — forma sem fatoração:**

```bnf
<comando_id> ::= ID "=" <expr> ";" | ID "(" <args_opt> ")" ";"
```

**Forma fatorada adotada:**

```bnf
<comando_id> ::= ID <resto_comando_id> ";"
<resto_comando_id> ::= "=" <expr> | "(" <args_opt> ")"
```

As outras operações binárias usam o mesmo padrão de *operando + cauda*. A gramática final não possui recursão à esquerda direta nem indireta. O agrupamento de chamadas em `<sufixo_primario>` também separa `ID` simples de `ID(...)` sem alternativas com o mesmo prefixo.

## 3. Conjuntos FIRST e FOLLOW

Os conjuntos foram obtidos pela propagação das produções até um ponto fixo. Para manter a tabela legível, algumas uniões usam os seguintes conjuntos, definidos **explicitamente**:

- `T = {int, long, float, double, boolean, string, bool, char}` — inícios de tipos.
- `I = T ∪ {function}` — inícios de itens globais.
- `E = {ID, LIT_INT, LIT_REAL, LIT_STRING, true, false, "(", not, "+", "-"}` — inícios de expressão.
- `C = T ∪ {ID, if, while, for, return, "{"}` — inícios de comando.

`EOF` indica fim de arquivo no cálculo de FOLLOW, e `ε` aparece apenas nos FIRST de produções anuláveis. Todas as expressões com `∪` representam uniões de conjuntos (não são novos tokens).

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

## 4. Verificação explícita da condição LL(1)

Para cada não-terminal `A`, foram conferidas as três condições solicitadas: **(a)** interseção dos FIRST das alternativas (ignorando `ε`), **(b)** quantidade de alternativas anuláveis, que deve ser no máximo uma, e **(c)** interseção de FIRST das alternativas não anuláveis com FOLLOW(`A`) quando existe uma alternativa anulável. Em (a) e (c), `∅` significa ausência de conflito; `—` indica condição não aplicável. A ordem dos FIRST por alternativa acompanha a ordem das produções na Seção 2.

| Não-terminal | FIRST de cada alternativa | (a) Interseções | (b) Anuláveis | (c) FIRST × FOLLOW |
|---|---|---|---|---|
| `<programa>` | `I ∪ {ε}` | `—` | `1` | `∅` |
| `<itens>` | `I / {ε}` | `∅` | `1` | `∅` |
| `<item>` | `{function} / T` | `∅` | `0` | `—` |
| `<funcao>` | `{function}` | `—` | `0` | `—` |
| `<tipo_retorno>` | `T / {void}` | `∅` | `0` | `—` |
| `<tipo>` | `{int} / {long} / {float} / {double} / {boolean} / {string} / {bool} / {char}` | `∅` | `0` | `—` |
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

### 4.1. Exemplos de cálculo e conferência das condições

Os desenvolvimentos a seguir detalham casos representativos da tabela anterior: alternativas com inícios distintos, produções com `ε`, propagação de FOLLOW e listas recursivas. A notação `T`, `C` e `E` é a mesma definida na Seção 4.

**Parâmetros opcionais — `<params_opt>`**

```bnf
<params_opt> ::= <params> | ε
<params> ::= <param> <params_rest>
<param> ::= <tipo> ID
```

Como `<param>` começa por `<tipo>`, e `<tipo>` começa por um elemento de `T`, temos:

```text
FIRST(<params>)     = FIRST(<param>) = FIRST(<tipo>) = T
FIRST(<params_opt>) = T ∪ {ε}
```

Em `<funcao> ::= "function" <tipo_retorno> ID "(" <params_opt> ")" <bloco>`, o símbolo imediatamente seguinte a `<params_opt>` é `")"`. Portanto:

```text
FOLLOW(<params_opt>) = {")"}
(a) FIRST(<params>) ∩ FIRST(ε), desconsiderando ε = ∅
(b) Alternativas anuláveis = 1
(c) T ∩ {")"} = ∅
```

Assim, um tipo inicia a lista de parâmetros e `")"` autoriza a alternativa vazia.

**Alternativas distintas — `<comando>`**

```bnf
<comando> ::= <decl_var> | <comando_id> | <condicional> | <enquanto>
            | <para> | <retorno> | <bloco>
```

Aplicando FIRST a cada alternativa, pela primeira palavra ou classe de token que ela pode consumir:

```text
FIRST(<decl_var>)    = T
FIRST(<comando_id>)  = {ID}
FIRST(<condicional>) = {if}
FIRST(<enquanto>)    = {while}
FIRST(<para>)        = {for}
FIRST(<retorno>)     = {return}
FIRST(<bloco>)       = {"{"}
```

**(a)** Os sete conjuntos são disjuntos dois a dois, portanto todas as interseções são `∅`. **(b)** Nenhuma alternativa deriva `ε`. **(c)** Não se aplica, já que não existe alternativa anulável. O token corrente, sozinho, identifica qual produção deve ser seguida.

**Continuação da seleção — `<cauda_cond>`**

```bnf
<cauda_cond> ::= "elif" "(" <expr> ")" <bloco> <cauda_cond>
               | "else" <bloco>
               | ε
```

Os três FIRST por alternativa são `{elif}`, `{else}` e `{ε}`. Para calcular FOLLOW, observamos que `<cauda_cond>` encerra `<condicional>`, que por sua vez é alternativa de `<comando>`. Além disso, `<comando>` aparece em `<comandos> ::= <comando> <comandos>` e `<comandos>` é encerrado por `"}"` em `<bloco>`:

```text
FOLLOW(<comandos>)   = {"}"}
FOLLOW(<comando>)    = (FIRST(<comandos>) − {ε}) ∪ FOLLOW(<comandos>)
                     = C ∪ {"}"}
FOLLOW(<condicional>) = FOLLOW(<comando>)
FOLLOW(<cauda_cond>)  = FOLLOW(<condicional>) = C ∪ {"}"}
```

Verificação: **(a)** `{elif} ∩ {else} = ∅`; **(b)** apenas a terceira alternativa é anulável; **(c)** `{elif, else} ∩ (C ∪ {"}"}) = ∅`. Consequentemente, `elif` ou `else` prolongam a seleção; um token de FOLLOW encerra a cauda por `ε`.

**Operações de soma — `<cauda_soma>`**

```bnf
<expr_soma> ::= <expr_prod> <cauda_soma>
<cauda_soma> ::= "+" <expr_prod> <cauda_soma>
               | "-" <expr_prod> <cauda_soma>
               | ε
```

O FIRST de cada alternativa é `{+}`, `{-}` e `{ε}`. Como `<cauda_soma>` aparece no fim de `<expr_soma>`, herda seu FOLLOW. Este é propagado a partir das expressões de maior abrangência: `<expr_rel>` pode ser seguido por `equal`, `<expr_igual>` por `and`, `<expr_and>` por `or`, e `<expr>` por `")"`, `","` ou `";"`.

```text
FOLLOW(<expr>)       = {")", ",", ";"}
FOLLOW(<expr_and>)   = {or} ∪ FOLLOW(<expr>)
FOLLOW(<expr_igual>) = {and} ∪ FOLLOW(<expr_and>)
FOLLOW(<expr_rel>)   = {equal} ∪ FOLLOW(<expr_igual>)
FOLLOW(<expr_soma>)  = {"<", "<=", ">", ">="} ∪ FOLLOW(<expr_rel>)
FOLLOW(<cauda_soma>) = FOLLOW(<expr_soma>)
                     = {"<", "<=", ">", ">=", equal, and, or, ")", ",", ";"}
```

Verificação: **(a)** `{+} ∩ {-} = ∅`; **(b)** existe uma única alternativa anulável; **(c)** `{+, -} ∩ FOLLOW(<cauda_soma>) = ∅`. Dessa forma, `+` e `-` prosseguem a soma, enquanto operadores de outros níveis ou delimitadores encerram a cauda.

**Identificador ou chamada — `<sufixo_primario>`**

```bnf
<primario> ::= LIT_INT | LIT_REAL | LIT_STRING | "true" | "false"
             | ID <sufixo_primario> | "(" <expr> ")"
<sufixo_primario> ::= "(" <args_opt> ")" | ε
```

O primeiro caso de `<sufixo_primario>` começa com `"("`; o segundo deriva `ε`. Como o sufixo encerra a alternativa `ID <sufixo_primario>`, ele herda `FOLLOW(<primario>)`, formado pelos operadores que podem continuar a expressão e por seus delimitadores:

```text
FIRST(<sufixo_primario>) = {"(", ε}
FOLLOW(<sufixo_primario>) = FOLLOW(<primario>)
                           = {"+", "-", "*", "/", "%", equal, and, or,
                              "<", "<=", ">", ">=", ")", ",", ";"}
(a) {"("} ∩ ∅ = ∅   (segunda alternativa é ε)
(b) Alternativas anuláveis = 1
(c) {"("} ∩ FOLLOW(<sufixo_primario>) = ∅
```

Logo, um `"("` imediatamente após `ID` identifica uma chamada, e um token de FOLLOW permite reconhecer apenas o identificador. Um token que não pertença a nenhum desses conjuntos representa erro sintático, não a escolha automática por `ε`.

Esses exemplos ilustram as mesmas regras aplicadas aos demais não-terminais na tabela. Em particular, uma alternativa vazia somente pode ser selecionada se o *lookahead* estiver em FOLLOW do não-terminal correspondente.

**Resultado:** os 44 não-terminais satisfazem as três condições. Não foram encontrados conflitos FIRST/FIRST nem FIRST/FOLLOW; nenhum não-terminal possui mais de uma alternativa anulável. Portanto, considerando os terminais distinguidos por tipo e lexema conforme a Seção 1, a gramática é LL(1).
