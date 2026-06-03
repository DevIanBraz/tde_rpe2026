// Registro de uma operacao para permitir undo (desfazer).
// Guarda o tipo (ALOCAR/LIBERAR), o id, o tamanho solicitado e o no afetado.
public class OperacaoHistorico {
    public static final int ALOCAR = 1;
    public static final int LIBERAR = 2;

    public int tipo;
    public String identificador;
    public int tamanhoKB;
    public No noAfetado;

    public OperacaoHistorico(int tipo, String identificador, int tamanhoKB, No noAfetado) {
        this.tipo = tipo;
        this.identificador = identificador;
        this.tamanhoKB = tamanhoKB;
        this.noAfetado = noAfetado;
    }

    public String tipoComoString() {
        if (tipo == ALOCAR) return "ALOCAR";
        if (tipo == LIBERAR) return "LIBERAR";
        return "?";
    }
}
