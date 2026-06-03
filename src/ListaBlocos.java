// Conjunto de 14 listas duplamente encadeadas, uma para cada tamanho
// de bloco possivel no buddy (4 KB, 8 KB, 16 KB, ..., 32 MB).
// Permite ao alocador buscar um bloco livre de um tamanho especifico em O(1).
public class ListaBlocos {
    public static final int NIVEIS = 14;             // log2(32768 / 4) + 1
    public static final int TAMANHO_MINIMO_KB = 4;
    public static final int TAMANHO_MAXIMO_KB = 32768;

    private NoLista[] inicios;   // primeiro no de cada uma das 14 listas
    private int[] tamanhos;      // quantidade de blocos em cada lista

    public ListaBlocos() {
        this.inicios = new NoLista[NIVEIS];
        this.tamanhos = new int[NIVEIS];
        for (int i = 0; i < NIVEIS; i++) {
            this.inicios[i] = null;
            this.tamanhos[i] = 0;
        }
    }

    // Calcula o nivel (0-13) correspondente a um tamanho em KB.
    // Sem usar Math.log: divide por 4 e conta os shifts ate chegar em 1.
    public static int nivelDe(int tamanhoKB) {
        int n = tamanhoKB / TAMANHO_MINIMO_KB;
        int nivel = 0;
        while (n > 1) {
            n = n >> 1;
            nivel++;
        }
        return nivel;
    }

    // Inverso de nivelDe: 4 << n. Sem usar Math.pow.
    public static int tamanhoDoNivel(int nivel) {
        return TAMANHO_MINIMO_KB << nivel;
    }

    // Insere o bloco no inicio da lista do nivel correto em O(1).
    public void inserir(No bloco) {
        int nivel = nivelDe(bloco.tamanho);
        NoLista novo = new NoLista(bloco);
        novo.proximo = inicios[nivel];
        if (inicios[nivel] != null) {
            inicios[nivel].anterior = novo;
        }
        inicios[nivel] = novo;
        tamanhos[nivel]++;
    }

    // Remove o bloco da lista correta. Como e duplamente encadeada,
    // a remocao do no e O(1) depois de localiza-lo na lista.
    public void remover(No bloco) {
        int nivel = nivelDe(bloco.tamanho);
        NoLista atual = inicios[nivel];
        while (atual != null) {
            if (atual.bloco == bloco) {
                if (atual.anterior != null) {
                    atual.anterior.proximo = atual.proximo;
                } else {
                    inicios[nivel] = atual.proximo;
                }
                if (atual.proximo != null) {
                    atual.proximo.anterior = atual.anterior;
                }
                tamanhos[nivel]--;
                return;
            }
            atual = atual.proximo;
        }
    }

    // Retorna qualquer bloco livre desse tamanho (o do inicio da lista) ou null.
    public No buscarPrimeiro(int tamanhoKB) {
        int nivel = nivelDe(tamanhoKB);
        if (inicios[nivel] == null) {
            return null;
        }
        return inicios[nivel].bloco;
    }

    public boolean estaVazia(int tamanhoKB) {
        int nivel = nivelDe(tamanhoKB);
        return inicios[nivel] == null;
    }

    public int tamanho(int tamanhoKB) {
        int nivel = nivelDe(tamanhoKB);
        return tamanhos[nivel];
    }

    public int contagemNivel(int nivel) {
        return tamanhos[nivel];
    }

    // Gera string no estilo /proc/buddyinfo do Linux.
    public String formatarBuddyinfo() {
        StringBuilder sb = new StringBuilder();
        sb.append("Blocos livres:\n  ");
        for (int i = 0; i < NIVEIS; i++) {
            int tam = tamanhoDoNivel(i);
            String rotulo;
            if (tam < 1024) {
                rotulo = tam + "KB";
            } else {
                rotulo = (tam / 1024) + "MB";
            }
            sb.append(rotulo).append(": ").append(tamanhos[i]);
            if (i < NIVEIS - 1) {
                sb.append(" | ");
            }
        }
        return sb.toString();
    }
}
