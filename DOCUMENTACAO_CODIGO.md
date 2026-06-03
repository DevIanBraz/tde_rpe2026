# Documentação Técnica — Alocador Buddy Binário

Explicação detalhada de **cada arquivo**, **cada classe** e **cada método** do projeto. Útil para estudar, defender o trabalho e responder na prova de autoria.

---

## 1. Visão geral da arquitetura

O sistema tem **5 camadas** que se empilham:

```
┌────────────────────────────────────────────────────────────┐
│  CAMADA 5  Interface gráfica (Swing)                       │
│            Main.java  →  JanelaPrincipal.java              │
├────────────────────────────────────────────────────────────┤
│  CAMADA 4  Alocador Buddy Binário (núcleo)                 │
│            AlocadorBuddy.java                              │
├────────────────────────────────────────────────────────────┤
│  CAMADA 3  Árvore rubro-negra                              │
│            No.java                                         │
├────────────────────────────────────────────────────────────┤
│  CAMADA 2  Estruturas lineares (RA1)                       │
│            Fila + Pilha + ListaBlocos                      │
├────────────────────────────────────────────────────────────┤
│  CAMADA 1  Tipos básicos (enums)                           │
│            Cor.java   +   EstadoBloco.java                 │
└────────────────────────────────────────────────────────────┘
```

A regra de ouro: **toda camada superior usa as classes da camada inferior, nunca o contrário.** O `AlocadorBuddy` é o "cérebro" — ele orquestra a árvore, a fila, a pilha e as listas. A interface gráfica só lê estado e chama métodos do alocador, sem nunca mexer em ponteiros.

---

## 2. Camada 1 — Enums (tipos discretos)

### 📄 `Cor.java`

```java
public enum Cor { VERMELHO, PRETO }
```

Representa a cor de cada nó da árvore rubro-negra. Toda árvore RB tem exatamente duas cores; um nó é sempre uma das duas.

- **VERMELHO** → nó "novo", recém-inserido por um split.
- **PRETO** → nó "estável", garantia das propriedades RB.

Não há lógica aqui — é só um rótulo para manter as 5 propriedades rubro-negras (a raiz é preta, todo NIL é preto, vermelho não tem filho vermelho, etc.).

---

### 📄 `EstadoBloco.java`

```java
public enum EstadoBloco { LIVRE, OCUPADO, DIVIDIDO }
```

Representa o estado físico de um bloco de memória na árvore buddy.

| Estado | Significado | Tem filhos? | Aparece no mapa? |
|---|---|---|---|
| **LIVRE** | Disponível para alocação | Não (é folha) | Sim, em verde |
| **OCUPADO** | Foi alocado para algum `identificador` | Não (é folha) | Sim, em vermelho |
| **DIVIDIDO** | Foi quebrado em dois buddies | Sim (filhos esq/dir) | Não (é nó intermediário) |

Esses três estados são **mutuamente exclusivos**: um bloco está em exatamente um deles a cada instante.

---

## 3. Camada 2 — Estruturas Lineares (RA1)

Aqui mora o coração da nota de **RA1 (3,0 pontos)**. Tudo é construído **só com nós encadeados** — nenhuma classe pronta da biblioteca Java.

### 3.1. Fila de pendentes (FIFO)

#### 📄 `NoFila.java`

```java
public class NoFila {
    public String identificador;
    public int tamanhoKB;
    public NoFila proximo;
}
```

Nó singular da fila. Guarda:
- `identificador` — o nome do bloco que tentou alocar
- `tamanhoKB` — o tamanho solicitado original (antes de arredondar pra potência de 2)
- `proximo` — referência ao próximo nó da fila (encadeamento simples, só "pra frente")

---

#### 📄 `Fila.java`

Implementação manual de fila com ponteiros para `inicio` e `fim`, mais um contador `qtd`.

| Método | O que faz |
|---|---|
| `enfileirar(id, kb)` (`Fila.java:12`) | Cria um `NoFila` e anexa no **fim** da fila. Se a fila estava vazia, ele vira `inicio` e `fim` ao mesmo tempo. |
| `desenfileirar()` (`Fila.java:24`) | Remove e retorna o **início** da fila. Atualiza `inicio` para o próximo nó. Se ficar vazia, zera `fim` também. |
| `espiar()` / `primeiro()` (`Fila.java:38`, `:50`) | Retorna o nó do início **sem remover**. Útil pra GUI iterar mostrando todos os pendentes. |
| `estaVazia()` (`Fila.java:42`) | Retorna `true` se não há nenhum nó. |
| `tamanho()` (`Fila.java:46`) | Retorna `qtd`, o contador interno. |

