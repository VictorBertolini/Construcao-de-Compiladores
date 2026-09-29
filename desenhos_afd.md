# Autômatos Finitos Determinísticos
## 1. Caracteres válidos
```
letra   = [A-Za-z]
dígito  = [0-9]
```
## 2. Identificadores e palavras reservadas

```
identificador = (letra | "_") (letra | dígito | "_")*
```

```mermaid
flowchart LR
    init((" ")) --> ID_INICIO((ID_INICIO))
    ID_INICIO -- "letra ou _" --> ID_CORPO(((ID_CORPO)))
    ID_CORPO -- "letra ou digito ou _" --> ID_CORPO

    style init fill:none,stroke:none
```

## 3. Strings

```
string          = aspas (caractere_comum | barra escape)* aspas
aspas           = "
barra           = \
caractere_comum = qualquer caractere exceto aspas, barra e quebra de linha
escape          = um dos caracteres: "  \  n  t
```

Nessa notação, `escape` é o caractere lido depois da barra. As sequências completas permitidas são `\"`, `\\`, `\n` e `\t`.

```mermaid
flowchart LR
    init((" ")) --> STR_INICIO((STR_INICIO))
    STR_INICIO -- "aspas" --> STR_CORPO((STR_CORPO))
    
    STR_CORPO -- "\" --> STR_ESCAPE((STR_ESCAPE))
    
    STR_ESCAPE -- "n ou t ou \\ ou aspas-duplas" --> STR_CORPO
    
    STR_CORPO -- "caractere_comum" --> STR_CORPO
    STR_CORPO -- "aspas" --> STR_FIM(((STR_FIM)))
    
    ACEITA["ACEITA STRING"] -.- STR_FIM
    
    style init fill:none,stroke:none
```

## 4. Literais numéricos

```
inteiro = dígito+
real    = dígito+ "." dígito+
```

```mermaid
flowchart LR
    init((" ")) --> NUM_INICIO((NUM_INICIO))
    
    NUM_INICIO -- "digito" --> NUM_INTEIRO(((NUM_INTEIRO)))
    NUM_INTEIRO -- "digito" --> NUM_INTEIRO
    
    NUM_INTEIRO -- "ponto" --> NUM_PONTO((NUM_PONTO))
    NUM_PONTO -- "digito" --> NUM_REAL(((NUM_REAL)))
    NUM_REAL -- "digito" --> NUM_REAL

    note1["ACEITA INT"] -.- NUM_INTEIRO
    note2["ACEITA REAL"] -.- NUM_REAL

    style init fill:none,stroke:none
```

Depois do primeiro dígito, já existe um inteiro válido. A leitura permanece em `NUM_INTEIRO` enquanto houver dígitos; se aparecer `.`, passa para `NUM_PONTO`.

Esse estado não aceita. Falta pelo menos um dígito para completar a parte decimal e chegar a `NUM_REAL`.


## 5. Operadores

Os operadores simbólicos são `+`, `-`, `*`, `/`, `%`, `=`, `<`, `>`, `<=` e `>=`. Todos geram o tipo `OPERATOR`; o lexema identifica qual foi lido.

```mermaid
flowchart TB
    init((" ")) --> OP_INICIO((OP_INICIO))
    
    OP_INICIO -- "+ ou - ou * ou / ou % ou =" --> OP_SIMPLES(((OP_SIMPLES)))
    OP_INICIO -- "menor ou maior" --> OP_RELACIONAL(((OP_RELACIONAL)))
    OP_RELACIONAL -- "=" --> OP_COMPOSTO(((OP_COMPOSTO)))

    style init fill:none,stroke:none
```


No desenho, `menor` representa `<` e `maior` representa `>`.

Os operadores de um caractere chegam a `OP_SIMPLES`. Para `<` e `>`, existe uma leitura adicional: se o próximo caractere for `=`, ele é consumido e o autômato chega a `OP_COMPOSTO`. Caso contrário, o scanner emite o operador já reconhecido, sem consumir o próximo caractere.

Essa é a regra de **maximal munch** para os operadores: `<=` é um token só. O mesmo vale para `>=`.

Não há `==` nem `!=`. O texto `==` produz dois tokens `=`, enquanto `!` gera erro léxico. A igualdade é escrita com `equal`; para desigualdade, usa-se `not (a equal b)`.

## 6. Delimitadores

```
delimitador = "(" | ")" | "{" | "}" | "," | ";"
```

```mermaid
flowchart LR
    init((" ")) --> DEL_INICIO((DEL_INICIO))
    DEL_INICIO -- "delimitador" --> DEL_FIM(((DEL_FIM)))
    
    style init fill:none,stroke:none
```

Cada delimitador ocupa um caractere e gera `DELIMITER`. Não há forma composta.

| Símbolo | Uso |
|---|---|
| `;` | termina todo comando |
| `,` | separa parâmetros e argumentos |
| `( )` | parâmetros, chamadas e expressões |
| `{ }` | blocos |

## 7. Comentários e espaços em branco

Comentários começam e terminam com `$`. Entre esses marcadores, qualquer caractere é permitido, inclusive quebra de linha.

```mermaid
flowchart LR
    init((" ")) --> COM_INICIO((COM_INICIO))
    COM_INICIO -- "$" --> COM_CORPO((COM_CORPO))
    
    COM_CORPO -- "qualquer caractere diferente de $" --> COM_CORPO
    COM_CORPO -- "$" --> COM_FIM(((COM_FIM)))

    style init fill:none,stroke:none
```



