# Parte manuscrita do CP4 — roteiro para duas folhas A4

**Atenção:** os desenhos e a validação devem ser feitos pelo grupo em papel,
sem copiar automaticamente a execução do programa. O ZIP não contém fotos
fingindo ser manuscritas. Os modelos em `escopo-design.md` servem para conferir.

## Folha A4 1: desenho dos ambientes

1. No topo, escrever `CP4 — Ambientes e escopos`, o nome do grupo e a data.
2. Copiar à esquerda, com linhas numeradas, **todo** o programa de
   `test/programas/ambiente_manual.txt` (15 linhas).
3. À direita, desenhar caixas para `Nível 0 (global)`, `Nível 1 (função)`,
   `Nível 2 (bloco externo)` e `Nível 3 (bloco interno)`.
4. Após cada declaração nas linhas 1, 2, 3, 5 e 7, atualizar o nome existente
   no ambiente correspondente. O parâmetro `p` é declarado na linha 2.
5. Marcar as aberturas das linhas 2, 4 e 6. Ao fechar os blocos nas linhas 9,
   11 e 15, riscar ou marcar como removida a caixa do escopo encerrado.
6. Usar `x@1`, `x@5`, `p@2`, `soma@3`, `soAqui@7` e `exemplo@2` para
   distinguir declarações pelo número da linha.
7. Marcar com setas a declaração alcançada em **cada uso** nas linhas 3, 7,
   8, 10, 12, 13 e 14. Circular `soAqui` na linha 13: esse nome saiu do escopo.
8. Conferir só depois com a tabela do capítulo 1 de `escopo-design.md`.

**Arquivo no projeto:** fotografar ou escanear e salvar como
`manual/ambiente_A4.jpg` ou `manual/ambiente_A4.pdf`.

## Folha A4 2: validação independente das saídas

1. No topo, escrever `CP4 — Validação das resoluções e erros`.
2. Sem executar a passada, usar os programas de `test/programas/` e prever
   quais nomes são encontrados e quais erros devem aparecer.
3. Para o caso `escopos_sombreamento.txt`, mostrar pelo menos cinco relações
   `uso -> declaração@linha` e identificar o escopo (nível 0, 1 ou 2).
4. Para `redeclaracao.txt`, anotar as duas declarações, qual permanece e a
   mensagem de erro com a linha da primeira.
5. Para `nao_declarado.txt` e `fora_do_escopo.txt`, mostrar por que a busca
   falha e a mensagem esperada.
6. Para `tres_erros.txt`, prever **os três erros na ordem das linhas**.
7. Para `procedimento_recursivo.txt`, mostrar que a chamada encontra a própria
   declaração, pois o nome foi inserido antes do corpo.
8. Para as cinco decisões, registrar o resultado de cada programa em uma
   tabelinha: nome, válido/inválido, ligação ou erro e justificativa.
9. Somente depois, conferir o que foi previsto com os arquivos
   `test/esperado/*.txt` e com o resultado de `mvn test`.

**Arquivo no projeto:** fotografar ou escanear e salvar como
`manual/validacao_A4.jpg` ou `manual/validacao_A4.pdf`.

## Como entregar as fotos dentro do ZIP

1. Salvar os arquivos nas duas posições indicadas dentro desta pasta `manual/`.
2. No `escopo-design.md`, acrescentar os links Markdown, conforme a extensão:

   ```md
   ![Desenho dos ambientes](manual/ambiente_A4.jpg)
   ![Validação independente](manual/validacao_A4.jpg)
   ```

3. Se preferirem um único PDF contendo as duas folhas, salvá-lo como
   `manual/desenhos_e_validacao.pdf` e colocar um link para ele no documento.
4. Revisar se as fotos estão nítidas, com números de linha e nomes legíveis.
5. Recompactar o projeto com as fotos e executar novamente os testes.

## O que já existe para consulta

- `escopo-design.md`: conteúdo teórico, cinco decisões e modelo de ambiente.
- `test/esperado/*.txt`: previsões de saídas digitadas para testes automatizados.
- `README-CP4.md`: comandos de compilação, execução e organização final.

**Não é necessário desenhar toda a AST de novo:** isso foi o CP3. No CP4, o
desenho exigido é da **tabela de símbolos e seus ambientes**, com a validação
independente das resoluções de identificadores.