**Por que essa fila existe:** quando você tenta alocar e não tem espaço, a requisição é **enfileirada**. Depois de cada `liberar()` bem-sucedido, o alocador chama `atenderPendentes()` que tenta alocar de novo, na mesma ordem (FIFO).

---

### 3.2. Pilha de histórico (UNDO)

#### 📄 `OperacaoHistorico.java`

```java
public static final int ALOCAR = 1;
public static final int LIBERAR = 2;

public int tipo;
public String identificador;
public int tamanhoKB;
public No noAfetado;
```

Registro de uma operação para poder desfazê-la depois. Cada empilhamento guarda:
- `tipo` → constante `ALOCAR` ou `LIBERAR`
- `identificador` → nome do bloco
- `tamanhoKB` → tamanho que foi solicitado
- `noAfetado` → referência direta ao nó da árvore que sofreu a operação

O método `tipoComoString()` (`OperacaoHistorico.java:17`) converte a constante numérica em texto, útil para logs e debug.

---

#### 📄 `NoPilha.java`

```java
public class NoPilha {
    public OperacaoHistorico operacao;
    public NoPilha proximo;
}
```

Nó da pilha. Guarda a operação registrada e um único ponteiro `proximo` (pilha é unidirecional).

---

#### 📄 `Pilha.java`

Pilha LIFO com ponteiro único para o `topo` e contador `qtd`.

| Método | O que faz |
|---|---|
| `empilhar(op)` (`Pilha.java:10`) | Cria um `NoPilha` e coloca **no topo**. O `proximo` do novo nó aponta para o antigo topo. |
| `desempilhar()` (`Pilha.java:17`) | Retira o nó do topo e retorna sua `OperacaoHistorico`. Avança `topo` para `proximo`. |
| `topo()` (`Pilha.java:27`) | Retorna a operação do topo **sem remover** (peek). |
| `estaVazia()` (`Pilha.java:34`) | `true` se não há nenhuma operação registrada. |
| `tamanho()` (`Pilha.java:38`) | Quantidade total empilhada. |

**Por que LIFO:** undo precisa desfazer a **última** operação primeiro, depois a penúltima, etc. — exatamente o comportamento de uma pilha.

---

### 3.3. Listas de blocos livres (buddyinfo)

#### 📄 `NoLista.java`

```java
public class NoLista {
    public No bloco;
    public NoLista proximo;
    public NoLista anterior;
}
```

Nó de **lista duplamente encadeada**. O `bloco` referencia um `No` da árvore (não duplica dados, só aponta). Tem `proximo` e `anterior` porque essa lista precisa remover do meio rapidamente, então ter o "anterior" evita ter que percorrer tudo de novo.

---

#### 📄 `ListaBlocos.java`

Esta é a estrutura mais sofisticada do RA1. Mantém **14 listas independentes**, uma para cada tamanho possível de bloco no buddy.

```java
public static final int NIVEIS = 14;            // 4KB, 8KB, ..., 32MB
public static final int TAMANHO_MINIMO_KB = 4;
public static final int TAMANHO_MAXIMO_KB = 32768;
```

| Nível | Tamanho do bloco |
|---|---|
| 0 | 4 KB |
| 1 | 8 KB |
| 2 | 16 KB |
| ... | ... |
| 13 | 32 MB |

Por dentro:
```java
private NoLista[] inicios;   // ponteiro para o início de cada uma das 14 listas
private int[] tamanhos;      // contador da quantidade de cada nível
```

#### Métodos

