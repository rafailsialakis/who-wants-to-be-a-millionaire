package quizgame;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;

/**
 * Custom result dialog — replaces JOptionPane in MillionaireGame.showResults().
 * Returns: 0 = Νέο παιχνίδι, 1 = Μενού, 2 = Έξοδος
 */
public class ResultScreen extends JDialog {

    private static final Color BG_DARK  = MenuScreen.BG_DARK;
    private static final Color BG_MID   = MenuScreen.BG_MID;
    private static final Color GOLD     = MenuScreen.GOLD;
    private static final Color GOLD_DIM = MenuScreen.GOLD_DIM;
    private static final Color HOT_BLUE = MenuScreen.HOT_BLUE;
    private static final Color WHITE    = MenuScreen.WHITE;
    private static final Color TEXT_DIM = MenuScreen.TEXT_DIM;
    private static final Color CORRECT  = new Color(30, 200, 100);
    private static final Color WRONG    = new Color(220, 40, 50);

    private int result = 2; // default: exit
    private String savedName = null;
    private int    savedQ    = -1;

    private ResultScreen(JFrame owner, boolean won, String prize, String safeHaven, int questionsAnswered) {
        super(owner, "Αποτέλεσμα", true);

        // Load icon from classpath so it works in JAR too
        java.net.URL iconUrl = getClass().getClassLoader().getResource("icon.jpg");
        if (iconUrl != null) setIconImage(new ImageIcon(iconUrl).getImage());

        setUndecorated(true);
        setSize(520, 460);
        setLocationRelativeTo(owner);

        // ── Animated background ────────────────────────────────────────────────
        final float[] phase = {0f};

        JPanel bg = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth(), h = getHeight();

                RadialGradientPaint rp = new RadialGradientPaint(
                    new Point2D.Float(w / 2f, h / 2f),
                    Math.max(w, h) * 0.75f,
                    new float[]{0f, 0.55f, 1f},
                    new Color[]{BG_MID, BG_DARK, new Color(2, 4, 20)}
                );
                g2.setPaint(rp);
                g2.fillRect(0, 0, w, h);

                float pulse = (float)(0.6 + 0.4 * Math.sin(phase[0]));
                int cx = w / 2, cy = h / 2;
                Color ringColor = won ? GOLD : HOT_BLUE;
                for (int i = 4; i >= 1; i--) {
                    int alpha = Math.min(255, (int)(pulse * (35 + i * 20)));
                    g2.setColor(new Color(ringColor.getRed(), ringColor.getGreen(), ringColor.getBlue(), alpha));
                    g2.setStroke(new BasicStroke(i == 1 ? 2f : 1f));
                    int er = 80 + i * 26;
                    g2.drawOval(cx - er, cy - er / 2, er * 2, er);
                }

                g2.setColor(won ? GOLD_DIM : new Color(40, 60, 140));
                g2.setStroke(new BasicStroke(2f));
                g2.drawRect(1, 1, w - 2, h - 2);
                g2.dispose();
            }
        };
        bg.setBackground(BG_DARK);

        Timer anim = new Timer(30, e -> { phase[0] += 0.05f; bg.repaint(); });
        anim.start();
        addWindowListener(new WindowAdapter() {
            public void windowClosed(WindowEvent e) { anim.stop(); }
        });

        // ── Content ────────────────────────────────────────────────────────────
        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setOpaque(false);
        content.setBorder(new EmptyBorder(36, 60, 32, 60));

        JLabel lblEmoji = new JLabel(won ? "🏆" : "❌", SwingConstants.CENTER);
        lblEmoji.setFont(new Font("SansSerif", Font.PLAIN, 54));
        lblEmoji.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel lblTitle = new JLabel(won ? "ΣΥΓΧΑΡΗΤΗΡΙΑ!" : "ΛΑΘΟΣ ΑΠΑΝΤΗΣΗ!", SwingConstants.CENTER);
        lblTitle.setFont(new Font("Georgia", Font.BOLD, 26));
        lblTitle.setForeground(won ? GOLD : WRONG);
        lblTitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        String prizeText = won
            ? "Κερδίσατε: " + prize
            : "Φεύγετε με: " + safeHaven + "  (ασφαλής βαθμίδα)";
        JLabel lblPrize = new JLabel(prizeText, SwingConstants.CENTER);
        lblPrize.setFont(new Font("Georgia", Font.BOLD, 18));
        lblPrize.setForeground(won ? WHITE : TEXT_DIM);
        lblPrize.setAlignmentX(Component.CENTER_ALIGNMENT);

        String subText = won
            ? "Απαντήσατε σωστά σε όλες τις ερωτήσεις!"
            : "Η σωστή απάντηση εμφανίστηκε παραπάνω.";
        JLabel lblSub = new JLabel(subText, SwingConstants.CENTER);
        lblSub.setFont(new Font("Georgia", Font.ITALIC, 13));
        lblSub.setForeground(TEXT_DIM);
        lblSub.setAlignmentX(Component.CENTER_ALIGNMENT);

        // ── Player name input ──────────────────────────────────────────────────
        JPanel nameRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 0));
        nameRow.setOpaque(false);
        nameRow.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel lblNamePrompt = new JLabel("Όνομα:");
        lblNamePrompt.setFont(new Font("Georgia", Font.BOLD, 13));
        lblNamePrompt.setForeground(TEXT_DIM);

        JTextField nameField = new JTextField("Παίκτης", 14);
        nameField.setFont(new Font("Georgia", Font.PLAIN, 13));
        nameField.setBackground(new Color(15, 25, 80));
        nameField.setForeground(WHITE);
        nameField.setCaretColor(WHITE);
        nameField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(60, 90, 200), 1),
            BorderFactory.createEmptyBorder(4, 8, 4, 8)
        ));
        nameField.setHorizontalAlignment(JTextField.CENTER);
        // Select all on focus so user can just start typing
        nameField.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent e) { nameField.selectAll(); }
        });

        nameRow.add(lblNamePrompt);
        nameRow.add(nameField);

        // ── Navigation buttons ─────────────────────────────────────────────────
        JPanel navRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        navRow.setOpaque(false);
        navRow.setAlignmentX(Component.CENTER_ALIGNMENT);

        JButton btnScoreboard = buildBtn("🏆  Scoreboard",   new Color(60, 45, 0),     GOLD);
        JButton btnRestart    = buildBtn("🔄  Νέο παιχνίδι", new Color(30, 80, 200),   WHITE);
        JButton btnMenu       = buildBtn("🏠  Μενού",         new Color(20, 50, 120),   GOLD);
        JButton btnExit       = buildBtn("✕  Έξοδος",         new Color(80, 20, 20),    new Color(220, 80, 80));

        // Helper: save score then perform action — only saves once
        final boolean[] scoreSaved = {false};
        Runnable saveScore = () -> {
            if (scoreSaved[0]) return;
            scoreSaved[0] = true;
            String name = nameField.getText().trim();
            if (name.isEmpty()) name = "Ανώνυμος";
            savedName = name;
            savedQ    = questionsAnswered;
            nameField.setEditable(false);
            ScoreboardManager.addAndSave(name, questionsAnswered, prize);
        };

        btnScoreboard.addActionListener(e -> {
            saveScore.run();
            ScoreboardScreen.show((JFrame) getOwner(), savedName, savedQ);
        });
        btnRestart.addActionListener(e -> { saveScore.run(); result = 0; anim.stop(); dispose(); });
        btnMenu   .addActionListener(e -> { saveScore.run(); result = 1; anim.stop(); dispose(); });
        btnExit   .addActionListener(e -> { saveScore.run(); result = 2; anim.stop(); dispose(); });

        navRow.add(btnScoreboard);
        navRow.add(btnRestart);
        navRow.add(btnMenu);
        navRow.add(btnExit);

        content.add(lblEmoji);
        content.add(Box.createVerticalStrut(6));
        content.add(lblTitle);
        content.add(Box.createVerticalStrut(8));
        content.add(lblPrize);
        content.add(Box.createVerticalStrut(4));
        content.add(lblSub);
        content.add(Box.createVerticalStrut(14));
        content.add(nameRow);
        content.add(Box.createVerticalStrut(14));
        content.add(navRow);

        bg.add(content, BorderLayout.CENTER);
        setContentPane(bg);
    }

    private JButton buildBtn(String text, Color bgColor, Color fgColor) {
        JButton btn = new JButton(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getModel().isRollover() ? bgColor.brighter() : bgColor);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 28, 28);
                g2.setColor(fgColor.darker());
                g2.setStroke(new BasicStroke(1.2f));
                g2.drawRoundRect(1, 1, getWidth()-2, getHeight()-2, 28, 28);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setFont(new Font("Georgia", Font.BOLD, 13));
        btn.setForeground(fgColor);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setOpaque(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(160, 40));
        return btn;
    }

    /**
     * Shows the result dialog and returns the user's choice:
     *   0 = Νέο παιχνίδι
     *   1 = Μενού
     *   2 = Έξοδος
     */
    public static int show(JFrame owner, boolean won, String prize, String safeHaven, int questionsAnswered) {
        ResultScreen dlg = new ResultScreen(owner, won, prize, safeHaven, questionsAnswered);
        dlg.setVisible(true);
        return dlg.result;
    }
}
