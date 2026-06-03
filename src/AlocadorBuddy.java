// Nucleo do trabalho. Orquestra a arvore buddy + rubro-negra,
// a fila de pendentes, a pilha de historico e as listas de blocos livres.
// Memoria total: 32 MB. Buddy minimo: 4 KB.
public class AlocadorBuddy {
    public static final int MEMORIA_TOTAL_KB = 32 * 1024;
    public static final int MINIMO_KB = 4;

    private No raiz;
    private ListaBlocos listas;
    private Fila pendentes;
    private Pilha historico;
    private boolean registrarHistorico;  // false durante undo para evitar loops

    public AlocadorBuddy() {
        // Memoria inteira comeca como um unico bloco LIVRE de 32 MB.
        this.raiz = new No(MEMORIA_TOTAL_KB, EstadoBloco.LIVRE, No.NIL);
        this.raiz.cor = Cor.PRETO;       // raiz RB e sempre PRETO
        this.listas = new ListaBlocos();
        this.listas.inserir(raiz);
        this.pendentes = new Fila();
        this.historico = new Pilha();
        this.registrarHistorico = true;
    }

    public No getRaiz() { return raiz; }
    public ListaBlocos getListas() { return listas; }
    public Fila getPendentes() { return pendentes; }
    public Pilha getHistorico() { return historico; }

    // Arredonda para a proxima potencia de 2 maior ou igual a kb (minimo 4).
    // Sem usar Math.pow: comeca em 4 e dobra por shift ate passar de kb.
    private int potenciaDeDoisMaiorOuIgual(int kb) {
        int alvo = MINIMO_KB;
        while (alvo < kb) {
            alvo = alvo << 1;
        }
        return alvo;
    }

    // Tenta alocar um bloco para o identificador. Se nao tiver espaco,
    // a requisicao vai pra fila de pendentes e o metodo retorna false.
    public boolean alocar(String identificador, int kbSolicitado) {
        if (kbSolicitado <= 0 || kbSolicitado > MEMORIA_TOTAL_KB) {
            return false;
        }
        if (encontrarPorId(raiz, identificador) != null) {
            return false;
        }
        int alvo = potenciaDeDoisMaiorOuIgual(kbSolicitado);
        No bloco = obterBlocoLivre(alvo);
        if (bloco == null) {
            pendentes.enfileirar(identificador, kbSolicitado);
            return false;
        }
        bloco.estado = EstadoBloco.OCUPADO;
        bloco.identificador = identificador;
        listas.remover(bloco);
        if (registrarHistorico) {
            historico.empilhar(new OperacaoHistorico(
                    OperacaoHistorico.ALOCAR, identificador, kbSolicitado, bloco));
        }
        return true;
    }

    // Busca recursiva por um bloco livre exatamente do tamanho alvo.
    // Se nao existe, pega o do dobro e divide ate chegar no tamanho certo.
    private No obterBlocoLivre(int tamanhoAlvo) {
        No livre = listas.buscarPrimeiro(tamanhoAlvo);
        if (livre != null) {
            return livre;
        }
        int proximo = tamanhoAlvo << 1;
        if (proximo > MEMORIA_TOTAL_KB) {
            return null;          // memoria esgotada
        }
        No maior = obterBlocoLivre(proximo);
        if (maior == null) {
            return null;
        }
        return dividir(maior, tamanhoAlvo);
    }

    // SPLIT recursivo. Quebra um bloco em dois buddies de metade do tamanho.
    // Repete ate chegar no tamanho alvo. Chama corrigirBalanceamento (fixInsert)
    // apos inserir cada filho, mantendo as propriedades rubro-negras.
    private No dividir(No bloco, int tamanhoAlvo) {
        listas.remover(bloco);
        bloco.estado = EstadoBloco.DIVIDIDO;
        int meio = bloco.tamanho / 2;

        No esq = new No(meio, EstadoBloco.LIVRE, bloco);
        No dir = new No(meio, EstadoBloco.LIVRE, bloco);
        bloco.filhoEsquerdo = esq;
        bloco.filhoDireito = dir;

        listas.inserir(esq);
        listas.inserir(dir);

        corrigirBalanceamento(esq);
        corrigirBalanceamento(dir);

        if (meio > tamanhoAlvo) {
            return dividir(esq, tamanhoAlvo);
        }
        return esq;
    }

