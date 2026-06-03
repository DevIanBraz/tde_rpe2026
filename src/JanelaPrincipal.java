import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

// Janela principal da aplicacao. Layout moderno em cards, com:
//   - cabecalho: titulo + cards de status (usado/livre/pendentes/historico)
//   - barra de acoes: botao primario ALOCAR + secundarios + sair
//   - mapa horizontal da memoria (folhas: ocupado/livre)
//   - arvore rubro-negra visual (estado + cor da borda = cor RB)
//   - fila de pendentes e listas de blocos livres (lateral direita)
//   - log textual de operacoes (rodape)
// Toda interacao do usuario chama metodos publicos do AlocadorBuddy.
public class JanelaPrincipal extends JFrame {

    // ===== Paleta de cores =====
    private static final Color BG             = new Color(245, 247, 250);
    private static final Color PANEL          = Color.WHITE;
    private static final Color BORDER         = new Color(226, 232, 240);
    private static final Color TEXT_PRIMARY   = new Color(15, 23, 42);
    private static final Color TEXT_SECONDARY = new Color(71, 85, 105);
    private static final Color TEXT_MUTED     = new Color(148, 163, 184);
    private static final Color ACCENT         = new Color(37, 99, 235);
    private static final Color ACCENT_HOVER   = new Color(29, 78, 216);
    private static final Color DANGER         = new Color(220, 38, 38);
    private static final Color WARNING        = new Color(245, 158, 11);
    private static final Color HOVER_LIGHT    = new Color(241, 245, 249);
    private static final Color BLOCK_LIVRE    = new Color(52, 211, 153);
    private static final Color BLOCK_OCUPADO  = new Color(248, 113, 113);
    private static final Color BLOCK_DIVIDIDO = new Color(203, 213, 225);
    private static final Color TREE_LINE      = new Color(203, 213, 225);
    private static final Color RB_VERMELHO    = new Color(220, 38, 38);
    private static final Color RB_PRETO       = new Color(30, 41, 59);

    // ===== Tipografia =====
    private static final Font FONT_TITLE       = new Font("Segoe UI", Font.BOLD,  22);
    private static final Font FONT_VALUE       = new Font("Segoe UI", Font.BOLD,  20);
    private static final Font FONT_BUTTON      = new Font("Segoe UI", Font.BOLD,  13);
    private static final Font FONT_BODY        = new Font("Segoe UI", Font.PLAIN, 13);
    private static final Font FONT_LABEL       = new Font("Segoe UI", Font.BOLD,  11);
    private static final Font FONT_MONO        = new Font(Font.MONOSPACED, Font.PLAIN, 12);
    private static final Font FONT_BLOCK       = new Font("Segoe UI", Font.BOLD,  11);

    // ===== Icones do log =====
    private static final String ICO_OK   = "✓";   // ✓
    private static final String ICO_FILA = "⚠";   // ⚠
    private static final String ICO_FAIL = "✗";   // ✗
    private static final String ICO_UNDO = "↶";   // ↶
    private static final String ICO_INFO = "●";   // ●

    // ===== Estado =====
    private AlocadorBuddy alocador;

    // ===== Componentes =====
    private PainelMapa painelMapa;
    private PainelArvore painelArvore;
    private DefaultListModel<String> modeloFila;
    private DefaultListModel<String> modeloBuddyinfo;
    private JTextArea areaLog;
    private JLabel valorUsado;
    private JLabel valorLivre;
    private JLabel valorPendentes;
    private JLabel valorHistorico;

    public JanelaPrincipal() {
        super("Alocador Buddy Binario");
        this.alocador = new AlocadorBuddy();
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setIconImage(criarIcone());
        construirUI();
        atualizarTudo();
        setSize(1440, 900);
        setMinimumSize(new Dimension(1180, 760));
        setLocationRelativeTo(null);
    }

