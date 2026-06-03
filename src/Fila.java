// Fila FIFO de requisicoes pendentes. Ponteiros inicio/fim + contador.
// Toda alocacao que falha vai pro fim; cada liberacao tenta atender o inicio.
public class Fila {
    private NoFila inicio;
    private NoFila fim;
    private int qtd;

    public Fila() {
        this.inicio = null;
        this.fim = null;
        this.qtd = 0;
    }

    // Insere no fim da fila em O(1).
    public void enfileirar(String identificador, int tamanhoKB) {
        NoFila novo = new NoFila(identificador, tamanhoKB);
        if (inicio == null) {
            inicio = novo;
            fim = novo;
        } else {
            fim.proximo = novo;
            fim = novo;
        }
        qtd++;
    }

    // Remove e retorna o no do inicio (null se vazia).
    public NoFila desenfileirar() {
        if (inicio == null) {
            return null;
        }
        NoFila removido = inicio;
        inicio = inicio.proximo;
        if (inicio == null) {
            fim = null;
        }
        removido.proximo = null;
        qtd--;
        return removido;
    }

    // Olha o no do inicio sem remover.
    public NoFila espiar() {
        return inicio;
    }

    public boolean estaVazia() {
        return inicio == null;
    }

    public int tamanho() {
        return qtd;
    }

    // Usado pela UI para iterar a fila sem desenfileirar.
    public NoFila primeiro() {
        return inicio;
    }
}
