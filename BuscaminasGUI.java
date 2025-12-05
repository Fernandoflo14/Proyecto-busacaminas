import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class BuscaminasGUI extends JFrame {
    /*
     * CONFIGURACIÓN / Puntos fácilmente personalizables
     * El equipo puede cambiar estas constantes para adaptar el aspecto
     * o el comportamiento del tablero sin tocar la lógica interna.
     */
    private final int SIZE = 8; // Tamaño del tablero (8x8)
    private final int NUM_MINAS = 10;

    // Temas y estilos: cambiar aquí para personalizar colores, fuentes, iconos
    private static final Color BOARD_BG = new Color(60, 63, 65);
    private static final Color CELL_BG = new Color(220, 220, 220);
    private static final Color OPEN_CELL_BG = new Color(245, 245, 245);
    private static final Color MINE_BG = new Color(255, 180, 180);
    private static final Font TITLE_FONT = new Font("SansSerif", Font.BOLD, 24);
    private static final Font CELL_FONT = new Font("SansSerif", Font.BOLD, 18);

    // Iconos/literales para bandera y mina — el equipo puede reemplazarlos por imágenes
    // Si usan imágenes, reemplacen estas cadenas por `new ImageIcon("path")` en los puntos indicados.
    private static final String FLAG_TEXT = "⚑";
    private static final String MINE_TEXT = "💣";

    private JButton[][] botones;
    private boolean[][] minas;
    // Contador de celdas abiertas (para calcular puntaje)
    private int openedCells = 0;
    private Ranking rankingPanel; // panel lateral de ranking

    // Constructor
    public BuscaminasGUI() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}

        setTitle("Buscaminas");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(8, 8));

        botones = new JButton[SIZE][SIZE];
        minas = new boolean[SIZE][SIZE];

        // Panel superior con título y reinicio
        JPanel top = new JPanel(new BorderLayout());
        top.setBorder(BorderFactory.createEmptyBorder(8, 8, 0, 8));
        JLabel titulo = new JLabel("Buscaminas");
        // Equipo: pueden cambiar `TITLE_FONT` para ajustar la tipografía global
        titulo.setFont(TITLE_FONT);
        titulo.setHorizontalAlignment(SwingConstants.CENTER);
        JButton reiniciar = new JButton("Reiniciar");
        reiniciar.addActionListener(e -> resetGame());
        top.add(titulo, BorderLayout.CENTER);
        top.add(reiniciar, BorderLayout.EAST);
        add(top, BorderLayout.NORTH);

        // Panel del tablero
        // Panel del tablero: modificar `BOARD_BG` para tema del tablero
        JPanel tablero = new JPanel(new GridLayout(SIZE, SIZE, 4, 4));
        tablero.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        tablero.setBackground(BOARD_BG);

        // Colocar las minas aleatoriamente
        colocarMinas();

        // Panel de ranking a la derecha
        rankingPanel = new Ranking();
        add(rankingPanel.getPanel(), BorderLayout.EAST);

        // Crear los botones
        for (int i = 0; i < SIZE; i++) {
            for (int j = 0; j < SIZE; j++) {
                final int fi = i, fj = j;
                JButton btn = new JButton();
                btn.setPreferredSize(new Dimension(56, 56));
                // Fuente y estilo de celda: cambiar `CELL_FONT` para todo el tablero
                btn.setFont(CELL_FONT);
                btn.setBackground(CELL_BG);
                btn.setFocusPainted(false);
                btn.setOpaque(true);
                btn.setBorder(BorderFactory.createRaisedBevelBorder());

                btn.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseClicked(MouseEvent e) {
                        if (!btn.isEnabled()) return;
                        if (SwingUtilities.isRightMouseButton(e)) {
                            toggleFlag(fi, fj);
                        } else if (SwingUtilities.isLeftMouseButton(e)) {
                            handleLeftClick(fi, fj);
                        }
                    }
                });

                botones[i][j] = btn;
                tablero.add(btn);
            }
        }

        add(tablero, BorderLayout.CENTER);
        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    // Alternar bandera en clic derecho
    private void toggleFlag(int i, int j) {
        JButton btn = botones[i][j];
        if (!btn.isEnabled()) return;
        // Hook de personalización: aquí el equipo puede llamar a su controlador
        // Ejemplo: GameController.onToggleFlag(i, j, btn.getText().equals(FLAG_TEXT));
        String txt = btn.getText();
        if (FLAG_TEXT.equals(txt)) {
            btn.setText("");
            btn.setForeground(Color.BLACK);
        } else {
            btn.setText(FLAG_TEXT);
            btn.setForeground(new Color(200, 30, 30));
        }
    }

    // Manejar clic izquierdo
    private void handleLeftClick(int i, int j) {
        JButton btn = botones[i][j];
        if (minas[i][j]) {
            // Hook: notificar al controlador de evento "mina explotada"
            // Ejemplo: GameController.onMineTriggered(i, j);
            btn.setText(MINE_TEXT);
            btn.setBackground(new Color(255, 102, 102));
            btn.setEnabled(false);
            revelarTodasMinas();
            // Registrar puntaje parcial en ranking
            String nombre = JOptionPane.showInputDialog(this, "Perdiste. Ingresa tu nombre para el ranking:", "Jugador");
            if (nombre != null && !nombre.trim().isEmpty()) {
                rankingPanel.addEntry(nombre.trim(), openedCells);
            }
            JOptionPane.showMessageDialog(this, "¡Perdiste! Has explotado una mina.", "Juego terminado", JOptionPane.INFORMATION_MESSAGE);
            // Hook: aquí se podría llamar a GameController.onGameLost();
        } else {
            int conteo = contarMinasAdjacentes(i, j);
            btn.setText(String.valueOf(conteo));
            btn.setEnabled(false);
            btn.setBackground(OPEN_CELL_BG);
            btn.setBorder(BorderFactory.createLoweredBevelBorder());
            applyNumberColor(btn, conteo);
            openedCells++;
            int safeCells = SIZE * SIZE - NUM_MINAS;
            if (openedCells >= safeCells) {
                // El jugador ganó
                String nombre = JOptionPane.showInputDialog(this, "¡Ganaste! Ingresa tu nombre para el ranking:", "Jugador");
                if (nombre != null && !nombre.trim().isEmpty()) {
                    rankingPanel.addEntry(nombre.trim(), openedCells);
                }
                JOptionPane.showMessageDialog(this, "¡Felicidades! Has ganado.", "Victoria", JOptionPane.INFORMATION_MESSAGE);
            }
            // Hook: notificar al controlador que se abrió una celda
            // Ejemplo: GameController.onCellOpened(i, j, conteo);
            if (conteo == 0) {
                abrirCeldasVacias(i, j);
            }
        }
    }

    private void applyNumberColor(JButton btn, int n) {
        switch (n) {
            case 1: btn.setForeground(new Color(30, 90, 200)); break; // azul
            case 2: btn.setForeground(new Color(10, 120, 10)); break; // verde
            case 3: btn.setForeground(new Color(180, 20, 20)); break; // rojo
            case 4: btn.setForeground(new Color(90, 30, 120)); break; // morado
            default: btn.setForeground(new Color(80, 80, 80)); break;
        }
    }

    // Revelar todas las minas al perder
    /**
     * Revela todas las minas en el tablero. Punto de extensión:
     * - Si el equipo quiere una animación o logging, pueden reemplazar
     *   el cuerpo o llamar a su controlador desde aquí.
     */
    private void revelarTodasMinas() {
        for (int i = 0; i < SIZE; i++) {
            for (int j = 0; j < SIZE; j++) {
                if (minas[i][j]) {
                    // Reemplazar `MINE_TEXT` por imagen si se desea
                    botones[i][j].setText(MINE_TEXT);
                    botones[i][j].setEnabled(false);
                    botones[i][j].setBackground(MINE_BG);
                }
            }
        }
    }

    // Abrir recursivamente celdas vacías
    private void abrirCeldasVacias(int i, int j) {
        for (int di = -1; di <= 1; di++) {
            for (int dj = -1; dj <= 1; dj++) {
                int ni = i + di, nj = j + dj;
                if (ni >= 0 && nj >= 0 && ni < SIZE && nj < SIZE) {
                    JButton nb = botones[ni][nj];
                    if (nb.isEnabled() && !FLAG_TEXT.equals(nb.getText())) {
                        int c = contarMinasAdjacentes(ni, nj);
                        nb.setText(String.valueOf(c));
                        nb.setEnabled(false);
                        applyNumberColor(nb, c);
                        nb.setBackground(OPEN_CELL_BG);
                        nb.setBorder(BorderFactory.createLoweredBevelBorder());
                        if (c == 0) abrirCeldasVacias(ni, nj);
                    }
                }
            }
        }
    }

    // Método para colocar minas aleatoriamente
    private void colocarMinas() {
        // limpiar
        for (int i = 0; i < SIZE; i++) for (int j = 0; j < SIZE; j++) minas[i][j] = false;
        int minasColocadas = 0;
        while (minasColocadas < NUM_MINAS) {
            int fila = (int) (Math.random() * SIZE);
            int col = (int) (Math.random() * SIZE);
            if (!minas[fila][col]) {
                minas[fila][col] = true;
                minasColocadas++;
            }
        }
    }

    // Contar minas adyacentes
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
        for (int i = 0; i < SIZE; i++) {
            for (int j = 0; j < SIZE; j++) {
                JButton b = botones[i][j];
                b.setText("");
                b.setEnabled(true);
                // Restablecer a la apariencia configurada por `CELL_BG`
                b.setBackground(CELL_BG);
                b.setBorder(BorderFactory.createRaisedBevelBorder());
                b.setForeground(Color.BLACK);
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(BuscaminasGUI::new);
    }
}
