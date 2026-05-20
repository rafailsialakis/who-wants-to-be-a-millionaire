package quizgame;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.nio.file.*;
import java.time.*;
import java.time.format.*;
import java.util.*;
import java.util.List;

/**
 * Manages the scoreboard: reads/writes scores to scoreboard.txt
 * and shows a styled leaderboard dialog.
 *
 * File format (one line per entry, pipe-separated):
 *   NAME|PRIZE_LABEL|QUESTIONS_ANSWERED|DATE
 *   e.g.  Νίκος|€32.000|9|2026-05-19
 */
public class Scoreboard {

    static final String FILE = "resources/scoreboard.txt";
    static final String SEP  = "|";

    // ── Data model ─────────────────────────────────────────────────────────────

    public static class Entry implements Comparable<Entry> {
        final String name;
        final String prizeLabel;
        final int    questionsAnswered;
        final String date;

        Entry(String name, String prizeLabel, int questionsAnswered, String date) {
            this.name              = name;
            this.prizeLabel        = prizeLabel;
            this.questionsAnswered = questionsAnswered;
            this.date              = date;
        }

        /** Higher questions answered = better rank */
        @Override public int compareTo(Entry o) {
            return Integer.compare(o.questionsAnswered, this.questionsAnswered);
        }
    }

    // ── File I/O ───────────────────────────────────────────────────────────────

    public static void save(String name, String prizeLabel, int questionsAnswered) {
        String date = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
        String line = name + SEP + prizeLabel + SEP + questionsAnswered + SEP + date;
        try {
            Files.createDirectories(Paths.get("resources"));
            try (BufferedWriter bw = new BufferedWriter(new FileWriter(FILE, true))) {
                bw.write(line);
                bw.newLine();
            }
        } catch (IOException e) {
            System.err.println("Scoreboard write error: " + e.getMessage());
        }
    }

