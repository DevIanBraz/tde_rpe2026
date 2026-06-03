// No da lista de blocos livres (uma por tamanho). Duplamente encadeada
// para permitir remocao do meio em O(1) sem percorrer a lista inteira.
public class NoLista {
    public No bloco;
    public NoLista proximo;
    public NoLista anterior;

    public NoLista(No bloco) {
        this.bloco = bloco;
        this.proximo = null;
        this.anterior = null;
    }
}
