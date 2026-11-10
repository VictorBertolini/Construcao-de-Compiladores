# CP4 — Desenho do ambiente e decisões de escopo

Grupo: Sthephanny Caroline e Victor Bertolini.

> **Importante:** este documento registra a especificação e um modelo textual de
> conferência. O desenho original no papel e a validação independente das saídas
> devem ser feitos e fotografados pelo grupo, seguindo `manual/ROTEIRO-DA-PARTE-MANUAL.md`.
> Não apresentar o modelo digitado como se fosse uma folha escrita à mão.

## 1. Programa-base para desenhar

É o mesmo conteúdo de `test/programas/ambiente_manual.txt`. Os números abaixo
são referências de linha, e não fazem parte do código-fonte.

```text
 1  int x = 1;
 2  function int exemplo(int p) {
 3      int soma = p;
 4      {
 5          int x = 2;
 6          {
 7              int soAqui = x;
 8              soma = soAqui;
 9          }
10          soma = x;
11      }
12      soma = x;
13      soma = soAqui;
14      return soma;
15  }
```

Há três níveis locais: corpo da função (nível 1), primeiro bloco interno
(nível 2) e segundo bloco interno (nível 3). O `x` da linha 5 oculta o `x`
global da linha 1 enquanto o bloco de nível 2 estiver aberto. A linha 13
usa `soAqui` após o fechamento do bloco onde foi declarada, gerando erro.

### Ambientes após cada declaração ou fechamento

Na folha, desenhar **caixas empilhadas** para os níveis ainda abertos.
Esta tabela é a referência para comparar com o desenho independente:

| Linha / evento | Global, nível 0 | Função, nível 1 | Bloco, nível 2 | Bloco, nível 3 |
|---|---|---|---|---|
| 1: declara `x` | `x@1` | — | — | — |
| 2: declara `exemplo` | `x@1`, `exemplo@2` | — | — | — |
| 2: entra na função e declara `p` | idem | `p@2` | — | — |
| 3: declara `soma` | idem | `p@2`, `soma@3` | — | — |
| 4: abre bloco | idem | idem | vazio | — |
| 5: declara `x` | idem | idem | `x@5` | — |
| 6: abre bloco | idem | idem | `x@5` | vazio |
| 7: declara `soAqui` | idem | idem | `x@5` | `soAqui@7` |
| 9: fecha nível 3 | idem | idem | `x@5` | removido |
| 11: fecha nível 2 | idem | `p@2`, `soma@3` | removido | removido |
| 15: fecha função | `x@1`, `exemplo@2` | removido | removido | removido |

### Resolução manual de cada ocorrência de uso

| Linha | Uso | Declaração encontrada |
|---|---|---|
| 3 | `p` | `p@2` |
| 7 | `x` | `x@5` |
| 8 | `soma` | `soma@3` |
| 8 | `soAqui` | `soAqui@7` |
| 10 | `soma` | `soma@3` |
| 10 | `x` | `x@5` |
| 12 | `soma` | `soma@3` |
| 12 | `x` | `x@1` |
| 13 | `soma` | `soma@3` |
| 13 | `soAqui` | **não declarado no ambiente atual** |
| 14 | `soma` | `soma@3` |

## 2. Campos de `Symbol` e fases consumidoras

| Campo | Significado | Para que serve |
|---|---|---|
| `name: String` | Grafia original do nome | Identificação CP4, busca por nome e erros |
| `category: Category` | Variável, constante, parâmetro, função ou procedimento | CP4 registra a origem; CP5 verificará usos e categorias |
| `declaration: Ast.Node` | Referência ao nó onde o nome foi declarado | CP4 decora os usos; CP5 consulta informações da declaração |
| `level: int` | Profundidade léxica, começando no global 0 | CP4 registra qual declaração foi encontrada; fases posteriores usam escopos |
| `type: Ast.Type` | Tipo declarado ou retorno da função, quando aplicável | Campo reservado à checagem no CP5; pode ser refinado nessa fase |
| `offset: Integer` | Deslocamento de memória ainda desconhecido (`null`) | Aulas posteriores de alocação e geração de código |

Não existe nó de declaração de constante (`ConstDecl`) no CP3 entregue.
`Category.CONSTANT` está previsto sem inventar sintaxe ou modificar a AST.

## 3. Implementação escolhida para a tabela

Escolha: **pilha de mapas** (`StackSymbolTable`), um mapa por nível. A busca
percorre do topo para o global, de forma que sombreamento é natural. Ao fechar
um escopo, o mapa do topo é descartado e as declarações anteriores reaparecem.

O construtor cria o nível global 0. `closeScope()` no global lança
`IllegalStateException` explicativa, sem remover o mapa global.
`declare()` retorna `false` sem substituir a primeira declaração; `lookup`
e `lookupLocal` retornam `null` quando não encontram o nome.

