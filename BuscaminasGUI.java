import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Random;

public class BuscaminasGUI extends JFrame {

    // ===== NIVELES (más estilo Google: más minas) =====
    private enum Nivel {
        FACIL("Fácil", 8, 10),
        INTERMEDIO("Intermedio", 10, 18),
        DIFICIL("Difícil", 12, 28);

        final String nombre;
        final int size;
        final int minas;
        Nivel(String n, int s, int m) { nombre = n; size = s; minas = m; }
        @Override public String toString() { return nombre; }
    }

    private int SIZE = 8;
    private int NUM_MINAS = 10;
    private Nivel nivelActual = Nivel.FACIL;

    // ===== TEMA CELESTE (estético) =====
    private static final Color APP_BG = new Color(233, 245, 255);        // fondo general
    private static final Color TOP_BG = new Color(210, 236, 255);        // barra superior
    private static final Color BOARD_BG = new Color(200, 225, 245);      // fondo tablero

    private static final Color CELL_CLOSED_BG = new Color(245, 250, 255); // celda tapada
    private static final Color CELL_HOVER_BG  = new Color(230, 244, 255); // hover
    private static final Color CELL_OPEN_BG   = new Color(214, 232, 245); // celda abierta

    private static final Color CELL_BORDER    = new Color(170, 200, 220); // borde suave
    private static final Color FLAG_COLOR     = new Color(200, 60, 60);
    private static final Color TEXT_SOFT      = new Color(70, 80, 90);

    private static final Color MINE_BG = new Color(255, 190, 190);

    private static final Font TITLE_FONT = new Font("SansSerif", Font.BOLD, 24);
    private static final Font CELL_FONT  = new Font("SansSerif", Font.BOLD, 18);

    private static final String FLAG_TEXT = "⚑";
    private static final String MINE_TEXT = "💣";

    private JButton[][] botones;
    private boolean[][] minas;

    private int openedCells = 0;
    private Ranking rankingPanel;
    private boolean gameOver = false;

    private JPanel tablero;
    private JComboBox<Nivel> comboNivel;

    private final Random rnd = new Random();

    public BuscaminasGUI() {
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
        catch (Exception ignored) {}

        setTitle("Buscaminas");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(8, 8));

        // Fondo general
        getContentPane().setBackground(APP_BG);

        // ===== TOP BAR =====
        JPanel top = new JPanel(new BorderLayout());
        top.setBorder(BorderFactory.createEmptyBorder(10, 12, 8, 12));
        top.setBackground(TOP_BG);

        JLabel titulo = new JLabel("Buscaminas");
        titulo.setFont(TITLE_FONT);
        titulo.setForeground(TEXT_SOFT);
        titulo.setHorizontalAlignment(SwingConstants.CENTER);
        top.add(titulo, BorderLayout.CENTER);

        JPanel rightTop = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        rightTop.setBackground(TOP_BG);

        comboNivel = new JComboBox<>(Nivel.values());
        comboNivel.setSelectedItem(nivelActual);
        comboNivel.setFocusable(false);

        comboNivel.addActionListener(e -> {
            Nivel seleccionado = (Nivel) comboNivel.getSelectedItem();
            if (seleccionado != null) cambiarNivel(seleccionado);
        });

        JButton reiniciar = new JButton("Reiniciar");
        reiniciar.setFocusable(false);
        reiniciar.addActionListener(e -> resetGame());

        JLabel modoLbl = new JLabel("Modo:");
        modoLbl.setForeground(TEXT_SOFT);

        rightTop.add(modoLbl);
        rightTop.add(comboNivel);
        rightTop.add(reiniciar);

        top.add(rightTop, BorderLayout.EAST);
        add(top, BorderLayout.NORTH);

        rankingPanel = new Ranking();
        add(rankingPanel.getPanel(), BorderLayout.EAST);