    public static List<Entry> load() {
        List<Entry> entries = new ArrayList<>();
        File f = new File(FILE);
        if (!f.exists()) return entries;
        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                String[] parts = line.split("\\" + SEP, -1);
                if (parts.length < 4) continue;
                try {
                    entries.add(new Entry(parts[0], parts[1],
                        Integer.parseInt(parts[2]), parts[3]));
                } catch (NumberFormatException ignored) {}
            }
        } catch (IOException e) {
            System.err.println("Scoreboard read error: " + e.getMessage());
        }
        Collections.sort(entries);
        return entries;
    }

    // ── UI ─────────────────────────────────────────────────────────────────────

    public static void show(JFrame parent) {
        List<Entry> entries = load();

        JDialog dialog = new JDialog(parent, "🏆 Scoreboard", true);
        dialog.setSize(580, 480);
        dialog.setLocationRelativeTo(parent);
        dialog.setLayout(new BorderLayout(0, 0));
        dialog.getContentPane().setBackground(MenuScreen.BG_DARK);

        // ── Header ─────────────────────────────────────────────────────────────
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(new Color(10, 20, 70));
        header.setBorder(new EmptyBorder(16, 24, 16, 24));

        JLabel lblTitle = new JLabel("🏆  HALL OF FAME");
        lblTitle.setFont(new Font("Georgia", Font.BOLD, 20));
        lblTitle.setForeground(MenuScreen.GOLD);

        JLabel lblSub = new JLabel("Οι καλύτεροι παίκτες");
        lblSub.setFont(new Font("Georgia", Font.ITALIC, 13));
        lblSub.setForeground(MenuScreen.TEXT_DIM);

        JPanel headerText = new JPanel();
        headerText.setOpaque(false);
        headerText.setLayout(new BoxLayout(headerText, BoxLayout.Y_AXIS));
        headerText.add(lblTitle);
        headerText.add(Box.createVerticalStrut(4));
        headerText.add(lblSub);
        header.add(headerText, BorderLayout.WEST);

        dialog.add(header, BorderLayout.NORTH);

        // ── Table ──────────────────────────────────────────────────────────────
        if (entries.isEmpty()) {
            JLabel empty = new JLabel("Δεν υπάρχουν εγγραφές ακόμα. Παίξε πρώτα!", SwingConstants.CENTER);
            empty.setFont(new Font("Georgia", Font.ITALIC, 15));
            empty.setForeground(MenuScreen.TEXT_DIM);
            empty.setBorder(new EmptyBorder(40, 0, 40, 0));
            dialog.add(empty, BorderLayout.CENTER);
        } else {
            JPanel tableWrapper = new JPanel(new BorderLayout());
            tableWrapper.setBackground(MenuScreen.BG_DARK);
            tableWrapper.setBorder(new EmptyBorder(16, 20, 8, 20));

            // Column headers
            JPanel colHeader = buildRow("#", "ΟΝΟΜΑ", "ΒΡΑΒΕΙΟ", "ΕΡΩΤΗΣΕΙΣ", "ΗΜΕΡΟΜΗΝΙΑ", true, -1);
            tableWrapper.add(colHeader, BorderLayout.NORTH);

            JPanel rows = new JPanel();
            rows.setLayout(new BoxLayout(rows, BoxLayout.Y_AXIS));
            rows.setBackground(MenuScreen.BG_DARK);

            int limit = Math.min(entries.size(), 20); // show top 20
            for (int i = 0; i < limit; i++) {
                Entry e  = entries.get(i);
                JPanel row = buildRow(
                    String.valueOf(i + 1),
                    e.name,
                    e.prizeLabel,
                    e.questionsAnswered + " / 15",
                    e.date,
                    false,
                    i
                );
                rows.add(row);
            }

            JScrollPane scroll = new JScrollPane(rows);
            scroll.setBackground(MenuScreen.BG_DARK);
            scroll.getViewport().setBackground(MenuScreen.BG_DARK);
            scroll.setBorder(BorderFactory.createEmptyBorder());
            tableWrapper.add(scroll, BorderLayout.CENTER);
            dialog.add(tableWrapper, BorderLayout.CENTER);
        }

        // ── Footer ─────────────────────────────────────────────────────────────
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 20, 12));
        footer.setBackground(new Color(10, 20, 70));

        JButton btnClose = new JButton("Κλείσιμο");
        btnClose.setFont(new Font("Georgia", Font.BOLD, 13));
        btnClose.setBackground(new Color(30, 80, 200));
        btnClose.setForeground(Color.WHITE);
        btnClose.setFocusPainted(false);
        btnClose.setBorderPainted(false);
        btnClose.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnClose.setPreferredSize(new Dimension(120, 36));
        btnClose.addActionListener(e -> dialog.dispose());
        footer.add(btnClose);
        dialog.add(footer, BorderLayout.SOUTH);

        dialog.setVisible(true);
    }

    // ── Row builder ────────────────────────────────────────────────────────────

    private static JPanel buildRow(String rank, String name, String prize,
                                   String questions, String date,
                                   boolean isHeader, int rowIndex) {
        JPanel row = new JPanel(new GridLayout(1, 5, 0, 0));
        Color bg = isHeader
            ? new Color(15, 30, 100)
            : (rowIndex % 2 == 0 ? new Color(18, 25, 65) : new Color(12, 18, 50));
        row.setBackground(bg);
        row.setBorder(new EmptyBorder(isHeader ? 10 : 8, 12, isHeader ? 10 : 8, 12));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, isHeader ? 40 : 36));

        Color fg   = isHeader ? MenuScreen.GOLD : MenuScreen.WHITE;
        Font  font = isHeader
            ? new Font("SansSerif", Font.BOLD, 12)
            : new Font("SansSerif", Font.PLAIN, 13);

        // Medal for top 3
        String rankDisplay = rank;
        if (!isHeader) {
            if ("1".equals(rank)) rankDisplay = "🥇";
            else if ("2".equals(rank)) rankDisplay = "🥈";
            else if ("3".equals(rank)) rankDisplay = "🥉";
        }

        // Prize color
        Color prizeColor = isHeader ? MenuScreen.GOLD
            : (prize.contains("1.000.000") ? new Color(255, 215, 0)
             : prize.contains("000")       ? new Color(100, 220, 130)
             : MenuScreen.WHITE);

        row.add(makeCell(rankDisplay, font, isHeader ? fg : MenuScreen.TEXT_DIM, SwingConstants.CENTER));
        row.add(makeCell(name,        font, fg,         SwingConstants.LEFT));
        row.add(makeCell(prize,       font, prizeColor, SwingConstants.CENTER));
        row.add(makeCell(questions,   font, fg,         SwingConstants.CENTER));
        row.add(makeCell(date,        font, MenuScreen.TEXT_DIM, SwingConstants.CENTER));

        return row;
    }

    private static JLabel makeCell(String text, Font font, Color fg, int align) {
        JLabel lbl = new JLabel(text, align);
        lbl.setFont(font);
        lbl.setForeground(fg);
        lbl.setOpaque(false);
        return lbl;
    }

    // ── Name input dialog ──────────────────────────────────────────────────────

    /**
     * Shows a styled name-input dialog. Returns the entered name,
     * or "Ανώνυμος" if the user cancels or leaves it blank.
     */
    public static String askName(JFrame parent) {
        JDialog dialog = new JDialog(parent, "Καταχώρηση Αποτελέσματος", true);
        dialog.setSize(400, 220);
        dialog.setLocationRelativeTo(parent);
        dialog.setLayout(new BorderLayout(0, 0));
        dialog.getContentPane().setBackground(new Color(12, 20, 65));
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBackground(new Color(12, 20, 65));
        body.setBorder(new EmptyBorder(24, 32, 16, 32));

        JLabel lbl = new JLabel("Βάλε το όνομά σου για το Scoreboard:");
        lbl.setFont(new Font("Georgia", Font.BOLD, 14));
        lbl.setForeground(MenuScreen.WHITE);
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);

        JTextField field = new JTextField();
        field.setFont(new Font("Georgia", Font.PLAIN, 16));
        field.setBackground(new Color(25, 40, 100));
        field.setForeground(MenuScreen.WHITE);
        field.setCaretColor(MenuScreen.WHITE);
        field.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(60, 100, 200)),
            new EmptyBorder(6, 10, 6, 10)
        ));
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        field.setAlignmentX(Component.LEFT_ALIGNMENT);

        body.add(lbl);
        body.add(Box.createVerticalStrut(14));
        body.add(field);

        // Footer buttons
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 12));
        footer.setBackground(new Color(8, 14, 50));

        final String[] result = {"Ανώνυμος"};

        JButton btnOk = new JButton("Αποθήκευση");
        btnOk.setFont(new Font("SansSerif", Font.BOLD, 13));
        btnOk.setBackground(MenuScreen.GOLD);
        btnOk.setForeground(Color.BLACK);
        btnOk.setFocusPainted(false);
        btnOk.setBorderPainted(false);
        btnOk.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnOk.setPreferredSize(new Dimension(130, 36));
        btnOk.addActionListener(e -> {
            String name = field.getText().trim();
            result[0] = name.isEmpty() ? "Ανώνυμος" : name;
            dialog.dispose();
        });

        JButton btnSkip = new JButton("Παράλειψη");
        btnSkip.setFont(new Font("SansSerif", Font.PLAIN, 13));
        btnSkip.setBackground(new Color(40, 50, 90));
        btnSkip.setForeground(MenuScreen.TEXT_DIM);
        btnSkip.setFocusPainted(false);
        btnSkip.setBorderPainted(false);
        btnSkip.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnSkip.setPreferredSize(new Dimension(110, 36));
        btnSkip.addActionListener(e -> {
            result[0] = null; // null = don't save
            dialog.dispose();
        });

        // Enter key submits
        field.addActionListener(e -> btnOk.doClick());

        footer.add(btnSkip);
        footer.add(btnOk);

        dialog.add(body,   BorderLayout.CENTER);
        dialog.add(footer, BorderLayout.SOUTH);
        dialog.setVisible(true);

        return result[0]; // null if skipped
    }
}
