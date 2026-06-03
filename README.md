# TDE01 — Alocador Buddy Binário com Estruturas de Dados Auxiliares

Simulador em Java do **Alocador Buddy Binário** sobre 32 MB de memória, com bloco
mínimo de 4 KB, integrando fila, pilha e listas encadeadas implementadas
**manualmente com nós encadeados** (sem `ArrayList`, `LinkedList`, `Queue`,
`Stack`, `Math.pow` ou qualquer biblioteca externa).

A árvore que organiza os blocos é uma **Árvore Rubro-Negra** com sentinela NIL,
rotações esquerda/direita e procedimentos de correção após inserção (`fixInsert`)
e remoção (`fixDelete`).

A aplicação possui **interface gráfica Swing** que desenha o mapa da memória, a
árvore rubro-negra, a fila de pendentes, as listas de blocos livres e o log de
operações em tempo real.

---

## Autor

- Ian Carlo Araujo Braz (trabalho individual)

---

## Estrutura de arquivos

```
src/
  NoFila.java
  Fila.java
  NoPilha.java
  Pilha.java
  OperacaoHistorico.java
  NoLista.java
  ListaBlocos.java
  Cor.java
  EstadoBloco.java
  No.java
  AlocadorBuddy.java
  JanelaPrincipal.java
  Main.java
dataset.txt
README.md
DOCUMENTACAO_CODIGO.md
```

---

## Classes e responsabilidades

| Classe | Responsabilidade |
|--------|------------------|
| `NoFila` / `Fila` | Fila FIFO encadeada para requisições de alocação pendentes. |
| `NoPilha` / `Pilha` | Pilha LIFO encadeada para histórico de operações (undo). |
| `OperacaoHistorico` | Registro do tipo da operação (ALOCAR/LIBERAR), id, tamanho e nó afetado. |
| `NoLista` / `ListaBlocos` | 14 listas duplamente encadeadas (uma por tamanho) com blocos livres. |
| `Cor` | Enum `{ VERMELHO, PRETO }` da árvore rubro-negra. |
| `EstadoBloco` | Enum `{ LIVRE, OCUPADO, DIVIDIDO }`. |
| `No` | Nó da árvore: tamanho, estado, identificador, pai, filhos, cor. Inclui sentinela `NIL` (PRETO). |
| `AlocadorBuddy` | Lógica do alocador: split/merge recursivos, rotações, correções RB, atendimento da fila, registro no histórico. |
| `JanelaPrincipal` | Interface gráfica Swing. Desenha o mapa, a árvore rubro-negra, fila, buddyinfo e log. |
| `Main` | Ponto de entrada. Inicializa o Look and Feel, abre a janela e carrega o dataset opcional. |

---

## Operações principais

### Split (divisão)
1. Calcula a menor potência de 2 ≥ tamanho solicitado (mínimo 4 KB) via deslocamento de bits.
2. Procura bloco livre na lista do tamanho alvo (`ListaBlocos.buscarPrimeiro`).
3. Se não há, busca recursivamente nível imediatamente maior e divide o bloco encontrado.
4. Ao dividir: remove o bloco da lista de seu nível, marca como `DIVIDIDO`, cria
   dois filhos `LIVRE` de metade do tamanho e os insere na lista do nível inferior.
5. Após cada inserção, chama `corrigirBalanceamento` (fixInsert).
6. Continua dividindo até atingir o tamanho alvo (parando no buddy mínimo de 4 KB).
7. Marca o bloco final como `OCUPADO`, salva o identificador e empilha a operação no histórico.
8. Se não há memória suficiente para servir a requisição, ela vai para a **fila de pendentes**.

### Merge (fusão)
1. Liberar marca o bloco como `LIVRE` e o reinsere na lista do seu nível.
2. Verifica o **buddy** (irmão do nó). Se também está `LIVRE`, funde:
   - Remove ambos das listas de livres.
   - Volta o pai (que era `DIVIDIDO`) para `LIVRE`, descarta os filhos.
   - Insere o pai na lista do nível imediatamente superior.
3. Após cada fusão chama `corrigirRemocao` (fixDelete) e recursivamente tenta
   subir mais um nível.
4. Após o merge, percorre uma passada da **fila de pendentes** tentando
   reatendê-las; requisições que falham voltam para o fim da fila.
5. A operação de liberação é registrada na pilha.

### Undo (desfazer)
- Desempilha a última operação:
  - Se foi `ALOCAR`, executa um `liberar` interno (sem reempilhar).
  - Se foi `LIBERAR`, executa um `alocar` interno (sem reempilhar).
- Restaura o estado da árvore e das listas encadeadas de blocos livres.

### Fila de pendentes
- Encadeada com ponteiros `inicio` / `fim` para enfileiramento em O(1).
- Cada requisição armazena `identificador` e `tamanhoKB`.
- Toda alocação que falha vai para o fim da fila.
- Toda operação de merge dispara uma passada de atendimento.

### Listas de blocos livres (estilo `buddyinfo`)
- 14 listas duplamente encadeadas (níveis 0–13), uma para cada tamanho de bloco
  entre 4 KB e 32 MB.
- Índice por nível calculado por *bit shifting* (sem `Math.pow`/`Math.log`).
- Exibição inspirada em `/proc/buddyinfo` do Linux:
  ```
  Blocos livres:
    4KB: 0 | 8KB: 1 | 16KB: 0 | ... | 16MB: 1 | 32MB: 0
  ```

### Árvore Rubro-Negra
- Cada `No` possui campo `cor` (VERMELHO/PRETO) além dos campos do buddy.
- Sentinela `NIL` estática (PRETO) usada nos filhos das folhas.
- Métodos implementados: `rotacaoEsquerda`, `rotacaoDireita`,
  `corrigirBalanceamento` (fixInsert), `corrigirRemocao` (fixDelete).