        cambiarNivel(nivelActual);

        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    // ===== ESTILOS DE CELDA =====
    private void styleCellClosed(JButton b) {
        b.setBackground(CELL_CLOSED_BG);
        b.setForeground(TEXT_SOFT);
        b.setFocusPainted(false);
        b.setOpaque(true);
        b.setContentAreaFilled(true);
        b.setBorder(BorderFactory.createLineBorder(CELL_BORDER, 2, true)); // redondeado
        b.setMargin(new Insets(0, 0, 0, 0));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    private void styleCellOpen(JButton b) {
        b.setBackground(CELL_OPEN_BG);
        b.setBorder(BorderFactory.createLineBorder(CELL_BORDER, 2, true));
        b.setCursor(Cursor.getDefaultCursor());
    }

    private void styleCellMine(JButton b) {
        b.setBackground(MINE_BG);
        b.setBorder(BorderFactory.createLineBorder(CELL_BORDER, 2, true));
    }

    private void cambiarNivel(Nivel nivel) {
        this.nivelActual = nivel;
        this.SIZE = nivel.size;
        this.NUM_MINAS = nivel.minas;

        openedCells = 0;
        gameOver = false;

        if (tablero != null) remove(tablero);

        botones = new JButton[SIZE][SIZE];
        minas = new boolean[SIZE][SIZE];

        tablero = new JPanel(new GridLayout(SIZE, SIZE, 6, 6)); // más aire entre casillas
        tablero.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        tablero.setBackground(BOARD_BG);

        colocarMinas();

        for (int i = 0; i < SIZE; i++) {
            for (int j = 0; j < SIZE; j++) {
                final int fi = i, fj = j;

                JButton btn = new JButton();
                btn.setPreferredSize(new Dimension(56, 56));
                btn.setFont(CELL_FONT);
                styleCellClosed(btn);

                btn.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseClicked(MouseEvent e) {
                        if (gameOver || !btn.isEnabled()) return;

                        if (SwingUtilities.isRightMouseButton(e)) {
                            toggleFlag(fi, fj);
                        } else if (SwingUtilities.isLeftMouseButton(e)) {
                            handleLeftClick(fi, fj);
                        }
                    }

                    @Override
                    public void mouseEntered(MouseEvent e) {
                        if (!gameOver && btn.isEnabled() && !FLAG_TEXT.equals(btn.getText())) {
                            btn.setBackground(CELL_HOVER_BG);
                        }
                    }

                    @Override
                    public void mouseExited(MouseEvent e) {
                        if (btn.isEnabled() && !FLAG_TEXT.equals(btn.getText())) {
                            btn.setBackground(CELL_CLOSED_BG);
                        }
                    }
                });

                botones[i][j] = btn;
                tablero.add(btn);
            }
        }

