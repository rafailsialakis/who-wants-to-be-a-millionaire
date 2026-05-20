package quizgame;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

public class QuizGame extends JFrame {

    // ── Χρώματα / θέμα ────────────────────────────────────────────────────────
    private static final Color BG          = new Color(15, 15, 25);
    private static final Color CARD_BG     = new Color(25, 25, 40);
    private static final Color ACCENT      = new Color(255, 60, 80);
    private static final Color BTN_DEFAULT = new Color(35, 35, 55);
    private static final Color BTN_HOVER   = new Color(55, 55, 85);
    private static final Color BTN_CORRECT = new Color(30, 180, 100);
    private static final Color BTN_WRONG   = new Color(220, 50, 60);
    private static final Color TEXT_MAIN   = new Color(240, 240, 255);
    private static final Color TEXT_DIM    = new Color(150, 150, 180);

    private static final Font FONT_TITLE  = new Font("SansSerif", Font.BOLD, 13);
    private static final Font FONT_Q      = new Font("SansSerif", Font.BOLD, 20);
    private static final Font FONT_BTN    = new Font("SansSerif", Font.PLAIN, 16);
    private static final Font FONT_STATUS = new Font("SansSerif", Font.BOLD, 14);

    // ── State ──────────────────────────────────────────────────────────────────
    private List<Question> questions;
    private int currentIndex = 0;
    private int score        = 0;
    private boolean answered = false;

    // ── UI Components ──────────────────────────────────────────────────────────
    private JLabel  lblProgress, lblScore, lblQuestion;
    private JButton[] optionButtons = new JButton[4];
    private JButton  btnNext;
    private JPanel   mainPanel;

    // ──────────────────────────────────────────────────────────────────────────

    public QuizGame(List<Question> questions) {
        this.questions = new ArrayList<>(questions);
        Collections.shuffle(this.questions);

        setTitle("Quiz Game");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(700, 520);
        setMinimumSize(new Dimension(560, 440));
        setLocationRelativeTo(null);
        setResizable(true);

        buildUI();
        loadQuestion();
        setVisible(true);
    }

    // ── Build UI ───────────────────────────────────────────────────────────────

    private void buildUI() {
        mainPanel = new JPanel(new BorderLayout(0, 0));
        mainPanel.setBackground(BG);
        setContentPane(mainPanel);

        mainPanel.add(buildHeader(), BorderLayout.NORTH);
        mainPanel.add(buildCenter(), BorderLayout.CENTER);
        mainPanel.add(buildFooter(), BorderLayout.SOUTH);
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(CARD_BG);
        header.setBorder(new EmptyBorder(16, 24, 16, 24));

        JLabel title = new JLabel("⚡ QUIZ GAME");
        title.setFont(FONT_TITLE);
        title.setForeground(ACCENT);

        lblProgress = new JLabel();
        lblProgress.setFont(FONT_TITLE);
        lblProgress.setForeground(TEXT_DIM);

        lblScore = new JLabel();
        lblScore.setFont(FONT_STATUS);
        lblScore.setForeground(TEXT_MAIN);

        header.add(title,       BorderLayout.WEST);
        header.add(lblProgress, BorderLayout.CENTER);
        header.add(lblScore,    BorderLayout.EAST);

        // Λεπτή κόκκινη γραμμή κάτω
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(BG);
        wrapper.add(header, BorderLayout.CENTER);
        JSeparator sep = new JSeparator();
        sep.setForeground(ACCENT);
        sep.setBackground(ACCENT);
        wrapper.add(sep, BorderLayout.SOUTH);
        return wrapper;
    }

    private JPanel buildCenter() {
        JPanel center = new JPanel();
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
        center.setBackground(BG);
        center.setBorder(new EmptyBorder(32, 40, 16, 40));

        // Ερώτηση
        lblQuestion = new JLabel("<html><body style='width:500px'></body></html>");
        lblQuestion.setFont(FONT_Q);
        lblQuestion.setForeground(TEXT_MAIN);
        lblQuestion.setAlignmentX(Component.LEFT_ALIGNMENT);
        center.add(lblQuestion);
        center.add(Box.createVerticalStrut(28));

        // Επιλογές A/B/C/D
        String[] labels = {"A", "B", "C", "D"};
        for (int i = 0; i < 4; i++) {
            optionButtons[i] = buildOptionButton(labels[i]);
            final int idx = i;
            optionButtons[i].addActionListener(e -> handleAnswer(idx));
            optionButtons[i].setAlignmentX(Component.LEFT_ALIGNMENT);
            center.add(optionButtons[i]);
            center.add(Box.createVerticalStrut(10));
        }

        return center;
    }