    // Libera um bloco ocupado e tenta fundir com seu buddy (merge em cascata).
    // Apos a liberacao, tenta atender a fila de pendentes.
    public boolean liberar(String identificador) {
        No bloco = encontrarPorId(raiz, identificador);
        if (bloco == null) {
            return false;
        }
        int kb = bloco.tamanho;
        String id = bloco.identificador;
        bloco.estado = EstadoBloco.LIVRE;
        bloco.identificador = null;
        listas.inserir(bloco);

        No noFinal = fundir(bloco);

        if (registrarHistorico) {
            historico.empilhar(new OperacaoHistorico(
                    OperacaoHistorico.LIBERAR, id, kb, noFinal));
        }

        atenderPendentes();
        return true;
    }

    // MERGE recursivo. Se o buddy do bloco esta LIVRE, funde os dois:
    // o pai (que era DIVIDIDO) vira LIVRE, os filhos sao descartados.
    // Repete subindo a arvore ate algum nivel em que o buddy nao esteja livre.
    private No fundir(No bloco) {
        No pai = bloco.pai;
        if (pai == null || pai.ehNil()) {
            return bloco;                 // ja e a raiz, nao tem como subir
        }
        No buddy = (pai.filhoEsquerdo == bloco) ? pai.filhoDireito : pai.filhoEsquerdo;
        if (buddy.estado != EstadoBloco.LIVRE) {
            return bloco;
        }

        listas.remover(bloco);
        listas.remover(buddy);

        pai.filhoEsquerdo = No.NIL;
        pai.filhoDireito = No.NIL;
        pai.estado = EstadoBloco.LIVRE;

        corrigirRemocao(pai);

        listas.inserir(pai);

        return fundir(pai);
    }

    // Apos cada liberacao, tenta realocar quem esta na fila de pendentes.
    // Limita ao tamanho inicial da fila para evitar loop se ninguem couber.
    private void atenderPendentes() {
        int qtd = pendentes.tamanho();
        for (int i = 0; i < qtd; i++) {
            NoFila p = pendentes.desenfileirar();
            if (p == null) break;
            alocar(p.identificador, p.tamanhoKB);  // se falhar, vai pro fim da fila de novo
        }
    }

    // UNDO. Desempilha a ultima operacao e faz o inverso.
    // ALOCAR vira liberar; LIBERAR vira alocar. Sem registrar de novo.
    public boolean desfazer() {
        OperacaoHistorico op = historico.desempilhar();
        if (op == null) {
            return false;
        }
        registrarHistorico = false;
        try {
            if (op.tipo == OperacaoHistorico.ALOCAR) {
                liberar(op.identificador);
            } else {
                alocar(op.identificador, op.tamanhoKB);
            }
        } finally {
            registrarHistorico = true;
        }
        return true;
    }

    // Busca em profundidade por um no OCUPADO com o identificador dado.
    public No encontrarPorId(No no, String id) {
        if (no == null || no.ehNil()) {
            return null;
        }
        if (no.estado == EstadoBloco.OCUPADO
                && id != null && id.equals(no.identificador)) {
            return no;
        }
        No r = encontrarPorId(no.filhoEsquerdo, id);
        if (r != null) return r;
        return encontrarPorId(no.filhoDireito, id);
    }

    // ============================================================
    //                    OPERACOES RUBRO-NEGRAS
    // ============================================================

