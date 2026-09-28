# Especificação Léxica

Checkpoint 1 — Construção de Compiladores · UFU.FACOM.BCC

Este documento é o contrato do analisador léxico: o scanner implementa exatamente o que está descrito aqui, nem mais nem menos.

## 1. Convenções de notação

```
letra  = [A-Za-z]
dígito = [0-9]
```

Notação EBNF: `( )` agrupa, `|` alterna, `*` zero ou mais, `+` um ou mais, `?` opcional. Terminais ficam entre aspas.

## 2. Categorias de token

| Categoria | Notação (regex/EBNF) | Exemplos válidos | Decisões |
|---|---|---|---|
| Identificador | `( letra \| "_" ) ( letra \| dígito \| "_" )*` | `total`, `x1`, `_aux`, `contaItens` | Case-sensitive; `_` permitido, inclusive no início; não começa com dígito; **máximo 127 caracteres** (acima disso é erro léxico); apenas letras ASCII (sem acentos) |
| Palavra reservada | mesmo padrão do identificador + tabela de busca | `if`, `while`, `int`, `return` | Lista fechada na seção 4; sempre minúsculas |
| String | `"\"" ( caractere_comum \| escape )* "\""` | `"ok"`, `"linha 1"`, `"a\tb\n"` | Não pode conter quebra de linha; ver detalhes abaixo |
| Operador | `"+" \| "-" \| "*" \| "/" \| "%" \| "=" \| "<" \| ">" \| "<=" \| ">="` e as palavras `and`, `or`, `not`, `equal` | `=`, `<=`, `+`, `and` | Formas compostas: apenas `<=` e `>=` |
| Literal numérico inteiro (`INT`) | `dígito+` | `10`, `0`, `007` | Zeros à esquerda são válidos |
| Literal numérico real (`REAL`) | `dígito+ "." dígito+` | `3.14`, `0.5`, `1.2` | Sem `3.`, sem `.5`; sem notação científica nem hexadecimal |
| Delimitador | `"(" \| ")" \| "{" \| "}" \| "," \| ";"` | `(`, `;` | Categoria auxiliar, necessária para chamadas, blocos e parâmetros |

### 2.1 Strings

```
caractere_comum = qualquer caractere exceto  "  \  e quebra de linha
escape          = "\\\"" | "\\\\" | "\\n" | "\\t"      (ou seja:  \"   \\   \n   \t )
```

- Dentro de string qualquer caractere é aceito (inclusive acentos e `$`), exceto os excluídos acima.
- Uma string **não pode ocupar mais de uma linha**: quebra de linha antes do `"` final é erro léxico. Não existe continuação de linha com `\`.
- `\` seguido de qualquer outro caractere que não seja `"`, `\`, `n` ou `t` é erro léxico (escape inválido).
- O lexema guarda o texto original; a interpretação dos escapes é feita depois do scanner.

### 2.2 Números

- O sinal `-` **nunca** faz parte do literal: é sempre o operador de subtração/negação, e o parser decide.
- `1.2.3` é reconhecido como `REAL(1.2)`; o segundo `.` não inicia nenhum token válido e gera erro léxico, e a tokenização segue com `INT(3)`.
- `3.` (ponto não seguido de dígito) é erro léxico de número real mal formado.

### 2.3 Operadores

| Operador | Significado |
|---|---|
| `+` `-` `*` `/` `%` | aritméticos |
| `=` | atribuição |
| `<` `>` `<=` `>=` | relacionais |
| `equal` | igualdade |
| `and` `or` `not` | lógicos |

Não há `==` nem `!=`; a desigualdade é escrita `not (a equal b)`. As palavras `and`, `or`, `not` e `equal` seguem o padrão do identificador, ficam na tabela de palavras reservadas e o scanner as emite com token do tipo operador.

### 2.4 Delimitadores

| Símbolo | Uso |
|---|---|
| `;` | termina todo comando |
| `,` | separa parâmetros e argumentos |
| `( )` | parâmetros, chamadas e expressões |
| `{ }` | blocos, como em Java e C |

## 3. Respostas às perguntas obrigatórias

### 3.1 Qual é o alfabeto de entrada?

Caracteres que podem aparecer **fora** de strings e comentários:

- letras `A-Z` e `a-z`
- dígitos `0-9`
- `_`
- operadores: `+ - * / % = < >`
- delimitadores: `( ) { } , ;`
- `"` (abre string), `$` (abre/fecha comentário) e `.` (somente dentro de um literal real)
- espaço, tabulação, `\r` e `\n` (espaço em branco)

`\` só é válido dentro de strings, como início de escape. **Dentro de strings** vale qualquer caractere (inclusive acentuado) exceto `"`, `\` e quebra de linha isolados, conforme a seção 2.1. **Dentro de comentários** vale qualquer caractere, exceto `$`, que fecha o comentário.

Qualquer outro caractere fora de string ou comentário (por exemplo `@`, `#`, `!`, `[`, `]`, letras acentuadas) é erro léxico.

### 3.2 A linguagem é case-sensitive?

Sim. `Total` e `total` são identificadores diferentes. Palavras reservadas são sempre minúsculas e seguem a mesma regra: `IF` e `Int` são identificadores comuns, não palavras reservadas.

### 3.3 Espaços em branco e comentários

- **Espaços em branco** (espaço, tab, `\r`, `\n`) são ignorados e servem apenas para separar tokens. Não têm significado sintático: o fim de um comando é sempre o `;`.
- **Comentários** são escritos entre `$` e `$`, e o mesmo formato serve para uma linha ou várias:

  ```
  $ comentário de uma linha $
  $ comentário
    de várias linhas $
  ```

- Comentários **não podem ser aninhados**: o primeiro `$ ` depois da abertura fecha o comentário.
- Comentários são descartados pelo scanner e não geram token.
- `$` sem par até o fim do arquivo é erro léxico (comentário não fechado), reportado na posição do `$` de abertura.
- Dentro de strings, `$` é um caractere comum.

### 3.4 Desambiguação de operadores (maximal munch)

O scanner aplica **maximal munch**: sempre consome o maior prefixo do texto restante que ainda forma um token válido. Isso vale em três situações:

- `<=` é um único token, não `<` seguido de `=` (idem `>=`); `<` e `>` só saem sozinhos quando o próximo caractere não é `=`.
- `iffy` e `integer` são identificadores, não `if` + `fy` nem `int` + `eger`; `if` só é palavra reservada quando o lexema inteiro está na tabela.
- `10.5` é um único `REAL`, não `INT`, `.` e `INT`.

Como não existe `==`, o texto `==` é reconhecido como dois tokens `=` e cabe ao parser rejeitá-lo.

### 3.5 Palavras reservadas (lista fechada)

| Grupo | Palavras |
|---|---|
| Tipos | `int`, `long`, `float`, `double`, `boolean`, `string`, `void` |
| Controle | `if`, `elif`, `else`, `while`, `for` |
| Funções | `function`, `return` |
| Literais booleanos | `true`, `false` |
| Operadores em forma de palavra | `and`, `or`, `not`, `equal` |

Total: 20 palavras. Qualquer outra sequência que case com o padrão de identificador é um identificador, inclusive nomes de funções da biblioteca, como `print`.

## 4. Erros léxicos e recuperação

Todo erro é reportado com linha e coluna do ponto onde ocorre. Depois de reportar, o scanner segue para o próximo token.

| Situação | Comportamento |
|---|---|
| Caractere fora do alfabeto | Reporta o caractere, descarta-o e continua |
| String não fechada até o fim da linha | Reporta na posição da abertura `"`, e continua na linha seguinte |
| String não fechada até EOF | Reporta na posição da abertura `"` e termina no EOF |
| Escape inválido em string | Reporta e continua dentro da string |
| Comentário `$` não fechado até EOF | Reporta na posição do `$` de abertura e termina no EOF |
| Identificador com mais de 127 caracteres | Reporta erro léxico e continua depois do lexema |
| Número real mal formado (`3.`) | Reporta e continua depois do `.` |

## 5. Exemplo

```
function int soma(int a, int b) {
    return a + b; $ soma dois inteiros $
}
```

Tokens produzidos, em ordem: `function` `int` `soma` `(` `int` `a` `,` `int` `b` `)` `{` `return` `a` `+` `b` `;` `}`. O comentário e os espaços em branco não geram token.