    private JPanel buildFooter() {
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 24, 16));
        footer.setBackground(BG);

        btnNext = new JButton("Επόμενο →");
        btnNext.setFont(FONT_STATUS);
        btnNext.setBackground(ACCENT);
        btnNext.setForeground(Color.WHITE);
        btnNext.setFocusPainted(false);
        btnNext.setBorderPainted(false);
        btnNext.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnNext.setPreferredSize(new Dimension(150, 42));
        btnNext.setEnabled(false);
        btnNext.addActionListener(e -> nextQuestion());

        // Hover effect
        btnNext.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                if (btnNext.isEnabled())
                    btnNext.setBackground(ACCENT.brighter());
            }
            public void mouseExited(MouseEvent e) {
                btnNext.setBackground(ACCENT);
            }
        });

        footer.add(btnNext);
        return footer;
    }

    private JButton buildOptionButton(String label) {
        JButton btn = new JButton() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getBackground());
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.dispose();
                super.paintComponent(g);
            }
            @Override protected void paintBorder(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(80, 80, 120));
                g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 12, 12);
                g2.dispose();
            }
        };
        btn.setOpaque(false);
        btn.setContentAreaFilled(false);
        btn.setFocusPainted(false);
        btn.setBackground(BTN_DEFAULT);
        btn.setForeground(TEXT_MAIN);
        btn.setFont(FONT_BTN);
        btn.setHorizontalAlignment(SwingConstants.LEFT);
        btn.setBorder(new EmptyBorder(12, 18, 12, 18));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 52));

        // Hover
        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                if (!answered) btn.setBackground(BTN_HOVER);
            }
            public void mouseExited(MouseEvent e) {
                if (!answered) btn.setBackground(BTN_DEFAULT);
            }
        });

        return btn;
    }

    // ── Game Logic ─────────────────────────────────────────────────────────────

    private void loadQuestion() {
        if (currentIndex >= questions.size()) {
            showResults();
            return;
        }

        answered = false;
        Question q = questions.get(currentIndex);

        lblProgress.setText("  Ερώτηση " + (currentIndex + 1) + " / " + questions.size());
        lblScore.setText("Σκορ: " + score);
        lblQuestion.setText("<html><body style='width:500px'>" + q.getQuestion() + "</body></html>");

        String[] labels = {"A", "B", "C", "D"};
        String[] options = q.getOptions();
        for (int i = 0; i < 4; i++) {
            optionButtons[i].setText("  " + labels[i] + ".  " + options[i]);
            optionButtons[i].setBackground(BTN_DEFAULT);
            optionButtons[i].setEnabled(true);
        }

        btnNext.setEnabled(false);
    }

    private void handleAnswer(int selectedIndex) {
        if (answered) return;
        answered = true;

        Question q = questions.get(currentIndex);
        boolean correct = q.isCorrect(selectedIndex);

        if (correct) {
            score++;
            optionButtons[selectedIndex].setBackground(BTN_CORRECT);
        } else {
            optionButtons[selectedIndex].setBackground(BTN_WRONG);
            optionButtons[q.getCorrectIndex()].setBackground(BTN_CORRECT);
        }

        // Απενεργοποίηση όλων
        for (JButton btn : optionButtons) btn.setEnabled(false);

        lblScore.setText("Σκορ: " + score);
        btnNext.setEnabled(true);

        // Αν είναι τελευταία ερώτηση, άλλαξε το κείμενο
        if (currentIndex == questions.size() - 1) {
            btnNext.setText("Αποτελέσματα →");
        }
    }

    private void nextQuestion() {
        currentIndex++;
        loadQuestion();
    }

    private void showResults() {
        mainPanel.removeAll();

        JPanel resultPanel = new JPanel();
        resultPanel.setLayout(new BoxLayout(resultPanel, BoxLayout.Y_AXIS));
        resultPanel.setBackground(BG);
        resultPanel.setBorder(new EmptyBorder(60, 60, 60, 60));

        int total = questions.size();
        double pct = (double) score / total * 100;
        String emoji = pct == 100 ? "🏆" : pct >= 70 ? "🎉" : pct >= 40 ? "📚" : "💪";

        JLabel lblEmoji = new JLabel(emoji, SwingConstants.CENTER);
        lblEmoji.setFont(new Font("SansSerif", Font.PLAIN, 56));
        lblEmoji.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel lblTitle = new JLabel("Τέλος Quiz!", SwingConstants.CENTER);
        lblTitle.setFont(new Font("SansSerif", Font.BOLD, 32));
        lblTitle.setForeground(TEXT_MAIN);
        lblTitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel lblResult = new JLabel(
            score + " / " + total + "  (" + String.format("%.0f", pct) + "%)",
            SwingConstants.CENTER
        );
        lblResult.setFont(new Font("SansSerif", Font.BOLD, 22));
        lblResult.setForeground(ACCENT);
        lblResult.setAlignmentX(Component.CENTER_ALIGNMENT);

        String msg = pct == 100 ? "Τέλειο! Απάντησες όλα σωστά!" :
                     pct >= 70  ? "Πολύ καλά! Συνέχισε έτσι!" :
                     pct >= 40  ? "Όχι άσχημα, υπάρχει βελτίωση!" :
                                  "Προσπάθησε ξανά, τα καταφέρεις!";
        JLabel lblMsg = new JLabel(msg, SwingConstants.CENTER);
        lblMsg.setFont(FONT_STATUS);
        lblMsg.setForeground(TEXT_DIM);
        lblMsg.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Κουμπί επανεκκίνησης
        JButton btnRestart = new JButton("🔄  Παίξε ξανά");
        btnRestart.setFont(FONT_STATUS);
        btnRestart.setBackground(ACCENT);
        btnRestart.setForeground(Color.WHITE);
        btnRestart.setFocusPainted(false);
        btnRestart.setBorderPainted(false);
        btnRestart.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnRestart.setPreferredSize(new Dimension(180, 46));
        btnRestart.setMaximumSize(new Dimension(180, 46));
        btnRestart.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnRestart.addActionListener(e -> restartGame());

        resultPanel.add(lblEmoji);
        resultPanel.add(Box.createVerticalStrut(16));
        resultPanel.add(lblTitle);
        resultPanel.add(Box.createVerticalStrut(12));
        resultPanel.add(lblResult);
        resultPanel.add(Box.createVerticalStrut(8));
        resultPanel.add(lblMsg);
        resultPanel.add(Box.createVerticalStrut(32));
        resultPanel.add(btnRestart);

        mainPanel.add(resultPanel, BorderLayout.CENTER);
        mainPanel.revalidate();
        mainPanel.repaint();
    }

    private void restartGame() {
        currentIndex = 0;
        score        = 0;
        Collections.shuffle(questions);

        mainPanel.removeAll();
        buildUI();
        loadQuestion();
        mainPanel.revalidate();
        mainPanel.repaint();
    }

    // ── Entry Point ────────────────────────────────────────────────────────────

    public static void main(String[] args) {
        // Φόρτωση ερωτήσεων
        String jsonPath = "resources/questions.json";
        List<Question> questions;

        try {
            questions = QuestionLoader.loadFromFile(jsonPath);
            if (questions.isEmpty()) {
                JOptionPane.showMessageDialog(null,
                    "Δεν βρέθηκαν ερωτήσεις στο αρχείο:\n" + jsonPath,
                    "Σφάλμα", JOptionPane.ERROR_MESSAGE);
                return;
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null,
                "Αδύνατη φόρτωση αρχείου:\n" + jsonPath + "\n\n" + e.getMessage(),
                "Σφάλμα", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Εκκίνηση GUI στο EDT
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {}
            new QuizGame(questions);
        });
    }
}