    // Cria um icone simples para a janela (quatro blocos representando split/buddy).
    private Image criarIcone() {
        BufferedImage img = new BufferedImage(48, 48, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(ACCENT);
        g.fillRoundRect(0, 0, 48, 48, 12, 12);
        g.setColor(Color.WHITE);
        g.fillRoundRect(8, 8, 14, 14, 3, 3);
        g.fillRoundRect(26, 8, 14, 14, 3, 3);
        g.fillRoundRect(8, 26, 14, 14, 3, 3);
        g.fillRoundRect(26, 26, 14, 14, 3, 3);
        g.dispose();
        return img;
    }

    // Monta a UI inteira em tres regioes: cabecalho, corpo e rodape.
    private void construirUI() {
        JPanel raiz = new JPanel(new BorderLayout(16, 16));
        raiz.setBackground(BG);
        raiz.setBorder(new EmptyBorder(22, 22, 22, 22));

        raiz.add(construirCabecalho(), BorderLayout.NORTH);
        raiz.add(construirCorpo(),     BorderLayout.CENTER);
        raiz.add(construirRodape(),    BorderLayout.SOUTH);

        setContentPane(raiz);
    }

    // Cabecalho com titulo, 4 cards de status e barra de acoes.
    private JComponent construirCabecalho() {
        JPanel topo = new JPanel(new BorderLayout(20, 0));
        topo.setOpaque(false);

        JPanel tituloBox = new JPanel();
        tituloBox.setLayout(new BoxLayout(tituloBox, BoxLayout.Y_AXIS));
        tituloBox.setOpaque(false);

        JLabel titulo = new JLabel("Alocador Buddy Binario");
        titulo.setFont(FONT_TITLE);
        titulo.setForeground(TEXT_PRIMARY);
        titulo.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel subtitulo = new JLabel("Simulador  /  32 MB  /  buddy minimo 4 KB  /  arvore rubro-negra");
        subtitulo.setFont(FONT_BODY);
        subtitulo.setForeground(TEXT_SECONDARY);
        subtitulo.setBorder(new EmptyBorder(4, 0, 0, 0));
        subtitulo.setAlignmentX(Component.LEFT_ALIGNMENT);

        tituloBox.add(titulo);
        tituloBox.add(subtitulo);

        topo.add(tituloBox, BorderLayout.WEST);
        topo.add(construirStatsCards(), BorderLayout.EAST);

        JPanel separador = new JPanel();
        separador.setBackground(BORDER);
        separador.setPreferredSize(new Dimension(0, 1));
        separador.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));

        JPanel botoes = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        botoes.setOpaque(false);

        JButton btnAlocar   = botaoPrimario("ALOCAR");
        JButton btnLiberar  = botaoSecundario("Liberar");
        JButton btnDesfazer = botaoSecundario("Desfazer");
        JButton btnCarregar = botaoSecundario("Carregar dataset");
        JButton btnResetar  = botaoDestrutivo("Resetar");
        JButton btnSair     = botaoSecundario("Sair");

        btnAlocar.addActionListener(e -> acaoAlocar());
        btnLiberar.addActionListener(e -> acaoLiberar());
        btnDesfazer.addActionListener(e -> acaoDesfazer());
        btnCarregar.addActionListener(e -> acaoCarregar());
        btnResetar.addActionListener(e -> acaoResetar());
        btnSair.addActionListener(e -> System.exit(0));

        botoes.add(btnAlocar);
        botoes.add(btnLiberar);
        botoes.add(btnDesfazer);
        botoes.add(btnCarregar);
        botoes.add(btnResetar);
        botoes.add(Box.createHorizontalStrut(20));
        botoes.add(btnSair);

        JPanel cabecalho = new JPanel();
        cabecalho.setLayout(new BoxLayout(cabecalho, BoxLayout.Y_AXIS));
        cabecalho.setOpaque(false);