| Método | O que faz |
|---|---|
| `nivelDe(kb)` (`ListaBlocos.java:18`) | **Calcula o nível (0–13) sem usar `Math.log` nem `Math.pow`.** Divide `kb` por 4 e conta quantos shifts à direita são necessários para chegar em 1. Ex.: `nivelDe(16) → 16/4 = 4 → 4 >> 2 = 1 → nível 2`. |
| `tamanhoDoNivel(n)` (`ListaBlocos.java:28`) | Inverso do anterior: `4 << n`. Devolve o tamanho em KB daquele nível. |
| `inserir(bloco)` (`ListaBlocos.java:32`) | Calcula o nível do bloco, cria um `NoLista` e enxerta **no início** da lista correspondente (inserção em O(1)). |
| `remover(bloco)` (`ListaBlocos.java:43`) | Procura o `NoLista` que aponta para esse `bloco` na lista correta e o desconecta. O fato de ser duplamente encadeada permite a remoção sem percorrer a lista inteira a partir do início. |
| `buscarPrimeiro(kb)` (`ListaBlocos.java:63`) | Retorna o primeiro `No` da árvore disponível naquele tamanho, ou `null`. Usado pelo alocador para pegar "qualquer bloco livre" rapidamente. |
| `estaVazia(kb)` (`ListaBlocos.java:71`) | Verifica se a lista do nível está vazia. |
| `tamanho(kb)` / `contagemNivel(n)` (`ListaBlocos.java:76`, `:81`) | Quantidade de blocos livres num determinado tamanho. |
| `formatarBuddyinfo()` (`ListaBlocos.java:85`) | Gera a string `4KB: 0 | 8KB: 1 | …` — o famoso "buddyinfo" do Linux. |

**Por que 14 listas separadas e não uma só:** porque o alocador precisa **achar instantaneamente** um bloco livre de tamanho X. Com listas separadas, é O(1). Se fosse uma lista única, seria O(n) para buscar.

---

## 4. Camada 3 — Árvore Rubro-Negra

### 📄 `No.java`

```java
public class No {
    public static final No NIL = criarSentinela();

    public int tamanho;            // em KB
    public EstadoBloco estado;     // LIVRE | OCUPADO | DIVIDIDO
    public String identificador;   // null se LIVRE/DIVIDIDO
    public No filhoEsquerdo;       // sempre != null (aponta pra NIL se folha)
    public No filhoDireito;        // idem
    public No pai;                 // null só na raiz
    public Cor cor;                // VERMELHO | PRETO
}
```

Este é o nó da árvore buddy + RB ao mesmo tempo. Cada bloco da memória é representado por um nó.

#### O sentinela `NIL`

```java
public static final No NIL = criarSentinela();
```

`NIL` é o "nó folha sentinela" obrigatório em árvores rubro-negras. É um **único objeto compartilhado** com:
- `tamanho = 0`
- `cor = PRETO` (regra RB: todo NIL é preto)
- `filhoEsquerdo = filhoDireito = pai = null`

Em vez de usar `null` para "filho não existe", a árvore usa `NIL`. Isso simplifica os algoritmos: você nunca precisa checar `null` antes de acessar `.cor` ou `.filhoEsquerdo` — tudo funciona, basta verificar com `ehNil()`.

#### Métodos

| Método | O que faz |
|---|---|
| `No(tamanho, estado, pai)` (`No.java:12`) | Construtor normal. Cria um nó **VERMELHO** com filhos NIL. (Inserir vermelho é o padrão RB; só vai virar preto se a correção mandar.) |
| `criarSentinela()` (`No.java:32`) | Construtor privado que monta o único `NIL`. |
| `ehNil()` (`No.java:36`) | `true` se o nó é o sentinela. Usado pelo alocador em todo lugar pra parar a recursão. |

**Por que pai é guardado:** porque o merge precisa subir na árvore (do filho ao pai ao avô…) para fundir buddies. Sem `pai`, seria preciso buscar do topo toda hora.

---

## 5. Camada 4 — Alocador (RA2, núcleo do trabalho)

### 📄 `AlocadorBuddy.java`

O coração do trabalho. Aqui mora a lógica de **split, merge, undo, rotação RB e correção RB**.

#### Constantes

```java
public static final int MEMORIA_TOTAL_KB = 32 * 1024;  // 32 MB
public static final int MINIMO_KB        = 4;          // buddy mínimo
```

#### Campos

```java
private No raiz;                  // raiz da árvore RB (sempre PRETO)
private ListaBlocos listas;       // 14 listas de blocos livres
private Fila pendentes;           // requisições que falharam
private Pilha historico;          // operações para undo
private boolean registrarHistorico;  // flag interna para evitar undo de undo
```

#### Construtor (`AlocadorBuddy.java:11`)

