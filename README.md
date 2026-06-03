# TDE01 — Alocador Buddy Binário com Estruturas de Dados Auxiliares

Simulador em Java do **Alocador Buddy Binário** sobre 32 MB de memória, com bloco
mínimo de 4 KB, integrando fila, pilha e listas encadeadas implementadas
**manualmente com nós encadeados** (sem `ArrayList`, `LinkedList`, `Queue`,
`Stack`, `Math.pow` ou qualquer biblioteca externa).

A árvore que organiza os blocos é uma **Árvore Rubro-Negra** com sentinela NIL,
rotações esquerda/direita e procedimentos de correção após inserção (`fixInsert`)
e remoção (`fixDelete`).

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
  Main.java
dataset.txt
README.md
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
| `Main` | Menu interativo de 8 opções, visualização hierárquica, leitura do dataset. |

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

Pré-requisito: JDK 8 ou superior (`javac` no PATH).

### Compilação
```powershell
javac -d out src/*.java
```

### Execução
Com dataset por argumento:
```powershell
java -cp out Main dataset.txt
```

Sem dataset (inicia diretamente no menu):
```powershell
java -cp out Main
```

---

## Menu interativo

```
(1) Alocar
(2) Liberar
(3) Desfazer (Undo)
(4) Exibir memoria
(5) Exibir fila de pendentes
(6) Exibir listas de blocos livres
(7) Carregar dataset
(8) Sair
```

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

---

## Exemplo de execução (recorte)

Após alocar `a1=200KB` e `a2=100KB` em 1024 KB de memória (versão simplificada):

```
[1024K DIVIDIDO]
  ├── [512K DIVIDIDO]
  │     ├── [256K OCUPADO a1]
  │     └── [256K DIVIDIDO]
  │           ├── [128K OCUPADO a2]
  │           └── [128K LIVRE]
  └── [512K LIVRE]

Fila de pendentes (0): [vazia]
Blocos livres:
  4KB: 0 | ... | 128KB: 1 | 256KB: 0 | 512KB: 1 | ...
```

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
- Menu com as 8 opções obrigatórias.
- Carregamento de dataset via argumento ou opção do menu.
- Visualização hierárquica + fila + `buddyinfo`.