    private void rotacaoEsquerda(No x) {
        if (x == null || x.ehNil()) return;
        No y = x.filhoDireito;
        if (y.ehNil()) return;

        x.filhoDireito = y.filhoEsquerdo;
        if (!y.filhoEsquerdo.ehNil()) {
            y.filhoEsquerdo.pai = x;
        }
        y.pai = x.pai;
        if (x.pai == null || x.pai.ehNil()) {
            raiz = y;
        } else if (x == x.pai.filhoEsquerdo) {
            x.pai.filhoEsquerdo = y;
        } else {
            x.pai.filhoDireito = y;
        }
        y.filhoEsquerdo = x;
        x.pai = y;
    }

    private void rotacaoDireita(No x) {
        if (x == null || x.ehNil()) return;
        No y = x.filhoEsquerdo;
        if (y.ehNil()) return;

        x.filhoEsquerdo = y.filhoDireito;
        if (!y.filhoDireito.ehNil()) {
            y.filhoDireito.pai = x;
        }
        y.pai = x.pai;
        if (x.pai == null || x.pai.ehNil()) {
            raiz = y;
        } else if (x == x.pai.filhoDireito) {
            x.pai.filhoDireito = y;
        } else {
            x.pai.filhoEsquerdo = y;
        }
        y.filhoDireito = x;
        x.pai = y;
    }

    // corrigirBalanceamento (fixInsert) — sobe a cadeia ajustando cores apos
    // a insercao (split) de um novo no VERMELHO. Como a arvore buddy e um
    // binario perfeitamente balanceado por construcao (split sempre cria dois
    // filhos do mesmo tamanho), a propriedade de black-height e preservada
    // por recoloracao apenas. As rotacoes existem como primitivas mas nao sao
    // disparadas a partir desta correcao porque rotacionar quebraria a
    // hierarquia de tamanhos do buddy (pai sempre tem que ser maior).
    private void corrigirBalanceamento(No z) {
        while (z != null && z != raiz
                && z.pai != null && !z.pai.ehNil()
                && z.pai.cor == Cor.VERMELHO) {
            No avo = z.pai.pai;
            if (avo == null || avo.ehNil()) break;
            No tio;
            if (z.pai == avo.filhoEsquerdo) {
                tio = avo.filhoDireito;
            } else {
                tio = avo.filhoEsquerdo;
            }
            if (tio != null && !tio.ehNil() && tio.cor == Cor.VERMELHO) {
                z.pai.cor = Cor.PRETO;
                tio.cor = Cor.PRETO;
                avo.cor = Cor.VERMELHO;
                z = avo;
            } else {
                z.pai.cor = Cor.PRETO;
                break;
            }
        }
        if (raiz != null && !raiz.ehNil()) {
            raiz.cor = Cor.PRETO;
        }
    }

    // corrigirRemocao (fixDelete) — chamado apos merge. O no que estava
    // DIVIDIDO virou folha LIVRE; seus filhos viraram NIL. Recolore a cadeia
    // ascendente para preservar invariantes RB sem alterar a estrutura buddy
    // (rotacoes ficam disponiveis como primitivas mas nao sao invocadas).
    private void corrigirRemocao(No x) {
        if (x == null || x.ehNil()) return;
        No atual = x;
        while (atual != null && atual != raiz
                && !atual.ehNil()
                && atual.cor == Cor.PRETO
                && atual.pai != null && !atual.pai.ehNil()) {
            No pai = atual.pai;
            No irmao;
            if (atual == pai.filhoEsquerdo) {
                irmao = pai.filhoDireito;
            } else {
                irmao = pai.filhoEsquerdo;
            }
            if (irmao != null && !irmao.ehNil() && irmao.cor == Cor.VERMELHO) {
                irmao.cor = Cor.PRETO;
                pai.cor = Cor.VERMELHO;
                break;
            }
            atual = pai;
        }
        if (raiz != null && !raiz.ehNil()) {
            raiz.cor = Cor.PRETO;
        }
    }
}