Inicializa tudo: cria a raiz como **um único bloco LIVRE de 32 MB** (preto), insere essa raiz na `ListaBlocos` no nível 13 (32 MB), e cria fila/pilha vazias.

`registrarHistorico = true` por padrão — significa que cada operação será empilhada. Vira `false` temporariamente quando o próprio undo está executando, para não criar loops.

---

#### Operação `alocar(id, kb)` (`AlocadorBuddy.java:34`)

Tenta alocar um bloco para um identificador. **Sequência completa:**

1. **Validações iniciais:**
   - Se `kb <= 0` ou `kb > 32 MB` → retorna `false` sem fazer nada.
   - Se já existe um bloco com esse identificador → retorna `false` (não deixa duplicar).

2. **Calcula tamanho-alvo:** chama `potenciaDeDoisMaiorOuIgual(kb)` que arredonda para a próxima potência de 2 ≥ 4. Ex.: `5120 KB` → `8192 KB` (8 MB).

3. **Tenta obter um bloco livre** desse tamanho via `obterBlocoLivre()`.

4. **Se falhou** (não tem espaço): enfileira na `pendentes` e retorna `false`.

5. **Se conseguiu:**
   - Marca o bloco como `OCUPADO`
   - Grava o `identificador` no nó
   - Remove o bloco da `ListaBlocos` (não é mais livre)
   - Empilha a operação no `historico` (se a flag permitir)
   - Retorna `true`

---

#### `potenciaDeDoisMaiorOuIgual(kb)` (`AlocadorBuddy.java:26`)

```java
int alvo = MINIMO_KB;     // 4
while (alvo < kb) alvo = alvo << 1;
return alvo;
```

**Sem usar `Math.pow`.** Começa em 4 e dobra (shift à esquerda) até passar de `kb`. Para `kb = 100` retorna 128. Para `kb = 5120` retorna 8192. Para `kb = 1` retorna 4 (mínimo).

---

#### `obterBlocoLivre(alvo)` (`AlocadorBuddy.java:57`) — **recursão fundamental**

Procura um bloco livre exatamente do tamanho `alvo`. Se não existe:
1. Pede recursivamente um bloco **do dobro do tamanho**.
2. Divide esse bloco em dois buddies.
3. Retorna um dos buddies (do tamanho certo).

Pseudo-fluxo:
```
livre = listas.buscarPrimeiro(alvo)
se livre existe → retorna ele
senão → maior = obterBlocoLivre(alvo * 2)
        se maior == null → null (sem espaço)
        senão → return dividir(maior, alvo)
```

Esta é a recursão que faz o alocador **descer ao tamanho certo** começando do topo. Se o sistema só tem 32 MB livres e você pede 4 KB, ele recursivamente divide 32→16→8→...→8 KB→4 KB.

---

#### `dividir(bloco, alvo)` (`AlocadorBuddy.java:73`) — **o SPLIT**

Divide um bloco em dois buddies do mesmo tamanho:

1. Remove o bloco da `ListaBlocos` (não é mais "livre nesse nível").
2. Marca como `DIVIDIDO`.
3. Cria dois nós filhos (esquerdo e direito), cada um com **metade** do tamanho original.
4. Insere **ambos** na `ListaBlocos` (no nível de baixo).
5. **Chama `corrigirBalanceamento()` em cada filho** — eles foram inseridos vermelhos, agora precisa garantir as propriedades RB.
6. **Se ainda não chegou no tamanho desejado**, recursivamente divide o filho esquerdo.
7. Retorna o nó final que tem o tamanho exato.

**Detalhe sutil:** o split sempre **divide o filho esquerdo** quando precisa descer mais um nível. Isso garante determinismo (mesma entrada → mesma estrutura) e mantém o filho direito como buddy livre.

---

#### `liberar(id)` (`AlocadorBuddy.java:95`)

1. Procura o nó com aquele identificador via `encontrarPorId()`.
2. Se não encontra → retorna `false`.
3. **Marca o nó como LIVRE**, zera o identificador.
4. **Reinsere o bloco na `ListaBlocos`**.
5. Chama `fundir(bloco)` — pode mesclar com o buddy se ele também estiver livre.
6. Empilha a operação no `historico`.
7. **Chama `atenderPendentes()`** — agora que liberou espaço, talvez algum da fila possa ser atendido.

---

#### `fundir(bloco)` (`AlocadorBuddy.java:117`) — **o MERGE**