        add(tablero, BorderLayout.CENTER);
        revalidate();
        repaint();
        pack();
        setLocationRelativeTo(null);
    }

    private void toggleFlag(int i, int j) {
        JButton btn = botones[i][j];
        if (!btn.isEnabled()) return;

        String txt = btn.getText();
        if (FLAG_TEXT.equals(txt)) {
            btn.setText("");
            btn.setForeground(TEXT_SOFT);
            btn.setBackground(CELL_CLOSED_BG);
        } else {
            btn.setText(FLAG_TEXT);
            btn.setForeground(FLAG_COLOR);
            btn.setBackground(CELL_CLOSED_BG);
        }
        checkWin();
    }

    private void handleLeftClick(int i, int j) {
        JButton btn = botones[i][j];

        if (minas[i][j]) {
            btn.setText(MINE_TEXT);
            styleCellMine(btn);
            btn.setEnabled(false);

            revelarTodasMinas();
            gameOver = true;

            String nombre = JOptionPane.showInputDialog(this,
                    "Perdiste. Ingresa tu nombre para el ranking:", "Jugador");
            if (nombre != null && !nombre.trim().isEmpty()) {
                rankingPanel.addEntry(nombre.trim(), openedCells);
            }

            JOptionPane.showMessageDialog(this,
                    "¡Perdiste! Has explotado una mina.", "Juego terminado",
                    JOptionPane.INFORMATION_MESSAGE);

        } else {
            int conteo = contarMinasAdjacentes(i, j);

            // 0 = BLANCO
            if (conteo == 0) {
                btn.setText("");
                btn.setForeground(TEXT_SOFT);
            } else {
                btn.setText(String.valueOf(conteo));
                applyNumberColor(btn, conteo);
            }

            btn.setEnabled(false);
            styleCellOpen(btn);

            openedCells++;
            checkWin();

            if (conteo == 0) {
                int[] presupuesto = { 9 - 1 };
                abrirCeldasVaciasLimitado(i, j, presupuesto);
            }
        }
    }

    private void checkWin() {
        int safeCells = SIZE * SIZE - NUM_MINAS;
        boolean ganoPorAbrir = (openedCells >= safeCells);

        int banderasCorrectas = 0;
        int banderasIncorrectas = 0;

        for (int i = 0; i < SIZE; i++) {
            for (int j = 0; j < SIZE; j++) {
                boolean hayBandera = FLAG_TEXT.equals(botones[i][j].getText());
                if (hayBandera && minas[i][j]) banderasCorrectas++;
                if (hayBandera && !minas[i][j]) banderasIncorrectas++;
            }
        }

        boolean ganoPorBanderas = (banderasCorrectas == NUM_MINAS) && (banderasIncorrectas == 0);

        if (!gameOver && (ganoPorAbrir || ganoPorBanderas)) {
            gameOver = true;

            for (int i = 0; i < SIZE; i++) {
                for (int j = 0; j < SIZE; j++) {
                    botones[i][j].setEnabled(false);
                }
            }

            String nombre = JOptionPane.showInputDialog(this,
                    "¡Ganaste! Ingresa tu nombre para el ranking:", "Jugador");

            if (nombre != null && !nombre.trim().isEmpty()) {
                rankingPanel.addEntry(nombre.trim(), openedCells);
            }

            JOptionPane.showMessageDialog(this,
                    "¡Felicidades! Has ganado.", "Victoria",
                    JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void applyNumberColor(JButton btn, int n) {
        // tonos más suaves (se ven más “clean”)
        switch (n) {
            case 1: btn.setForeground(new Color(30, 120, 200)); break;  // celeste
            case 2: btn.setForeground(new Color(30, 160, 90)); break;   // verde
            case 3: btn.setForeground(new Color(220, 120, 40)); break;  // naranja
            case 4: btn.setForeground(new Color(60, 90, 220)); break;   // azul
            case 5: btn.setForeground(new Color(160, 70, 200)); break;  // morado
            default: btn.setForeground(TEXT_SOFT); break;
        }
    }

    private void revelarTodasMinas() {
        for (int i = 0; i < SIZE; i++) {
            for (int j = 0; j < SIZE; j++) {
                if (minas[i][j]) {
                    botones[i][j].setText(MINE_TEXT);
                    botones[i][j].setEnabled(false);
                    styleCellMine(botones[i][j]);
                }
            }
        }
    }

    private void abrirCeldasVaciasLimitado(int i, int j, int[] presupuesto) {
        if (presupuesto[0] <= 0) return;

        for (int di = -1; di <= 1; di++) {
            for (int dj = -1; dj <= 1; dj++) {

                if (presupuesto[0] <= 0) return;

                int ni = i + di, nj = j + dj;

                if (ni >= 0 && nj >= 0 && ni < SIZE && nj < SIZE) {
                    if (minas[ni][nj]) continue;

                    JButton nb = botones[ni][nj];

                    if (nb.isEnabled() && !FLAG_TEXT.equals(nb.getText())) {

                        int c = contarMinasAdjacentes(ni, nj);

                        if (c == 0) {
                            nb.setText("");
                            nb.setForeground(TEXT_SOFT);
                        } else {
                            nb.setText(String.valueOf(c));
                            applyNumberColor(nb, c);
                        }

                        nb.setEnabled(false);
                        styleCellOpen(nb);

                        openedCells++;
                        checkWin();

                        presupuesto[0]--;
                        if (presupuesto[0] <= 0) return;

                        if (c == 0) {
                            abrirCeldasVaciasLimitado(ni, nj, presupuesto);
                        }
                    }
                }
            }
        }
    }

    // ===== Colocar minas (misma lógica tuya para favorecer 2/3) =====
    private void colocarMinas() {
        int intentos = 1500 + (SIZE * SIZE);
        boolean[][] mejor = null;
        int mejorScore = Integer.MIN_VALUE;

        for (int t = 0; t < intentos; t++) {
            boolean[][] cand = generarMinasRandom();
            int score = puntuarTablero(cand);

            if (score > mejorScore) {
                mejorScore = score;
                mejor = cand;
            }
        }

        for (int i = 0; i < SIZE; i++) {
            for (int j = 0; j < SIZE; j++) {
                minas[i][j] = (mejor != null && mejor[i][j]);
            }
        }
    }

    private boolean[][] generarMinasRandom() {
        boolean[][] cand = new boolean[SIZE][SIZE];
        int colocadas = 0;

        while (colocadas < NUM_MINAS) {
            int r = rnd.nextInt(SIZE);
            int c = rnd.nextInt(SIZE);
            if (!cand[r][c]) {
                cand[r][c] = true;
                colocadas++;
            }
        }
        return cand;
    }

    private int puntuarTablero(boolean[][] cand) {
        int score = 0;

        for (int i = 0; i < SIZE; i++) {
            for (int j = 0; j < SIZE; j++) {
                if (cand[i][j]) continue;

                int adj = contarAdjCon(cand, i, j);

                if (adj == 2 || adj == 3) score += 6;
                else if (adj == 4) score += 2;
                else if (adj == 5) score += 1;
                else if (adj == 1) score -= 5;
                else if (adj == 0) score -= 3;
            }
        }

        return score;
    }

    private int contarAdjCon(boolean[][] cand, int i, int j) {
        int cnt = 0;
        for (int di = -1; di <= 1; di++) {
            for (int dj = -1; dj <= 1; dj++) {
                int ni = i + di, nj = j + dj;
                if (ni >= 0 && nj >= 0 && ni < SIZE && nj < SIZE) {
                    if (cand[ni][nj]) cnt++;
                }
            }
        }
        return cnt;
    }

    private int contarMinasAdjacentes(int i, int j) {
        int c = 0;
        for (int di = -1; di <= 1; di++) {
            for (int dj = -1; dj <= 1; dj++) {
                int ni = i + di, nj = j + dj;
                if (ni >= 0 && nj >= 0 && ni < SIZE && nj < SIZE) {
                    if (minas[ni][nj]) c++;
                }
            }
        }
        return c;
    }

    private void resetGame() {
        colocarMinas();
        openedCells = 0;
        gameOver = false;

        for (int i = 0; i < SIZE; i++) {
            for (int j = 0; j < SIZE; j++) {
                JButton b = botones[i][j];
                b.setText("");
                b.setEnabled(true);
                styleCellClosed(b);
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(BuscaminasGUI::new);
    }
}
