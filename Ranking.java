import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Clase sencilla de ranking que mantiene una lista de jugadores y puntajes
 * y muestra un panel con la tabla correspondiente. Guarda/lee un archivo
 * `ranking.csv` en la carpeta del proyecto para persistencia básica.
 */
public class Ranking {
	private final List<Entry> entries = new ArrayList<>();
	private final DefaultTableModel tableModel;
	private final JPanel panel;
	private final Path storageFile = Path.of("ranking.csv");

	public Ranking() {
		tableModel = new DefaultTableModel(new Object[]{"Nombre", "Puntaje"}, 0) {
			@Override
			public boolean isCellEditable(int row, int column) {
				return false;
			}
		};
		JTable table = new JTable(tableModel);
		table.setFillsViewportHeight(true);
		JScrollPane scroll = new JScrollPane(table);
		scroll.setPreferredSize(new Dimension(200, 300));

		panel = new JPanel(new BorderLayout(6,6));
		JLabel title = new JLabel("Ranking");
		title.setHorizontalAlignment(SwingConstants.CENTER);
		title.setFont(new Font("SansSerif", Font.BOLD, 16));
		panel.add(title, BorderLayout.NORTH);
		panel.add(scroll, BorderLayout.CENTER);

		loadFromFile();
	}

	public JPanel getPanel() {
		return panel;
	}

	public void addEntry(String nombre, int puntaje) {
		entries.add(new Entry(nombre, puntaje));
		tableModel.addRow(new Object[]{nombre, puntaje});
		saveToFile();
	}

	public void clear() {
		entries.clear();
		tableModel.setRowCount(0);
		saveToFile();
	}

	private void loadFromFile() {
		try {
			if (!Files.exists(storageFile)) return;
			List<String> lines = Files.readAllLines(storageFile);
			for (String line : lines) {
				String[] parts = line.split(",");
				if (parts.length >= 2) {
					String nombre = parts[0];
					int puntaje = Integer.parseInt(parts[1]);
					entries.add(new Entry(nombre, puntaje));
					tableModel.addRow(new Object[]{nombre, puntaje});
				}
			}
		} catch (Exception e) {
			System.err.println("No se pudo leer ranking: " + e.getMessage());
		}
	}

	private void saveToFile() {
		try (BufferedWriter w = Files.newBufferedWriter(storageFile)) {
			for (Entry e : entries) {
				w.write(e.nombre + "," + e.puntaje + "\n");
			}
		} catch (Exception ex) {
			System.err.println("No se pudo guardar ranking: " + ex.getMessage());
		}
	}

	private static class Entry {
		String nombre;
		int puntaje;

		Entry(String n, int p) {
			nombre = n;
			puntaje = p;
		}
	}
}