Recursivamente sobe na árvore tentando mesclar buddies livres.

```java
private No fundir(No bloco) {
    No pai = bloco.pai;
    se pai é null ou NIL → bloco                  // já é raiz, para
    buddy = irmão do bloco
    se buddy.estado != LIVRE → bloco              // buddy não está livre, para
    
    listas.remover(bloco)
    listas.remover(buddy)
    pai.filhoEsquerdo = NIL
    pai.filhoDireito = NIL
    pai.estado = LIVRE
    corrigirRemocao(pai)
    listas.inserir(pai)
    
    return fundir(pai)  // recursão: tenta mesclar o pai com SEU buddy
}
```

**O que isso significa:**
- Os dois buddies "desaparecem" (são esquecidos pela árvore)
- O pai, que era DIVIDIDO, vira LIVRE
- O pai entra na `ListaBlocos` no nível dele (maior)
- Tenta-se mesclar o pai com o tio do bloco original

Isso é o que faz a árvore **encolher de volta** quando você libera tudo: cascata de merges propagando até a raiz.

**Por isso o `img02` deixado para trás trava tudo:** o caminho de merge dele até a raiz fica bloqueado em cada nível porque o buddy do caminho está livre mas o outro lado está ocupado.

---

#### `atenderPendentes()` (`AlocadorBuddy.java:141`)

Depois de cada liberação bem-sucedida, percorre a fila tentando alocar de novo. Cada item desenfileirado vira uma chamada a `alocar()`:
- Se conseguir, o item sai da fila e fica ocupado.
- Se não conseguir, `alocar()` enfileira de volta automaticamente, **mas no final da fila**.

A iteração é limitada à quantidade inicial da fila (`qtd`) para evitar loop infinito — se ninguém da fila conseguir alocar, o loop ainda termina.

---

#### `desfazer()` (`AlocadorBuddy.java:150`) — **UNDO**

1. Desempilha a última `OperacaoHistorico`.
2. **Desliga `registrarHistorico`** para que o undo não registre o próprio undo (criando loop).
3. Se era um `ALOCAR` → chama `liberar()`.
4. Se era um `LIBERAR` → chama `alocar()`.
5. **Religa `registrarHistorico`** no `finally` (garantia de que sempre volta).

Note o `try/finally`: mesmo se o `alocar` lançar exceção, a flag volta a true.

---

#### `encontrarPorId(no, id)` (`AlocadorBuddy.java:168`)

Busca recursiva em profundidade. Desce a árvore procurando um nó **OCUPADO** com aquele identificador. Para na primeira folha que bater. Usado pela função `liberar()`.

---

#### Rotações RB

##### `rotacaoEsquerda(x)` (`AlocadorBuddy.java:185`)
##### `rotacaoDireita(x)` (`AlocadorBuddy.java:206`)

Implementação clássica do CLRS (Cormen). Pivoteia em torno de `x`, sobe o filho do lado oposto para a posição de `x`, e religa todos os ponteiros (`pai`, `filhoEsquerdo`, `filhoDireito`) corretamente. Inclui o caso especial de quando `x` era a raiz (atualiza `this.raiz`).

**No buddy NÃO são chamadas durante split/merge** (veja comentário no código): porque a árvore buddy já é **perfeitamente balanceada por construção** (split sempre cria dois filhos do mesmo tamanho). As rotações estão lá como **primitivas** porque o requisito do TDE pede que existam — e poderiam ser usadas em uma extensão real, mas balancear via rotação no buddy quebraria a relação "pai = soma dos filhos".

---

#### `corrigirBalanceamento(z)` (`AlocadorBuddy.java:234`) — **fixInsert**

Chamada após cada split. Sobe da inserção em direção à raiz **recolorindo nós** para manter as 5 propriedades RB:

```
enquanto z não é raiz E pai de z é VERMELHO:
    avo = pai do pai
    tio = irmão do pai de z
    se tio é VERMELHO:
        // Caso 1: recoloração
        pai e tio viram PRETO, avo vira VERMELHO
        z = avo (sobe e continua)
    senão:
        // Caso 2/3: só pinta o pai de preto e para
        pai de z vira PRETO
        break
sempre: raiz é PRETO
```

Como a árvore buddy nunca sofre rotação real, todos os casos do RB clássico que exigiriam rotação são resolvidos por **mera recoloração** — e o resultado fica RB-válido porque a árvore já é estruturalmente balanceada.

