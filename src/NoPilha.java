// No da pilha de historico de operacoes (LIFO). Encadeamento simples.
public class NoPilha {
    public OperacaoHistorico operacao;
    public NoPilha proximo;

    public NoPilha(OperacaoHistorico operacao) {
        this.operacao = operacao;
        this.proximo = null;
    }
}