## 4. Cinco decisões de projeto

### Decisão 1 — Sombreamento e redeclaração

- **Escolha:** é permitido declarar em um bloco interno um nome que já existe no externo. Duas declarações no mesmo escopo são erro, e a primeira permanece ativa.
- **Alternativa descartada:** proibir todo sombreamento, mesmo em níveis diferentes.
- **Motivo:** escopos lexicais ficam previsíveis, e o mapa local evita substituir nomes do pai.
- **Programas:** `escopos_sombreamento.txt` (válido) e `redeclaracao.txt` (inválido).

### Decisão 2 — Declarar antes de usar

- **Escolha:** um nome só pode ser usado depois de sua declaração ter sido visitada; no inicializador, o próprio nome já foi registrado.
- **Alternativa descartada:** antecipar as declarações em uma passagem prévia.
- **Motivo:** a passada única preserva a ordem textual dos filhos e dos erros.
- **Programa:** `uso_antes_declaracao.txt` (o uso na linha 2 dá erro; o da linha 4 funciona).

### Decisão 3 — Recursão mútua

- **Escolha:** recursão **direta** é aceita; recursão **mútua com referência antecipada** não é aceita neste CP4. O nome de uma função é inserido antes de seu próprio corpo.
- **Alternativa descartada:** pré-declarar todas as funções do bloco para aceitar referências futuras.
- **Motivo:** manter a análise em uma única passada, sem antecipações invisíveis na ordem do texto.
- **Programas:** `procedimento_recursivo.txt` (válido) e `recursao_mutua.txt` (a chamada de `segundo` na linha 2 falha).

### Decisão 4 — Parâmetros e corpo

- **Escolha:** os parâmetros e o primeiro bloco do corpo da função compartilham o mesmo escopo. Uma declaração local que repete o parâmetro é erro.
- **Alternativa descartada:** abrir outro nível apenas para o primeiro bloco do corpo, deixando sombrear o parâmetro.
- **Motivo:** evita duas variáveis diferentes com o mesmo nome logo na entrada de uma função e segue a regra escolhida no grupo.
- **Programa:** `parametro_corpo.txt` (redeclaração de `a` na linha 2).

### Decisão 5 — Espaços de nomes

- **Escolha:** variáveis e funções/procedimentos compartilham um único espaço de nomes em cada escopo.
- **Alternativa descartada:** mapas diferentes para variáveis e rotinas, permitindo os mesmos nomes no mesmo nível.
- **Motivo:** permite uma única operação `lookup` e segue o exemplo de linguagem do enunciado.
- **Programa:** `espaco_nomes.txt` (a rotina `item` colide com a variável global).

## 5. Regras adicionais de navegação

1. `Program` utiliza o ambiente global que já existe.
2. `Block` comum abre um nível, percorre filhos na ordem e fecha o nível.
3. `FunctionDecl` e `ProcedureDecl` declaram o próprio nome no ambiente pai antes de entrar no corpo.
4. Parâmetros entram no nível da função; o bloco mais externo do corpo não abre outro nível, conforme Decisão 4.
5. `For` abre um nível para inicializador, condição, atualização e corpo; o bloco do corpo ainda abre seu próprio nível. A declaração do inicializador não vaza após o laço.
6. `Id.decl` aponta para o primeiro símbolo encontrado ao pesquisar do nível mais interno ao mais externo. Quando não encontrado, fica `null`.
7. `CallExpr` no CP3 possui `String name`, não um nó `Id`. O nome da chamada também é resolvido; o vínculo é guardado externamente em `IdentificationVisitor.getCallDeclarations()`, sem modificar a AST proibidamente.
8. Erros são acumulados sem interromper a visita, com ordem determinística da travessia. Eles trazem linha, nome e, em redeclarações, a linha da primeira declaração.
9. O Parser do CP3 não constrói funções aninhadas; o visitante já as suporta se aparecerem na AST. Um teste específico constrói essa AST diretamente.

## 6. Conferência dos resultados

- `test/programas/` contém os programas, inclusive todos os testes de decisão.
- `test/esperado/` contém as previsões textuais para comparação automatizada.
- `src/test/java/SymbolTableTest.java` testa a API isolada.
- `src/test/java/IdentificationVisitorTest.java` testa a passada, os vínculos `decl` e os diagnósticos.
- `src/test/java/Cp4SmokeTest.java` permite executar checagens sem JUnit.
- Os testes antigos do CP3 permanecem no projeto e devem continuar aprovados.

**Verificação independente pendente do grupo:** preencher a folha A4 com os
ambientes, os `x@linha` e os diagnósticos previstos, confrontar com os arquivos
em `test/esperado/`, corrigir divergências e guardar as fotografias em `manual/`.