---

#### `corrigirRemocao(x)` (`AlocadorBuddy.java:265`) — **fixDelete**

Chamada após cada merge. O nó que era DIVIDIDO virou folha LIVRE. Sobe ajustando cores:

```
enquanto atual não é raiz E atual é PRETO E pai existe:
    irmao = irmão de atual
    se irmao é VERMELHO:
        irmao vira PRETO, pai vira VERMELHO
        break
    atual = pai (sobe)
sempre: raiz é PRETO
```

Mesmo princípio: mantém as invariantes RB com recoloração.

---

#### Getters (`AlocadorBuddy.java:21`–`:24`)

`getRaiz()`, `getListas()`, `getPendentes()`, `getHistorico()` — usados pela GUI para **ler** o estado sem modificar nada.

---

## 6. Camada 5 — Interface Gráfica (Swing)

### 📄 `Main.java`

Ponto de entrada do programa. Faz três coisas:

1. **Define o look-and-feel do sistema** (`UIManager.setSystemLookAndFeelClassName`) — usa o tema nativo do Windows em vez do Metal antigo da Java.

2. **Lê argumento opcional**: se foi passado um caminho de arquivo na linha de comando, guarda para carregar como dataset depois.

3. **Inicia a janela na EDT** (Event Dispatch Thread) via `SwingUtilities.invokeLater()` — boa prática de Swing: toda manipulação de UI deve rodar nessa thread específica, nunca na main.

Se tem dataset, chama `janela.carregarDataset(dataset)` logo depois de exibir a janela.

---

### 📄 `JanelaPrincipal.java`

Janela principal. Estende `JFrame`. Toda a UI vive aqui (com dois inner painéis para mapa e árvore).

#### Campos da janela

```java
private AlocadorBuddy alocador;             // a instância única do alocador
private PainelMapa painelMapa;              // o mapa horizontal de memória
private PainelArvore painelArvore;          // a árvore RB desenhada
private DefaultListModel<String> modeloFila;       // dados da JList da fila
private DefaultListModel<String> modeloBuddyinfo;  // dados da JList do buddyinfo
private JTextArea areaLog;                  // textarea do histórico
private JLabel rotuloStatus;                // label de status no topo
```

#### Construtor

Cria o alocador, monta a UI chamando `construirUI()`, chama `atualizarTudo()` para mostrar o estado inicial e centraliza a janela.

#### `construirUI()`

Monta o layout em camadas:

| Região | Conteúdo |
|---|---|
| `BorderLayout.NORTH` | `topo`: barra escura com 5 botões + label de status branco |
| `BorderLayout.CENTER` | `JSplitPane` horizontal: à esquerda os painéis de mapa/árvore (split vertical), à direita fila + buddyinfo |
| `BorderLayout.SOUTH` | `JScrollPane` com o `JTextArea` do log |

Cada `JScrollPane` envolve um componente para permitir rolagem se ele crescer.

#### `botao(texto, fundo, hover)`

Fábrica de botões padronizados. Cada botão tem:
- Cor de fundo original e cor de hover diferentes
- Fonte negrito 13pt
- Borda colorida (versão escura do fundo) que vira branca no hover
- Cursor de mãozinha
- `MouseListener` que troca cor e borda ao entrar/sair

---

#### Métodos de ação

| Método | O que faz |
|---|---|
| `acaoAlocar()` (`JanelaPrincipal.java:118`) | Mostra um `JOptionPane` com 2 campos (id e KB), valida, chama `alocador.alocar()`, escreve no log e atualiza tudo. |
| `acaoLiberar()` (`JanelaPrincipal.java:135`) | Pede o ID via `showInputDialog` e chama `liberarPorId()`. |
| `liberarPorId(id)` (`JanelaPrincipal.java:144`) | Chama `alocador.liberar()`, loga o resultado e atualiza a UI. |
| `acaoDesfazer()` (`JanelaPrincipal.java:150`) | Chama `alocador.desfazer()`, loga e atualiza. |
| `acaoCarregar()` (`JanelaPrincipal.java:156`) | Abre `JFileChooser`, passa o arquivo escolhido para `carregarDataset()`. |
| `carregarDataset(arquivo)` (`JanelaPrincipal.java:163`) | Lê o arquivo linha a linha. Ignora `#` (comentário) e linhas vazias. Cada `ALOCAR id kb` chama `alocador.alocar()`, cada `LIBERAR id` chama `alocador.liberar()`. Loga cada resultado. |
| `acaoResetar()` (`JanelaPrincipal.java:194`) | Pede confirmação, recria o `AlocadorBuddy` do zero, limpa o log. |

