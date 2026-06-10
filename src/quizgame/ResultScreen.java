package quizgame;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;

/**
 * Custom result dialog — replaces JOptionPane in MillionaireGame.showResults().
 * Returns: 0 = Νέο παιχνίδι, 1 = Μενού, 2 = Έξοδος
 *
 * Score-saving logic:
 *   - The player types their name and clicks "Υποβολή" explicitly.
 *   - After submission the name field and submit button are locked (disabled).
 *   - Navigation buttons (Νέο παιχνίδι, Μενού, Έξοδος) never trigger a save.
 *   - The Scoreboard button is only enabled after the score has been saved.
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

    private int    result     = 2;      // default: exit
    private String savedName  = null;
    private int    savedQ     = -1;
    private boolean scoreSaved = false; // true once the player clicks "Υποβολή"

    private ResultScreen(JFrame owner, boolean won, String prize, String safeHaven, int questionsAnswered) {
        super(owner, "Αποτέλεσμα", true);

        java.net.URL iconUrl = getClass().getClassLoader().getResource("icon.jpg");
        if (iconUrl != null) setIconImage(new ImageIcon(iconUrl).getImage());

        setUndecorated(true);
        setSize(520, 490);
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
        content.setBorder(new EmptyBorder(30, 60, 28, 60));

        // Result icon + title
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

        // ── Player name + submit row ───────────────────────────────────────────
        JPanel nameRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 0));
        nameRow.setOpaque(false);
        nameRow.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel lblNamePrompt = new JLabel("Όνομα:");
        lblNamePrompt.setFont(new Font("Georgia", Font.BOLD, 13));
        lblNamePrompt.setForeground(TEXT_DIM);

        JTextField nameField = new JTextField("Παίκτης", 13);
        nameField.setFont(new Font("Georgia", Font.PLAIN, 13));
        nameField.setBackground(new Color(15, 25, 80));
        nameField.setForeground(WHITE);
        nameField.setCaretColor(WHITE);
        nameField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(60, 90, 200), 1),
            BorderFactory.createEmptyBorder(4, 8, 4, 8)
        ));
        nameField.setHorizontalAlignment(JTextField.CENTER);
        nameField.addFocusListener(new FocusAdapter() {
            public void focusGained(FocusEvent e) { nameField.selectAll(); }
        });

        // Small submit button
        JButton btnSubmit = new JButton("Υποβολή") {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color bg2 = isEnabled()
                    ? (getModel().isRollover() ? new Color(20, 150, 70) : new Color(15, 110, 50))
                    : new Color(30, 40, 60);
                g2.setColor(bg2);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
                g2.setColor(isEnabled() ? CORRECT.darker() : new Color(50, 60, 80));
                g2.setStroke(new BasicStroke(1.2f));
                g2.drawRoundRect(1, 1, getWidth()-2, getHeight()-2, 20, 20);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btnSubmit.setFont(new Font("Georgia", Font.BOLD, 12));
        btnSubmit.setForeground(WHITE);
        btnSubmit.setContentAreaFilled(false);
        btnSubmit.setBorderPainted(false);
        btnSubmit.setFocusPainted(false);
        btnSubmit.setOpaque(false);
        btnSubmit.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnSubmit.setPreferredSize(new Dimension(100, 30));

        // Status label shown after submission
        JLabel lblStatus = new JLabel("", SwingConstants.CENTER);
        lblStatus.setFont(new Font("Georgia", Font.ITALIC, 12));
        lblStatus.setForeground(CORRECT);
        lblStatus.setAlignmentX(Component.CENTER_ALIGNMENT);

        nameRow.add(lblNamePrompt);
        nameRow.add(nameField);
        nameRow.add(btnSubmit);

        // ── Navigation buttons ─────────────────────────────────────────────────
        JPanel navRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        navRow.setOpaque(false);
        navRow.setAlignmentX(Component.CENTER_ALIGNMENT);

        JButton btnScoreboard = buildBtn("Scoreboard",   new Color(60, 45, 0),   GOLD);
        JButton btnRestart    = buildBtn("Νέο παιχνίδι", new Color(30, 80, 200), WHITE);
        JButton btnMenu       = buildBtn("Μενού",         new Color(20, 50, 120), GOLD);
        JButton btnExit       = buildBtn("✕  Έξοδος",         new Color(80, 20, 20),  new Color(220, 80, 80));

        // Scoreboard button starts disabled — enabled only after score is saved
        btnScoreboard.setEnabled(false);
        btnScoreboard.setForeground(new Color(120, 100, 40)); // dimmed until saved

        // ── Submit action ──────────────────────────────────────────────────────
        Runnable doSubmit = () -> {
            if (scoreSaved) return;
            scoreSaved = true;

            String name = nameField.getText().trim();
            if (name.isEmpty()) name = "Ανώνυμος";
            savedName = name;
            savedQ    = questionsAnswered;

            // Lock the name field and submit button
            nameField.setEditable(false);
            nameField.setBackground(new Color(10, 18, 55));
            nameField.setForeground(new Color(140, 160, 200));
            btnSubmit.setEnabled(false);

            // Persist score
            ScoreboardManager.addAndSave(name, questionsAnswered, prize);

            // Update status label and enable scoreboard button
            lblStatus.setText("✔ Αποθηκεύτηκε ως «" + name + "»");
            btnScoreboard.setEnabled(true);
            btnScoreboard.setForeground(GOLD);
            btnScoreboard.repaint();
        };

        btnSubmit.addActionListener(e -> doSubmit.run());
        // Allow Enter key in the name field to trigger submit
        nameField.addActionListener(e -> doSubmit.run());

        // Navigation — no saving, just navigate
        btnScoreboard.addActionListener(e -> {
            if (scoreSaved) ScoreboardScreen.show((JFrame) getOwner(), savedName, savedQ);
        });
        btnRestart.addActionListener(e -> { result = 0; anim.stop(); dispose(); });
        btnMenu   .addActionListener(e -> { result = 1; anim.stop(); dispose(); });
        btnExit   .addActionListener(e -> { result = 2; anim.stop(); dispose(); });

        navRow.add(btnScoreboard);
        navRow.add(btnRestart);
        navRow.add(btnMenu);
        navRow.add(btnExit);

        // ── Assemble ───────────────────────────────────────────────────────────
        content.add(lblEmoji);
        content.add(Box.createVerticalStrut(6));
        content.add(lblTitle);
        content.add(Box.createVerticalStrut(8));
        content.add(lblPrize);
        content.add(Box.createVerticalStrut(4));
        content.add(lblSub);
        content.add(Box.createVerticalStrut(14));
        content.add(nameRow);
        content.add(Box.createVerticalStrut(4));
        content.add(lblStatus);
        content.add(Box.createVerticalStrut(12));
        content.add(navRow);

        bg.add(content, BorderLayout.CENTER);
        setContentPane(bg);
    }

    private JButton buildBtn(String text, Color bgColor, Color fgColor) {
        JButton btn = new JButton(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color base = !isEnabled()
                    ? new Color(20, 25, 50)
                    : (getModel().isRollover() ? bgColor.brighter() : bgColor);
                g2.setColor(base);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 28, 28);
                g2.setColor(getForeground().darker());
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
        btn.setPreferredSize(new Dimension(155, 40));
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
