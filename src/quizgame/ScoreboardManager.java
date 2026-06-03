package quizgame;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Manages scoreboard entries: load, save, and sort by questions answered.
 * Scores are persisted to a plain CSV file (scores.csv) next to the JAR/classes.
 */
public class ScoreboardManager {

    private static final String FILE_NAME       = "scores.csv";
    private static final int    MAX_ENTRIES     = 20;
    private static final DateTimeFormatter FMT  =
        DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    // ── Entry ──────────────────────────────────────────────────────────────────

    public static class Entry implements Comparable<Entry> {
        public final String name;
        public final int    questionsAnswered;
        public final String prize;
        public final String date;

        public Entry(String name, int questionsAnswered, String prize, String date) {
            this.name              = name;
            this.questionsAnswered = questionsAnswered;
            this.prize             = prize;
            this.date              = date;
        }

        @Override
        public int compareTo(Entry o) {
            // Descending by questionsAnswered; ties broken by name alphabetically
            int cmp = Integer.compare(o.questionsAnswered, this.questionsAnswered);
            return (cmp != 0) ? cmp : this.name.compareToIgnoreCase(o.name);
        }

        /** CSV line: name,questionsAnswered,prize,date */
        public String toCsv() {
            return escapeCsv(name) + "," + questionsAnswered + "," +
                   escapeCsv(prize) + "," + escapeCsv(date);
        }

        private static String escapeCsv(String s) {
            if (s == null) return "";
            if (s.contains(",") || s.contains("\"") || s.contains("\n")) {
                return "\"" + s.replace("\"", "\"\"") + "\"";
            }
            return s;
        }
    }

    // ── Public API ─────────────────────────────────────────────────────────────

    /** Loads existing scores from disk, sorted best-first. */
    public static List<Entry> load() {
        List<Entry> list = new ArrayList<>();
        File f = getFile();
        if (!f.exists()) return list;

        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                String[] parts = splitCsvLine(line);
                if (parts.length < 4) continue;
                try {
                    list.add(new Entry(parts[0], Integer.parseInt(parts[1]), parts[2], parts[3]));
                } catch (NumberFormatException ignored) {}
            }
        } catch (IOException e) {
            System.err.println("[ScoreboardManager] Cannot read scores: " + e.getMessage());
        }

        Collections.sort(list);
        return list;
    }

    /**
     * Adds a new entry, prunes to MAX_ENTRIES, and persists to disk.
     * Returns the sorted list (including the new entry).
     */
    public static List<Entry> addAndSave(String playerName, int questionsAnswered, String prize) {
        List<Entry> list = load();
        String date = LocalDateTime.now().format(FMT);
        list.add(new Entry(playerName, questionsAnswered, prize, date));
        Collections.sort(list);
        if (list.size() > MAX_ENTRIES) list = list.subList(0, MAX_ENTRIES);

        File f = getFile();
        try (PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(f)))) {
            for (Entry e : list) pw.println(e.toCsv());
        } catch (IOException e) {
            System.err.println("[ScoreboardManager] Cannot write scores: " + e.getMessage());
        }

        return list;
    }

    /** Returns rank (1-based) of the most recently added entry in the sorted list. */
    public static int rankOf(List<Entry> sorted, String name, int questionsAnswered) {
        for (int i = 0; i < sorted.size(); i++) {
            Entry e = sorted.get(i);
            if (e.name.equals(name) && e.questionsAnswered == questionsAnswered) return i + 1;
        }
        return -1;
    }

    // ── Private helpers ────────────────────────────────────────────────────────

    private static File getFile() {
        return new File(FILE_NAME);
    }

    /** Minimal CSV line splitter supporting quoted fields. */
    private static String[] splitCsvLine(String line) {
        List<String> tokens = new ArrayList<>();
        StringBuilder sb    = new StringBuilder();
        boolean inQuotes    = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
                        sb.append('"'); i++;
                    } else {
                        inQuotes = false;
                    }
                } else {
                    sb.append(c);
                }
            } else {
                if (c == '"') {
                    inQuotes = true;
                } else if (c == ',') {
                    tokens.add(sb.toString()); sb.setLength(0);
                } else {
                    sb.append(c);
                }
            }
        }
        tokens.add(sb.toString());
        return tokens.toArray(new String[0]);
    }
}
