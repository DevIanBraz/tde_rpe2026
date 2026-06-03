// No da fila de requisicoes pendentes (FIFO). Encadeamento simples.
public class NoFila {
    public String identificador;
    public int tamanhoKB;
    public NoFila proximo;

    public NoFila(String identificador, int tamanhoKB) {
        this.identificador = identificador;
        this.tamanhoKB = tamanhoKB;
        this.proximo = null;
    }
}
