// No da arvore do alocador buddy + arvore rubro-negra ao mesmo tempo.
// Cada no representa um bloco de memoria: pode estar LIVRE, OCUPADO ou DIVIDIDO.
// Mantem cor RB (VERMELHO/PRETO) para preservar as 5 propriedades rubro-negras.
public class No {
    // Sentinela NIL compartilhada por todos os filhos vazios.
    // E sempre PRETO (propriedade RB) e nao deve ser modificada.
    public static final No NIL = criarSentinela();

    public int tamanho;            // em KB
    public EstadoBloco estado;     // LIVRE | OCUPADO | DIVIDIDO
    public String identificador;   // null se LIVRE ou DIVIDIDO
    public No filhoEsquerdo;       // sempre != null (aponta para NIL se folha)
    public No filhoDireito;        // idem
    public No pai;                 // null somente na raiz
    public Cor cor;                // VERMELHO | PRETO

    public No(int tamanho, EstadoBloco estado, No pai) {
        this.tamanho = tamanho;
        this.estado = estado;
        this.identificador = null;
        this.filhoEsquerdo = NIL;
        this.filhoDireito = NIL;
        this.pai = pai;
        this.cor = Cor.VERMELHO;   // novos nos entram VERMELHO (padrao RB)
    }

    // Construtor privado para o sentinela NIL.
    private No() {
        this.tamanho = 0;
        this.estado = EstadoBloco.LIVRE;
        this.identificador = null;
        this.filhoEsquerdo = null;
        this.filhoDireito = null;
        this.pai = null;
        this.cor = Cor.PRETO;
    }

    private static No criarSentinela() {
        return new No();
    }

    // Verifica se este no e o sentinela. Usado em todo lugar para parar recursoes.
    public boolean ehNil() {
        return this == NIL;
    }
}
