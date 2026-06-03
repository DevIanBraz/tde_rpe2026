import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

// Janela principal da aplicacao. Le o estado do AlocadorBuddy e desenha:
//   - mapa horizontal de memoria (folhas: ocupado/livre)
//   - arvore rubro-negra visual (estado + cor da borda = cor RB)
//   - fila de pendentes
//   - listas de blocos livres no estilo buddyinfo
//   - log textual de operacoes
// Toda interacao do usuario (botoes / cliques) chama metodos do alocador.
public class JanelaPrincipal extends JFrame {

    private AlocadorBuddy alocador;
    private PainelMapa painelMapa;
    private PainelArvore painelArvore;
    private DefaultListModel<String> modeloFila;
    private DefaultListModel<String> modeloBuddyinfo;
    private JTextArea areaLog;
    private JLabel rotuloStatus;

    public JanelaPrincipal() {
        super("Alocador Buddy Binario - 32 MB");
        this.alocador = new AlocadorBuddy();
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        construirUI();
        atualizarTudo();
        setSize(1400, 880);
        setLocationRelativeTo(null);
    }

    // Monta o layout: barra de botoes no topo, mapa+arvore no centro,
    // fila+buddyinfo na direita, log no rodape.
    private void construirUI() {
        JPanel raiz = new JPanel(new BorderLayout(8, 8));
        raiz.setBorder(new EmptyBorder(8, 8, 8, 8));

        JPanel topo = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        topo.setBackground(new Color(24, 28, 36));
        topo.setBorder(new EmptyBorder(10, 12, 10, 12));

        JButton btnAlocar   = botao("ALOCAR",            new Color(39, 174, 96),  new Color(76, 209, 130));
        JButton btnLiberar  = botao("LIBERAR",           new Color(231, 76, 60),  new Color(255, 118, 102));
        JButton btnDesfazer = botao("DESFAZER",          new Color(155, 89, 182), new Color(196, 130, 222));
        JButton btnCarregar = botao("CARREGAR DATASET",  new Color(52, 152, 219), new Color(102, 187, 245));
        JButton btnResetar  = botao("RESETAR",           new Color(241, 196, 15), new Color(255, 222, 84));

        btnAlocar.addActionListener(e -> acaoAlocar());
        btnLiberar.addActionListener(e -> acaoLiberar());
        btnDesfazer.addActionListener(e -> acaoDesfazer());
        btnCarregar.addActionListener(e -> acaoCarregar());
        btnResetar.addActionListener(e -> acaoResetar());

        topo.add(btnAlocar);
        topo.add(btnLiberar);
        topo.add(btnDesfazer);
        topo.add(btnCarregar);
        topo.add(btnResetar);

        rotuloStatus = new JLabel(" ");
        rotuloStatus.setBorder(new EmptyBorder(0, 18, 0, 0));
        rotuloStatus.setFont(rotuloStatus.getFont().deriveFont(Font.BOLD, 13f));
        rotuloStatus.setForeground(new Color(255, 255, 255));
        topo.add(rotuloStatus);

        painelMapa = new PainelMapa();
        painelArvore = new PainelArvore();

        JScrollPane scMapa = new JScrollPane(painelMapa);
        scMapa.setBorder(BorderFactory.createTitledBorder(
                "Mapa de memoria (clique em bloco OCUPADO para liberar) - verde = livre, vermelho = ocupado"));

        JScrollPane scArvore = new JScrollPane(painelArvore);
        scArvore.setBorder(BorderFactory.createTitledBorder(
                "Arvore rubro-negra (borda VERMELHA = no vermelho, borda PRETA = no preto)"));

        JSplitPane splitVert = new JSplitPane(JSplitPane.VERTICAL_SPLIT, scMapa, scArvore);
        splitVert.setResizeWeight(0.22);
        splitVert.setDividerLocation(160);

        modeloFila = new DefaultListModel<>();
        JList<String> listaFila = new JList<>(modeloFila);
        listaFila.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        JScrollPane scFila = new JScrollPane(listaFila);
        scFila.setBorder(BorderFactory.createTitledBorder("Fila de pendentes (FIFO)"));
        scFila.setPreferredSize(new Dimension(320, 180));

        modeloBuddyinfo = new DefaultListModel<>();
        JList<String> listaBuddy = new JList<>(modeloBuddyinfo);
        listaBuddy.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        JScrollPane scBuddy = new JScrollPane(listaBuddy);
        scBuddy.setBorder(BorderFactory.createTitledBorder("Listas livres (buddyinfo)"));

        JPanel direita = new JPanel(new BorderLayout(0, 8));
        direita.add(scFila, BorderLayout.NORTH);
        direita.add(scBuddy, BorderLayout.CENTER);
        direita.setPreferredSize(new Dimension(330, 0));

        areaLog = new JTextArea(8, 80);
        areaLog.setEditable(false);
        areaLog.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        JScrollPane scLog = new JScrollPane(areaLog);
        scLog.setBorder(BorderFactory.createTitledBorder("Historico de operacoes"));
        scLog.setPreferredSize(new Dimension(0, 160));

        JSplitPane splitHorz = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, splitVert, direita);
        splitHorz.setResizeWeight(0.78);
        splitHorz.setDividerLocation(1040);

