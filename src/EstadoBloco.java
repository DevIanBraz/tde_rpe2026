// Estado de um bloco de memoria no alocador buddy.
// LIVRE: disponivel | OCUPADO: alocado a um id | DIVIDIDO: tem dois filhos buddy.
public enum EstadoBloco {
    LIVRE,
    OCUPADO,
    DIVIDIDO
}