        topo.setAlignmentX(Component.LEFT_ALIGNMENT);
        topo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 90));
        botoes.setAlignmentX(Component.LEFT_ALIGNMENT);
        botoes.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));
        separador.setAlignmentX(Component.LEFT_ALIGNMENT);

        cabecalho.add(topo);
        cabecalho.add(Box.createVerticalStrut(18));
        cabecalho.add(separador);
        cabecalho.add(Box.createVerticalStrut(16));
        cabecalho.add(botoes);

        return cabecalho;
    }

    private JComponent construirStatsCards() {
        valorUsado     = new JLabel("0 KB");
        valorLivre     = new JLabel("32 MB");
        valorPendentes = new JLabel("0");
        valorHistorico = new JLabel("0");

        JPanel grid = new JPanel(new GridLayout(1, 4, 12, 0));
        grid.setOpaque(false);
        grid.add(statCard("USADO",     valorUsado,     ACCENT));
        grid.add(statCard("LIVRE",     valorLivre,     new Color(16, 185, 129)));
        grid.add(statCard("PENDENTES", valorPendentes, WARNING));
        grid.add(statCard("HISTORICO", valorHistorico, TEXT_SECONDARY));
        return grid;
    }

    // Card pequeno com label maiuscula em cima e valor grande embaixo.
    private JComponent statCard(String label, JLabel valor, Color corValor) {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(PANEL);
        card.setBorder(new CompoundBorder(
                new LineBorder(BORDER, 1, true),
                new EmptyBorder(10, 16, 10, 16)));
        card.setPreferredSize(new Dimension(150, 70));

        JLabel hdr = new JLabel(label);
        hdr.setFont(FONT_LABEL);
        hdr.setForeground(TEXT_MUTED);
        hdr.setAlignmentX(Component.LEFT_ALIGNMENT);

        valor.setFont(FONT_VALUE);
        valor.setForeground(corValor);
        valor.setAlignmentX(Component.LEFT_ALIGNMENT);
        valor.setBorder(new EmptyBorder(4, 0, 0, 0));

        card.add(hdr);
        card.add(valor);
        return card;
    }

    // Corpo central: cards de mapa e arvore a esquerda, fila e buddyinfo a direita.
    private JComponent construirCorpo() {
        painelMapa = new PainelMapa();
        painelArvore = new PainelArvore();

        modeloFila = new DefaultListModel<>();
        JList<String> listaFila = construirJList(modeloFila);

        modeloBuddyinfo = new DefaultListModel<>();
        JList<String> listaBuddy = construirJList(modeloBuddyinfo);

        JPanel viz = new JPanel(new GridBagLayout());
        viz.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0; gbc.gridy = 0;
        gbc.weightx = 1; gbc.weighty = 0.22;
        gbc.fill = GridBagConstraints.BOTH;
        viz.add(card("MAPA DE MEMORIA", scrollSemBorda(painelMapa)), gbc);

        gbc.gridy = 1;
        gbc.weighty = 0.78;
        gbc.insets = new Insets(16, 0, 0, 0);
        viz.add(card("ARVORE RUBRO-NEGRA", scrollSemBorda(painelArvore)), gbc);

        JPanel lateral = new JPanel(new GridBagLayout());
        lateral.setOpaque(false);
        lateral.setPreferredSize(new Dimension(340, 0));
        GridBagConstraints gb = new GridBagConstraints();
        gb.gridx = 0; gb.gridy = 0;
        gb.weightx = 1; gb.weighty = 0.35;
        gb.fill = GridBagConstraints.BOTH;
        lateral.add(card("FILA DE PENDENTES", scrollSemBorda(listaFila)), gb);

        gb.gridy = 1;
        gb.weighty = 0.65;
        gb.insets = new Insets(16, 0, 0, 0);
        lateral.add(card("LISTAS LIVRES (buddyinfo)", scrollSemBorda(listaBuddy)), gb);

        JPanel corpo = new JPanel(new BorderLayout(16, 0));
        corpo.setOpaque(false);
        corpo.add(viz, BorderLayout.CENTER);
        corpo.add(lateral, BorderLayout.EAST);
        return corpo;
    }

    private JComponent construirRodape() {
        areaLog = new JTextArea(7, 80);
        areaLog.setEditable(false);
        areaLog.setFont(FONT_MONO);
        areaLog.setBackground(PANEL);
        areaLog.setForeground(TEXT_PRIMARY);
        areaLog.setMargin(new Insets(6, 8, 6, 8));
        JComponent c = card("HISTORICO DE OPERACOES", scrollSemBorda(areaLog));
        c.setPreferredSize(new Dimension(0, 190));
        return c;
    }

    // Card base: header pequeno em cinza, conteudo abaixo, borda fina.
    private JComponent card(String titulo, JComponent conteudo) {
        JPanel card = new JPanel(new BorderLayout(0, 10));
        card.setBackground(PANEL);
        card.setBorder(new CompoundBorder(
                new LineBorder(BORDER, 1, true),
                new EmptyBorder(14, 16, 14, 16)));
        JLabel header = new JLabel(titulo);
        header.setFont(FONT_LABEL);
        header.setForeground(TEXT_MUTED);
        card.add(header, BorderLayout.NORTH);
        card.add(conteudo, BorderLayout.CENTER);
        return card;
    }

    // Embrulha um componente em JScrollPane sem bordas, com fundo branco.
    private JScrollPane scrollSemBorda(JComponent c) {
        JScrollPane sc = new JScrollPane(c);
        sc.setBorder(null);
        sc.setViewportBorder(null);
        sc.getViewport().setBackground(PANEL);
        return sc;
    }

    private JList<String> construirJList(DefaultListModel<String> modelo) {
        JList<String> lista = new JList<>(modelo);
        lista.setFont(FONT_MONO);
        lista.setBackground(PANEL);
        lista.setForeground(TEXT_PRIMARY);
        lista.setFixedCellHeight(22);
        lista.setSelectionBackground(HOVER_LIGHT);
        lista.setSelectionForeground(TEXT_PRIMARY);
        return lista;
    }

    // Botao de acao primaria: azul forte, texto branco, sem borda.
    private JButton botaoPrimario(String texto) {
        JButton b = new JButton(texto);
        b.setBackground(ACCENT);
        b.setForeground(Color.WHITE);
        b.setFont(FONT_BUTTON);
        b.setBorder(new EmptyBorder(10, 22, 10, 22));
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setOpaque(true);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { b.setBackground(ACCENT_HOVER); }
            @Override public void mouseExited(MouseEvent e)  { b.setBackground(ACCENT); }
        });
        return b;
    }

    // Botao secundario: fundo branco, borda cinza fina.
    private JButton botaoSecundario(String texto) {
        JButton b = new JButton(texto);
        b.setBackground(PANEL);
        b.setForeground(TEXT_PRIMARY);
        b.setFont(FONT_BUTTON);
        b.setBorder(new CompoundBorder(
                new LineBorder(BORDER, 1, true),
                new EmptyBorder(9, 18, 9, 18)));
        b.setFocusPainted(false);
        b.setContentAreaFilled(true);
        b.setOpaque(true);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { b.setBackground(HOVER_LIGHT); }
            @Override public void mouseExited(MouseEvent e)  { b.setBackground(PANEL); }
        });
        return b;
    }

    // Botao destrutivo: texto vermelho, borda rosa clara.
    private JButton botaoDestrutivo(String texto) {
        JButton b = new JButton(texto);
        b.setBackground(PANEL);
        b.setForeground(DANGER);
        b.setFont(FONT_BUTTON);
        b.setBorder(new CompoundBorder(
                new LineBorder(new Color(254, 226, 226), 1, true),
                new EmptyBorder(9, 18, 9, 18)));
        b.setFocusPainted(false);
        b.setContentAreaFilled(true);
        b.setOpaque(true);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        final Color hover = new Color(254, 242, 242);
        b.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { b.setBackground(hover); }
            @Override public void mouseExited(MouseEvent e)  { b.setBackground(PANEL); }
        });
        return b;
    }

    // ===== Acoes dos botoes =====

    private void acaoAlocar() {
        JTextField idField = new JTextField();
        JTextField kbField = new JTextField();
        Object[] msg = {"Identificador:", idField, "Tamanho (KB):", kbField};
        int res = JOptionPane.showConfirmDialog(this, msg, "Alocar",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (res != JOptionPane.OK_OPTION) return;
        String id = idField.getText().trim();
        if (id.isEmpty()) return;
        int kb;
        try { kb = Integer.parseInt(kbField.getText().trim()); }
        catch (NumberFormatException ex) { log(ICO_FAIL + "  Tamanho invalido."); return; }
        boolean ok = alocador.alocar(id, kb);
        log((ok ? ICO_OK : ICO_FILA) + "  ALOCAR   " + id + "   " + kb + " KB"
                + (ok ? "" : "   -> fila"));
        atualizarTudo();
    }

    private void acaoLiberar() {
        String id = JOptionPane.showInputDialog(this,
                "Identificador do bloco a liberar:",
                "Liberar", JOptionPane.PLAIN_MESSAGE);
        if (id == null) return;
        id = id.trim();
        if (id.isEmpty()) return;
        liberarPorId(id);
    }

    private void liberarPorId(String id) {
        boolean ok = alocador.liberar(id);
        log((ok ? ICO_OK : ICO_FAIL) + "  LIBERAR  " + id
                + (ok ? "" : "   (nao encontrado)"));
        atualizarTudo();
    }

    private void acaoDesfazer() {
        boolean ok = alocador.desfazer();
        log(ICO_UNDO + "  " + (ok ? "Operacao desfeita." : "Nada para desfazer."));
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
                        log((ok ? ICO_OK : ICO_FILA) + "  ALOCAR   " + partes[1] + "   " + kb + " KB");
                    } catch (NumberFormatException ex) {
                        log(ICO_FAIL + "  Tamanho invalido: " + linha);
                    }
                } else if (cmd.equals("LIBERAR")) {
                    boolean ok = alocador.liberar(partes[1]);
                    log((ok ? ICO_OK : ICO_FAIL) + "  LIBERAR  " + partes[1]);
                }
                n++;
            }
            log(ICO_INFO + "  Dataset processado: " + n + " operacoes.");
        } catch (IOException ex) {
            log(ICO_FAIL + "  Erro ao ler: " + ex.getMessage());
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
        log(ICO_INFO + "  Memoria reinicializada.");
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
            modeloFila.addElement("  (fila vazia)");
        } else {
            int i = 1;
            while (nf != null) {
                modeloFila.addElement(String.format(" %2d  %-14s %7d KB", i, nf.identificador, nf.tamanhoKB));
                nf = nf.proximo;
                i++;
            }
        }

        modeloBuddyinfo.clear();
        for (int i = 0; i < ListaBlocos.NIVEIS; i++) {
            int t = ListaBlocos.tamanhoDoNivel(i);
            String rot = (t < 1024) ? t + " KB" : (t / 1024) + " MB";
            int c = alocador.getListas().contagemNivel(i);
            String barra = c == 0 ? "" : "  " + repetir('■', c);
            modeloBuddyinfo.addElement(String.format(" %-7s  %3d%s", rot, c, barra));
        }

        int usado = calcularUsado(alocador.getRaiz());
        int livre = AlocadorBuddy.MEMORIA_TOTAL_KB - usado;
        valorUsado.setText(formatarKB(usado));
        valorLivre.setText(formatarKB(livre));
        valorPendentes.setText(String.valueOf(alocador.getPendentes().tamanho()));
        valorHistorico.setText(String.valueOf(alocador.getHistorico().tamanho()));
    }

    private static String repetir(char c, int n) {
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

    // Mistura uma cor com branco para criar tom mais claro (usado em gradientes).
    private static Color clarear(Color c, float ratio) {
        int r = (int) Math.min(255, c.getRed()   + (255 - c.getRed())   * ratio);
        int g = (int) Math.min(255, c.getGreen() + (255 - c.getGreen()) * ratio);
        int b = (int) Math.min(255, c.getBlue()  + (255 - c.getBlue())  * ratio);
        return new Color(r, g, b);
    }

    // Painel que desenha o MAPA HORIZONTAL da memoria.
    // Mostra apenas as folhas (LIVRE em verde, OCUPADO em vermelho).
    // Largura de cada retangulo e proporcional ao tamanho do bloco.
    // Clique em bloco OCUPADO libera; hover mostra tooltip.
    private class PainelMapa extends JPanel implements Scrollable {
        private int areaX, areaY, areaW, areaH;

        public PainelMapa() {
            setBackground(PANEL);
            setOpaque(true);
            ToolTipManager.sharedInstance().registerComponent(this);
            addMouseListener(new MouseAdapter() {
                @Override public void mouseClicked(MouseEvent e) {
                    No alvo = blocoEm(e.getX(), e.getY());
                    if (alvo != null && alvo.estado == EstadoBloco.OCUPADO) {
                        int r = JOptionPane.showConfirmDialog(JanelaPrincipal.this,
                                "Liberar bloco '" + alvo.identificador + "'  (" + formatarKB(alvo.tamanho) + ") ?",
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
            String idTxt = n.identificador == null ? "" : "  id=" + n.identificador;
            return n.estado + "   " + formatarKB(n.tamanho) + idTxt + "   (RB: " + n.cor + ")";
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(900, 100);
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
            areaX = 0;
            areaY = 0;
            areaW = getWidth();
            areaH = getHeight();
            desenharFolhas(g2, alocador.getRaiz(), areaX, areaY, areaW, areaH);
            g2.dispose();
        }

        // Recursao geometrica: desce dividindo a area ao meio em cada no DIVIDIDO.
        // Quando chega numa folha (LIVRE ou OCUPADO), pinta o retangulo com gradiente.
        private void desenharFolhas(Graphics2D g, No no, int x, int y, int w, int h) {
            if (no == null || no.ehNil() || w <= 0) return;
            if (no.estado == EstadoBloco.DIVIDIDO) {
                int meio = w / 2;
                desenharFolhas(g, no.filhoEsquerdo, x, y, meio, h);
                desenharFolhas(g, no.filhoDireito, x + meio, y, w - meio, h);
                return;
            }
            Color base = (no.estado == EstadoBloco.OCUPADO) ? BLOCK_OCUPADO : BLOCK_LIVRE;
            GradientPaint grad = new GradientPaint(
                    x, y, clarear(base, 0.18f),
                    x, y + h, base);
            g.setPaint(grad);
            g.fillRect(x, y, w, h);

            g.setColor(new Color(255, 255, 255, 110));
            g.fillRect(x + w - 1, y, 1, h);

            String texto = (no.estado == EstadoBloco.OCUPADO)
                    ? no.identificador + "  " + formatarKB(no.tamanho)
                    : formatarKB(no.tamanho);
            g.setFont(FONT_BLOCK);
            FontMetrics fm = g.getFontMetrics();
            int tw = fm.stringWidth(texto);
            if (tw < w - 6 && fm.getHeight() < h - 4) {
                g.setColor(Color.WHITE);
                g.drawString(texto, x + (w - tw) / 2, y + (h + fm.getAscent()) / 2 - 2);
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
    private class PainelArvore extends JPanel implements Scrollable {
        private int alturaPreferida = 600;

        public PainelArvore() {
            setBackground(PANEL);
            setOpaque(true);
            ToolTipManager.sharedInstance().registerComponent(this);
            addMouseListener(new MouseAdapter() {
                @Override public void mouseClicked(MouseEvent e) {
                    No alvo = acharNo(alocador.getRaiz(), 0, 0,
                            Math.max(200, getWidth()), 56, e.getX(), e.getY());
                    if (alvo != null && alvo.estado == EstadoBloco.OCUPADO) {
                        int r = JOptionPane.showConfirmDialog(JanelaPrincipal.this,
                                "Liberar '" + alvo.identificador + "'  (" + formatarKB(alvo.tamanho) + ") ?",
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
            No n = acharNo(alocador.getRaiz(), 0, 0,
                    Math.max(200, getWidth()), 56, e.getX(), e.getY());
            if (n == null) return null;
            String idTxt = n.identificador == null ? "" : "  id=" + n.identificador;
            return n.estado + "   " + formatarKB(n.tamanho) + idTxt + "   (RB: " + n.cor + ")";
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int margem = 4;
            int alturaNivel = 56;
            int w = Math.max(200, getWidth() - 2 * margem);
            desenharNo(g2, alocador.getRaiz(), margem, margem, w, alturaNivel);
            g2.dispose();
        }

        // Recursao: desenha o no atual, depois os dois filhos abaixo dele.
        // Cada filho ocupa metade da largura horizontal do pai.
        private void desenharNo(Graphics2D g, No no, int x, int y, int w, int alturaNivel) {
            if (no == null || no.ehNil()) return;
            int h = 38;
            int padX = 4;
            int boxX = x + padX;
            int boxW = Math.max(8, w - 2 * padX);

            Color base;
            switch (no.estado) {
                case OCUPADO: base = BLOCK_OCUPADO;  break;
                case LIVRE:   base = BLOCK_LIVRE;    break;
                default:      base = BLOCK_DIVIDIDO;
            }
            GradientPaint grad = new GradientPaint(
                    boxX, y, clarear(base, 0.20f),
                    boxX, y + h, base);
            g.setPaint(grad);
            g.fillRoundRect(boxX, y, boxW, h, 10, 10);

            Color borda = (no.cor == Cor.VERMELHO) ? RB_VERMELHO : RB_PRETO;
            g.setColor(borda);
            g.setStroke(new BasicStroke(no.cor == Cor.VERMELHO ? 2.4f : 1.4f));
            g.drawRoundRect(boxX, y, boxW, h, 10, 10);

            String texto = formatarKB(no.tamanho) + "   " + no.estado;
            if (no.identificador != null) texto += "   " + no.identificador;
            g.setFont(FONT_BLOCK.deriveFont(12f));
            FontMetrics fm = g.getFontMetrics();
            int tw = fm.stringWidth(texto);
            if (tw < boxW - 12) {
                g.setColor(no.estado == EstadoBloco.DIVIDIDO ? TEXT_PRIMARY : Color.WHITE);
                g.drawString(texto, boxX + (boxW - tw) / 2, y + (h + fm.getAscent()) / 2 - 2);
            }

            boolean temEsq = !no.filhoEsquerdo.ehNil();
            boolean temDir = !no.filhoDireito.ehNil();
            int yFilho = y + alturaNivel;
            int wMeio = w / 2;

            if (temEsq || temDir) {
                g.setColor(TREE_LINE);
                g.setStroke(new BasicStroke(1.2f));
                int xCentro = x + w / 2;
                if (temEsq) g.drawLine(xCentro, y + h, x + wMeio / 2, yFilho);
                if (temDir) g.drawLine(xCentro, y + h, x + wMeio + (w - wMeio) / 2, yFilho);
            }
            if (temEsq) desenharNo(g, no.filhoEsquerdo, x, yFilho, wMeio, alturaNivel);
            if (temDir) desenharNo(g, no.filhoDireito, x + wMeio, yFilho, w - wMeio, alturaNivel);
        }

        private No acharNo(No no, int x, int y, int w, int alturaNivel, int mx, int my) {
            if (no == null || no.ehNil()) return null;
            int h = 38;
            int padX = 4;
            int boxX = x + padX;
            int boxW = Math.max(8, w - 2 * padX);
            if (mx >= boxX && mx < boxX + boxW && my >= y && my < y + h) return no;
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