---

#### `atualizarTudo()` — o método-chave da UI

Chamado **depois de cada operação** para sincronizar a tela com o estado do alocador.

1. **Repinta os dois painéis gráficos** (mapa + árvore).
2. **Refaz a fila visual:** itera `alocador.getPendentes().primeiro()` e seus `proximo` em sequência. Cada item vira uma linha no `modeloFila`.
3. **Refaz o buddyinfo visual:** loop de 0 a 13 chamando `contagemNivel(i)`. Cada linha mostra `tamanho : qtd  =====` (barra proporcional).
4. **Recalcula uso total:** percorre a árvore via `calcularUsado()` somando tamanho de todos os nós OCUPADO.
5. **Atualiza o label de status** com Usado/Livre/Pendentes/Histórico.

#### `calcularUsado(no)`

Recursão simples: se o nó é OCUPADO retorna seu tamanho; se DIVIDIDO retorna a soma dos filhos. Folhas LIVRES contam zero.

#### `formatarKB(kb)`

Converte KB em texto legível: `512 KB`, `8 MB`, `1.5 MB`. Apenas formatação.

---

### Inner class `PainelMapa`

Subclasse de `JPanel` que **desenha o mapa horizontal de memória**.

#### Campos
```java
private int areaX, areaY, areaW, areaH;  // área útil do painel (sem margem)
```

Guardados para que o `MouseListener` possa usar as mesmas coordenadas da pintura ao detectar cliques.

#### Construtor
- Registra como receptor de tooltips.
- Adiciona um `MouseAdapter` que detecta clique e abre confirmação para liberar.

#### `getToolTipText(e)`
Quando o mouse passa em cima, chama `blocoEm(x, y)` para descobrir qual nó está naquela coordenada e gera uma string com estado/tamanho/id/cor.

#### `blocoEm(x, y)` → `achar(no, x0, y0, w0, h0, mx, my)`
Recursão geométrica: desce pela árvore dividindo a área visual ao meio em cada nó DIVIDIDO, espelhando exatamente como o desenho foi feito.

#### `paintComponent(g)`
Onde a mágica acontece. Limpa a área, calcula a região útil descontando margem, chama `desenharFolhas()` para pintar tudo, depois desenha uma borda externa.

#### `desenharFolhas(g, no, x, y, w, h)`
Recursão que **só pinta as folhas** (LIVRE e OCUPADO):
- Se o nó é **DIVIDIDO**: divide a largura ao meio e chama recursivamente para os dois filhos. Não pinta nada nesse nível.
- Se o nó é **LIVRE**: pinta de verde com borda branca.
- Se o nó é **OCUPADO**: pinta de vermelho.
- Depois escreve o texto centralizado (`identificador  tamanho` ou só `tamanho`), se couber.

**Resultado visual:** o mapa mostra a "geografia" real dos blocos, com larguras proporcionais aos tamanhos.

#### Métodos da interface `Scrollable`
Garantem que o painel ocupe sempre a largura do viewport do JScrollPane (não cria barra horizontal desnecessária).

---

### Inner class `PainelArvore`

Mesma estrutura do `PainelMapa`, mas desenha **a árvore inteira**, incluindo os nós DIVIDIDO (em cinza).

#### `recalcular()`
Calcula a profundidade máxima da árvore e ajusta a altura preferida do painel (`profundidade + 1) * 56`). Quando você aloca muito, a árvore fica mais alta e o `JScrollPane` ganha barra vertical.

#### `paintComponent(g)`
Calcula a área útil e chama `desenharNo()` na raiz.

#### `desenharNo(g, no, x, y, w, alturaNivel)`
Recursão que **pinta cada nó** com:
- **Cor de fundo**: vermelho/verde/cinza conforme estado.
- **Borda**: vermelha grossa se o nó RB é VERMELHO, preta fina se é PRETO. Isso é o que torna a árvore rubro-negra **visível**.
- **Texto centralizado**: tamanho + estado + (id se ocupado).
- **Linhas para os filhos**: desenha duas linhas saindo da base do nó até o centro de onde cada filho será desenhado.
- **Chamada recursiva** para esquerdo (na metade esquerda da largura) e direito (metade direita).