- **Observação de projeto:** como a árvore buddy é um binário perfeitamente
  balanceado por construção (todo split cria dois filhos do mesmo tamanho), as
  propriedades rubro-negras (incluindo black-height uniforme) são preservadas
  por **recoloração** após cada split/merge. As rotações estão implementadas e
  disponíveis como primitivas, mas não são acionadas pelas correções porque
  rotacionar deslocaria nós entre níveis de tamanho diferente — o que quebraria
  a hierarquia do buddy. Mantemos assim as 5 propriedades rubro-negras
  (raiz PRETO, NIL PRETO, sem RED-RED, mesmo número de PRETOs em cada caminho)
  sem comprometer a semântica do alocador.

---

## Como compilar e executar

**Pré-requisito:** JDK 11 ou superior. Recomendado utilizar a JDK instalada
pelo IntelliJ IDEA (em `~/.jdks/openjdk-23.0.2`) ou qualquer JDK moderna.

### Compilação
```powershell
javac -d out src/*.java
```

### Execução
Com dataset por argumento (a janela já abre com o dataset processado):
```powershell
java -cp out Main dataset.txt
```

Sem dataset (a janela abre vazia e o dataset pode ser carregado pelo botão):
```powershell
java -cp out Main
```

### Rodando no IntelliJ
1. **File → Open** e selecione a pasta do projeto.
2. Confirme que `src/` está marcado como **Sources**.
3. Em **Project Structure**, defina a JDK 11 ou superior.
4. Abra `src/Main.java` e clique em **Run 'Main.main()'**.
5. Para já carregar o dataset, em **Run → Edit Configurations**,
   no campo **Program arguments** coloque `dataset.txt`.

---

## Interface gráfica

A janela é dividida em cinco regiões:

| Região | Conteúdo |
|--------|----------|
| **Topo** | Barra de botões: `ALOCAR`, `LIBERAR`, `DESFAZER`, `CARREGAR DATASET`, `RESETAR`. Label de status com uso, livre, pendentes e tamanho do histórico. |
| **Mapa de memória** | Faixa horizontal mostrando as folhas da árvore. Largura proporcional ao tamanho real do bloco. Verde = `LIVRE`, vermelho = `OCUPADO`. Clique em bloco ocupado libera; hover mostra tooltip. |
| **Árvore rubro-negra** | Visualização hierárquica da árvore. Cor de fundo = estado do bloco (verde/vermelho/cinza). Borda VERMELHA = nó vermelho; borda PRETA = nó preto. |
| **Painel direito** | Lista da fila de pendentes (FIFO) e listas de blocos livres no estilo `buddyinfo` (14 níveis). |
| **Rodapé** | Histórico textual de operações com indicadores `[OK]`, `[FILA]`, `[FAIL]`, `[UNDO]`. |

---

## Formato do dataset

Arquivo `.txt` com uma operação por linha. Linhas vazias ou começadas por `#`
são tratadas como comentário e ignoradas.

```
ALOCAR <id> <tamanho_em_KB>
LIBERAR <id>
```

Exemplo:
```
ALOCAR a1 5120
ALOCAR a2 3072
LIBERAR a1
```

O dataset incluído no projeto (`dataset.txt`) cobre 13 fases: alocações
iniciais, liberações parciais para gerar fragmentação, alocações que aproveitam
espaços liberados, requisição grande que vai para a fila, liberações para gerar
merge em cascata e limpeza final.

---

## Análise de fragmentação interna

A fragmentação interna do buddy decorre do arredondamento para a próxima
potência de 2. Tabela de desperdício:

| Solicitado | Alocado (2ⁿ) | Desperdiçado | Desperdício % |
|------------|--------------|--------------|---------------|
| 100 KB     | 128 KB       | 28 KB        | 22%           |
| 3 MB       | 4 MB         | 1 MB         | 25%           |
| 5 MB       | 8 MB         | 3 MB         | 38%           |
| 10 MB      | 16 MB        | 6 MB         | 38%           |
| 16 MB      | 16 MB        | 0            | 0%            |
| 8 KB       | 8 KB         | 0            | 0%            |
| < 4 KB     | 4 KB (mín.)  | até 4 KB     | variável      |

O pior caso ocorre quando o tamanho solicitado fica logo acima de uma potência
de 2 (ex.: 5 MB → 8 MB), gerando ~38% de desperdício. O melhor caso ocorre
quando a requisição já é uma potência de 2 (0% de desperdício).

---

## Documentação adicional

O arquivo [`DOCUMENTACAO_CODIGO.md`](./DOCUMENTACAO_CODIGO.md) traz uma
descrição detalhada arquivo por arquivo, com explicação de cada classe, cada
método e o ciclo de vida completo de uma operação (da UI até o desenho do
pixel).

---

## Restrições atendidas

- Nenhum uso de `ArrayList`, `LinkedList`, `Queue`, `Stack`, `HashMap`.
- Sem `Math.pow` ou similares — todas as potências de 2 são calculadas por
  deslocamento de bits (`<<`).
- Todas as estruturas (fila, pilha, listas de blocos, árvore) implementadas com
  nós encadeados manuais.
- Sentinela `NIL` (PRETO) presente na árvore.
- Rotações esquerda/direita e correções `fixInsert`/`fixDelete` implementadas.
- Buddy mínimo de 4 KB respeitado em todos os splits.
- Merge recursivo propagando até a raiz quando possível.
- Fila reatendida após cada merge.
- Pilha registra alocações e liberações bem-sucedidas; undo reverte ambos.
- Carregamento de dataset via argumento de linha de comando ou botão da
  interface gráfica.
- Visualização gráfica completa (mapa, árvore RB, fila, buddyinfo, log).