        raiz.add(topo, BorderLayout.NORTH);
        raiz.add(splitHorz, BorderLayout.CENTER);
        raiz.add(scLog, BorderLayout.SOUTH);

        setContentPane(raiz);
    }

    // Fabrica botoes coloridos com efeito de hover.
    private JButton botao(String texto, Color fundo, Color hover) {
        final Color corOriginal = fundo;
        final Color corHover = hover;
        JButton b = new JButton(texto);
        b.setBackground(corOriginal);
        b.setForeground(Color.WHITE);
        b.setFont(b.getFont().deriveFont(Font.BOLD, 13f));
        b.setFocusPainted(false);
        b.setContentAreaFilled(true);
        b.setBorderPainted(false);
        b.setOpaque(true);
        b.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(corOriginal.darker(), 2, true),
                new EmptyBorder(10, 18, 10, 18)));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) {
                b.setBackground(corHover);
                b.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(Color.WHITE, 2, true),
                        new EmptyBorder(10, 18, 10, 18)));
            }
            @Override public void mouseExited(MouseEvent e) {
                b.setBackground(corOriginal);
                b.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(corOriginal.darker(), 2, true),
                        new EmptyBorder(10, 18, 10, 18)));
            }
        });
        return b;
    }

    private void acaoAlocar() {
        JTextField idField = new JTextField();
        JTextField kbField = new JTextField();
        Object[] msg = {"Identificador:", idField, "Tamanho (KB):", kbField};
        int res = JOptionPane.showConfirmDialog(this, msg, "Alocar",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (res != JOptionPane.OK_OPTION) return;
        String id = idField.getText().trim();
        if (id.isEmpty()) return;
        int kb;
        try { kb = Integer.parseInt(kbField.getText().trim()); }
        catch (NumberFormatException ex) { log("[!]    Tamanho invalido."); return; }
        boolean ok = alocador.alocar(id, kb);
        log((ok ? "[OK]   " : "[FILA] ") + "ALOCAR " + id + "  " + kb + " KB");
        atualizarTudo();
    }

    private void acaoLiberar() {
        String id = JOptionPane.showInputDialog(this,
                "Identificador do bloco a liberar:",
                "Liberar", JOptionPane.QUESTION_MESSAGE);
        if (id == null) return;
        id = id.trim();
        if (id.isEmpty()) return;
        liberarPorId(id);
    }

    private void liberarPorId(String id) {
        boolean ok = alocador.liberar(id);
        log((ok ? "[OK]   " : "[FAIL] ") + "LIBERAR " + id);
        atualizarTudo();
    }

    private void acaoDesfazer() {
        boolean ok = alocador.desfazer();
        log(ok ? "[UNDO] operacao desfeita" : "[UNDO] nada para desfazer");
        atualizarTudo();
    }

    private void acaoCarregar() {
        JFileChooser jfc = new JFileChooser(System.getProperty("user.dir"));
        jfc.setDialogTitle("Selecionar dataset");
        if (jfc.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
        carregarDataset(jfc.getSelectedFile());
    }

    // Le um arquivo .txt linha a linha. Ignora # e linhas vazias.
    // Cada ALOCAR/LIBERAR vira chamada ao alocador e entra no log.
    public void carregarDataset(File arquivo) {
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(arquivo));
            String linha;
            int n = 0;
            while ((linha = br.readLine()) != null) {
                linha = linha.trim();
                if (linha.length() == 0 || linha.charAt(0) == '#') continue;
                String[] partes = linha.split("\\s+");
                if (partes.length < 2) continue;
                String cmd = partes[0].toUpperCase();
                if (cmd.equals("ALOCAR") && partes.length >= 3) {
                    try {
                        int kb = Integer.parseInt(partes[2]);
                        boolean ok = alocador.alocar(partes[1], kb);
                        log((ok ? "[OK]   " : "[FILA] ") + "ALOCAR " + partes[1] + "  " + kb + " KB");
                    } catch (NumberFormatException ex) {
                        log("[!]    Tamanho invalido: " + linha);
                    }
                } else if (cmd.equals("LIBERAR")) {
                    boolean ok = alocador.liberar(partes[1]);
                    log((ok ? "[OK]   " : "[FAIL] ") + "LIBERAR " + partes[1]);
                }
                n++;
            }
            log("=== Dataset processado: " + n + " operacoes ===");
        } catch (IOException ex) {
            log("[!]    Erro ao ler: " + ex.getMessage());
        } finally {
            if (br != null) { try { br.close(); } catch (IOException ignored) {} }
        }
        atualizarTudo();
    }

    private void acaoResetar() {
        int res = JOptionPane.showConfirmDialog(this,
                "Recriar o alocador? Todo o estado sera perdido.",
                "Resetar", JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.WARNING_MESSAGE);
        if (res != JOptionPane.OK_OPTION) return;
        alocador = new AlocadorBuddy();
        areaLog.setText("");
        log("[RESET] Memoria reinicializada.");
        atualizarTudo();
    }

    private void log(String msg) {
        areaLog.append(msg + "\n");
        areaLog.setCaretPosition(areaLog.getDocument().getLength());
    }

    // Sincroniza toda a UI com o estado atual do alocador.
    // Chamado depois de cada operacao (alocar, liberar, undo, etc).
    private void atualizarTudo() {
        painelMapa.repaint();
        painelArvore.recalcular();
        painelArvore.repaint();

        modeloFila.clear();
        NoFila nf = alocador.getPendentes().primeiro();
        if (nf == null) {
            modeloFila.addElement("  (vazia)");
        } else {
            int i = 1;
            while (nf != null) {
                modeloFila.addElement(String.format(" %2d. %-14s %7d KB", i, nf.identificador, nf.tamanhoKB));
                nf = nf.proximo;
                i++;
            }
        }

        modeloBuddyinfo.clear();
        for (int i = 0; i < ListaBlocos.NIVEIS; i++) {
            int t = ListaBlocos.tamanhoDoNivel(i);
            String rot = (t < 1024) ? t + "KB" : (t / 1024) + "MB";
            int c = alocador.getListas().contagemNivel(i);
            String barra = repetir('=', c);
            modeloBuddyinfo.addElement(String.format(" %6s : %2d  %s", rot, c, barra));
        }

        int usado = calcularUsado(alocador.getRaiz());
        int livre = AlocadorBuddy.MEMORIA_TOTAL_KB - usado;
        rotuloStatus.setText(String.format(
                "Usado: %s   |   Livre: %s   |   Pendentes: %d   |   Historico (undo): %d ops",
                formatarKB(usado), formatarKB(livre),
                alocador.getPendentes().tamanho(),
                alocador.getHistorico().tamanho()));
    }

    private String repetir(char c, int n) {
        if (n <= 0) return "";
        char[] arr = new char[n];
        for (int i = 0; i < n; i++) arr[i] = c;
        return new String(arr);
    }

    // Soma o tamanho de todos os blocos OCUPADO da arvore.
    private static int calcularUsado(No no) {
        if (no == null || no.ehNil()) return 0;
        if (no.estado == EstadoBloco.OCUPADO) return no.tamanho;
        return calcularUsado(no.filhoEsquerdo) + calcularUsado(no.filhoDireito);
    }

    private static String formatarKB(int kb) {
        if (kb < 1024) return kb + " KB";
        if (kb % 1024 == 0) return (kb / 1024) + " MB";
        return String.format("%.1f MB", kb / 1024.0);
    }

    // Painel que desenha o MAPA HORIZONTAL da memoria.
    // Mostra apenas as folhas (LIVRE em verde, OCUPADO em vermelho).
    // Largura de cada retangulo e proporcional ao tamanho do bloco.
    // Clique em bloco OCUPADO libera; hover mostra tooltip.
    private class PainelMapa extends JPanel implements Scrollable {

        private int areaX, areaY, areaW, areaH;

        public PainelMapa() {
            setBackground(Color.WHITE);
            ToolTipManager.sharedInstance().registerComponent(this);
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    No alvo = blocoEm(e.getX(), e.getY());
                    if (alvo != null && alvo.estado == EstadoBloco.OCUPADO) {
                        int r = JOptionPane.showConfirmDialog(JanelaPrincipal.this,
                                "Liberar bloco '" + alvo.identificador + "' (" + formatarKB(alvo.tamanho) + ") ?",
                                "Liberar", JOptionPane.YES_NO_OPTION);
                        if (r == JOptionPane.YES_OPTION) liberarPorId(alvo.identificador);
                    }
                }
            });
        }

        @Override
        public String getToolTipText(MouseEvent e) {
            No n = blocoEm(e.getX(), e.getY());
            if (n == null) return null;
            String idTxt = (n.identificador == null) ? "" : "  id=" + n.identificador;
            return n.estado + " - " + formatarKB(n.tamanho) + idTxt + "   (RB: " + n.cor + ")";
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(900, 110);
        }

        private No blocoEm(int x, int y) {
            return achar(alocador.getRaiz(), areaX, areaY, areaW, areaH, x, y);
        }

        // Recursao geometrica: encontra o no folha que cobre o ponto (mx, my).
        private No achar(No no, int x0, int y0, int w0, int h0, int mx, int my) {
            if (no == null || no.ehNil() || w0 <= 0) return null;
            if (mx < x0 || mx >= x0 + w0 || my < y0 || my >= y0 + h0) return null;
            if (no.estado == EstadoBloco.DIVIDIDO) {
                int meio = w0 / 2;
                No r = achar(no.filhoEsquerdo, x0, y0, meio, h0, mx, my);
                if (r != null) return r;
                return achar(no.filhoDireito, x0 + meio, y0, w0 - meio, h0, mx, my);
            }
            return no;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int margem = 12;
            areaX = margem;
            areaY = margem;
            areaW = getWidth() - 2 * margem;
            areaH = getHeight() - 2 * margem;
            desenharFolhas(g2, alocador.getRaiz(), areaX, areaY, areaW, areaH);
            g2.setColor(new Color(60, 60, 60));
            g2.setStroke(new BasicStroke(1.4f));
            g2.drawRect(areaX, areaY, areaW, areaH);
            g2.dispose();
        }

        // Recursao geometrica: desce dividindo a area ao meio em cada no DIVIDIDO.
        // Quando chega numa folha (LIVRE ou OCUPADO), pinta o retangulo.
        private void desenharFolhas(Graphics2D g, No no, int x, int y, int w, int h) {
            if (no == null || no.ehNil() || w <= 0) return;
            if (no.estado == EstadoBloco.DIVIDIDO) {
                int meio = w / 2;
                desenharFolhas(g, no.filhoEsquerdo, x, y, meio, h);
                desenharFolhas(g, no.filhoDireito, x + meio, y, w - meio, h);
                return;
            }
            Color c = (no.estado == EstadoBloco.OCUPADO)
                    ? new Color(231, 76, 60)
                    : new Color(46, 204, 113);
            g.setColor(c);
            g.fillRect(x, y, w, h);
            g.setColor(Color.WHITE);
            g.setStroke(new BasicStroke(1f));
            g.drawRect(x, y, w, h);

            String texto = (no.estado == EstadoBloco.OCUPADO)
                    ? no.identificador + "  " + formatarKB(no.tamanho)
                    : formatarKB(no.tamanho);
            g.setFont(g.getFont().deriveFont(Font.BOLD, 11f));
            FontMetrics fm = g.getFontMetrics();
            int tw = fm.stringWidth(texto);
            if (tw < w - 6 && fm.getHeight() < h - 4) {
                int tx = x + (w - tw) / 2;
                int ty = y + (h + fm.getAscent()) / 2 - 2;
                g.setColor(Color.WHITE);
                g.drawString(texto, tx, ty);
            }
        }

        @Override public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
        @Override public int getScrollableUnitIncrement(Rectangle r, int o, int d) { return 16; }
        @Override public int getScrollableBlockIncrement(Rectangle r, int o, int d) { return 80; }
        @Override public boolean getScrollableTracksViewportWidth() { return true; }
        @Override public boolean getScrollableTracksViewportHeight() { return true; }
    }

    // Painel que desenha a ARVORE RUBRO-NEGRA hierarquicamente.
    // Cada no e um retangulo arredondado com:
    //   - fundo da cor do estado (vermelho=OCUPADO, verde=LIVRE, cinza=DIVIDIDO)
    //   - borda VERMELHA grossa se o no RB e VERMELHO; PRETA fina se e PRETO
    // Mostra nos DIVIDIDO (que o mapa nao mostra), provando que a arvore esta viva.
    private class PainelArvore extends JPanel implements Scrollable {

        private int alturaPreferida = 600;

        public PainelArvore() {
            setBackground(Color.WHITE);
            ToolTipManager.sharedInstance().registerComponent(this);
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    No alvo = acharNo(alocador.getRaiz(), 16, 16,
                            Math.max(200, getWidth() - 32), 56, e.getX(), e.getY());
                    if (alvo != null && alvo.estado == EstadoBloco.OCUPADO) {
                        int r = JOptionPane.showConfirmDialog(JanelaPrincipal.this,
                                "Liberar '" + alvo.identificador + "' (" + formatarKB(alvo.tamanho) + ") ?",
                                "Liberar", JOptionPane.YES_NO_OPTION);
                        if (r == JOptionPane.YES_OPTION) liberarPorId(alvo.identificador);
                    }
                }
            });
        }

        public void recalcular() {
            int p = profundidade(alocador.getRaiz());
            alturaPreferida = Math.max(220, (p + 1) * 56 + 24);
            revalidate();
        }

        private int profundidade(No no) {
            if (no == null || no.ehNil()) return -1;
            return 1 + Math.max(profundidade(no.filhoEsquerdo), profundidade(no.filhoDireito));
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(900, alturaPreferida);
        }

        @Override
        public String getToolTipText(MouseEvent e) {
            No n = acharNo(alocador.getRaiz(), 16, 16,
                    Math.max(200, getWidth() - 32), 56, e.getX(), e.getY());
            if (n == null) return null;
            String idTxt = (n.identificador == null) ? "" : "  id=" + n.identificador;
            return n.estado + " - " + formatarKB(n.tamanho) + idTxt + "   (RB: " + n.cor + ")";
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int margem = 16;
            int alturaNivel = 56;
            int w = Math.max(200, getWidth() - 2 * margem);
            desenharNo(g2, alocador.getRaiz(), margem, margem, w, alturaNivel);
            g2.dispose();
        }

        // Recursao: desenha o no atual, depois os dois filhos abaixo dele.
        // Cada filho ocupa metade da largura horizontal do pai.
        private void desenharNo(Graphics2D g, No no, int x, int y, int w, int alturaNivel) {
            if (no == null || no.ehNil()) return;
            int h = 40;
            Color fundo;
            switch (no.estado) {
                case OCUPADO: fundo = new Color(231, 76, 60); break;
                case LIVRE:   fundo = new Color(46, 204, 113); break;
                default:      fundo = new Color(189, 195, 199);
            }
            g.setColor(fundo);
            g.fillRoundRect(x + 2, y, Math.max(8, w - 4), h, 12, 12);

            Color borda = (no.cor == Cor.VERMELHO) ? new Color(192, 57, 43) : new Color(40, 40, 40);
            g.setColor(borda);
            g.setStroke(new BasicStroke(no.cor == Cor.VERMELHO ? 2.6f : 1.6f));
            g.drawRoundRect(x + 2, y, Math.max(8, w - 4), h, 12, 12);

            String texto = formatarKB(no.tamanho) + "  " + no.estado;
            if (no.identificador != null) texto += "  [" + no.identificador + "]";
            g.setFont(g.getFont().deriveFont(Font.BOLD, 12f));
            FontMetrics fm = g.getFontMetrics();
            int tw = fm.stringWidth(texto);
            if (tw < w - 14) {
                g.setColor(Color.WHITE);
                g.drawString(texto, x + (w - tw) / 2, y + (h + fm.getAscent()) / 2 - 2);
            }

            boolean temEsq = !no.filhoEsquerdo.ehNil();
            boolean temDir = !no.filhoDireito.ehNil();
            int yFilho = y + alturaNivel;
            int wMeio = w / 2;

            if (temEsq || temDir) {
                g.setColor(new Color(140, 140, 140));
                g.setStroke(new BasicStroke(1f));
                int xCentro = x + w / 2;
                if (temEsq) g.drawLine(xCentro, y + h, x + wMeio / 2, yFilho);
                if (temDir) g.drawLine(xCentro, y + h, x + wMeio + (w - wMeio) / 2, yFilho);
            }
            if (temEsq) desenharNo(g, no.filhoEsquerdo, x, yFilho, wMeio, alturaNivel);
            if (temDir) desenharNo(g, no.filhoDireito, x + wMeio, yFilho, w - wMeio, alturaNivel);
        }

        private No acharNo(No no, int x, int y, int w, int alturaNivel, int mx, int my) {
            if (no == null || no.ehNil()) return null;
            int h = 40;
            if (mx >= x + 2 && mx < x + w - 2 && my >= y && my < y + h) return no;
            int yFilho = y + alturaNivel;
            int wMeio = w / 2;
            No r = acharNo(no.filhoEsquerdo, x, yFilho, wMeio, alturaNivel, mx, my);
            if (r != null) return r;
            return acharNo(no.filhoDireito, x + wMeio, yFilho, w - wMeio, alturaNivel, mx, my);
        }

        @Override public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
        @Override public int getScrollableUnitIncrement(Rectangle r, int o, int d) { return 24; }
        @Override public int getScrollableBlockIncrement(Rectangle r, int o, int d) { return 80; }
        @Override public boolean getScrollableTracksViewportWidth() { return true; }
        @Override public boolean getScrollableTracksViewportHeight() { return false; }
    }
}