#### `acharNo()`
Versão geométrica do "que nó está em (x, y)?", usada por tooltip e clique.

---

## 7. Ciclo de vida de uma operação (exemplo completo)

Vamos seguir o que acontece quando você clica em **ALOCAR** com `id="teste"` e `kb=5120`:

1. `acaoAlocar()` abre o diálogo, captura os valores.
2. Chama `alocador.alocar("teste", 5120)`.
3. Em `alocar()`:
   - Validações passam.
   - `potenciaDeDoisMaiorOuIgual(5120)` retorna `8192` (8 MB).
   - `obterBlocoLivre(8192)` é chamado.
4. Em `obterBlocoLivre`:
   - `listas.buscarPrimeiro(8192)` retorna `null` (não tem 8 MB livre direto).
   - Chama recursivamente `obterBlocoLivre(16384)`.
   - Eventualmente acha um bloco de 32 MB e começa a descer dividindo.
5. `dividir(32MB, 8192)`:
   - Quebra 32 MB → 16 MB + 16 MB. Insere ambos nas listas. Corrige RB.
   - Como 16 MB ≠ 8 MB, chama `dividir(16MB, 8192)`.
   - Quebra 16 MB → 8 MB + 8 MB. Insere. Corrige RB.
   - 8 MB == 8 MB → retorna o filho esquerdo.
6. De volta em `alocar()`: marca o bloco como OCUPADO, grava `identificador="teste"`, remove da lista de 8 MB, empilha operação no histórico.
7. Retorna `true` para a UI.
8. `acaoAlocar()` loga `[OK] ALOCAR teste 5120 KB`.
9. Chama `atualizarTudo()`:
   - `painelMapa.repaint()` → o mapa redesenha mostrando o bloco vermelho de 8 MB.
   - `painelArvore.repaint()` → a árvore mostra os novos nós DIVIDIDO + OCUPADO + LIVRE.
   - `modeloBuddyinfo` se atualiza para refletir as novas contagens das listas.
   - Label de status mostra "Usado: 8 MB / Livre: 24 MB".

---

## 8. Tabela rápida de "onde encontrar o quê"

| Tema | Arquivo / método |
|---|---|
| Cálculo de potência de 2 sem `Math.pow` | `AlocadorBuddy.potenciaDeDoisMaiorOuIgual()` |
| Cálculo de nível sem `Math.log` | `ListaBlocos.nivelDe()` |
| Split recursivo | `AlocadorBuddy.dividir()` |
| Merge recursivo (cascata) | `AlocadorBuddy.fundir()` |
| Fila FIFO manual | `Fila.java` + `NoFila.java` |
| Pilha LIFO manual | `Pilha.java` + `NoPilha.java` |
| Lista dupl. encadeada manual | `ListaBlocos.java` + `NoLista.java` |
| Sentinela NIL da RB | `No.NIL` |
| Rotações RB | `AlocadorBuddy.rotacaoEsquerda/Direita()` |
| Correção pós-insert (split) | `AlocadorBuddy.corrigirBalanceamento()` |
| Correção pós-delete (merge) | `AlocadorBuddy.corrigirRemocao()` |
| Tentativa de atender fila após liberação | `AlocadorBuddy.atenderPendentes()` |
| Undo | `AlocadorBuddy.desfazer()` |
| Mapa visual da memória | `JanelaPrincipal.PainelMapa.desenharFolhas()` |
| Árvore visual com cores RB | `JanelaPrincipal.PainelArvore.desenharNo()` |
| Atualização da UI inteira | `JanelaPrincipal.atualizarTudo()` |

---

## 9. Restrições atendidas

Este código **NÃO usa**:
- `ArrayList`, `LinkedList`, `Queue`, `Stack`, `Map` (proibido pelo enunciado).
- `Math.pow`, `Math.log` (proibido pelo enunciado).
- Bibliotecas de terceiros.

Toda a lógica do alocador, fila, pilha e listas usa **nós encadeados manuais**.

Os componentes Swing (`JFrame`, `JPanel`, `JButton`, etc.) são usados **apenas** para a interface gráfica, conforme autorizado pelo professor.
