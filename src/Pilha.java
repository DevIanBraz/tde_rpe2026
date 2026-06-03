// Pilha LIFO de historico de operacoes para suportar undo.
// Toda alocacao/liberacao bem-sucedida e empilhada; desfazer desempilha.
public class Pilha {
    private NoPilha topo;
    private int qtd;

    public Pilha() {
        this.topo = null;
        this.qtd = 0;
    }

    // Coloca uma operacao no topo em O(1).
    public void empilhar(OperacaoHistorico operacao) {
        NoPilha novo = new NoPilha(operacao);
        novo.proximo = topo;
        topo = novo;
        qtd++;
    }

    // Remove e retorna a operacao do topo (null se vazia).
    public OperacaoHistorico desempilhar() {
        if (topo == null) {
            return null;
        }
        OperacaoHistorico op = topo.operacao;
        topo = topo.proximo;
        qtd--;
        return op;
    }

    // Olha o topo sem remover.
    public OperacaoHistorico topo() {
        if (topo == null) {
            return null;
        }
        return topo.operacao;
    }

    public boolean estaVazia() {
        return topo == null;
    }

    public int tamanho() {
        return qtd;
    }
}
