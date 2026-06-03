import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.io.File;

// Ponto de entrada. Abre a janela Swing e, se foi passado um caminho de
// dataset como argumento, carrega ele assim que a janela aparece.
public class Main {

    public static void main(String[] args) {
        // Usa o look-and-feel nativo do sistema operacional.
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}

        final File dataset;
        if (args != null && args.length > 0) {
            dataset = new File(args[0]);
        } else {
            dataset = null;
        }

        // Toda manipulacao de UI Swing deve ocorrer na Event Dispatch Thread.
        SwingUtilities.invokeLater(() -> {
            JanelaPrincipal janela = new JanelaPrincipal();
            janela.setVisible(true);
            if (dataset != null && dataset.exists()) {
                janela.carregarDataset(dataset);
            }
        });
    }
}
