package quizgame;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.*;
import java.util.List;

/**
 * Animated "Who Wants to Be a Millionaire" style main menu.
 */
public class MenuScreen extends JFrame {

    // ── Palette ────────────────────────────────────────────────────────────────
    static final Color BG_DARK   = new Color(5, 10, 40);
    static final Color BG_MID    = new Color(10, 30, 90);
    static final Color GOLD      = new Color(255, 210, 50);
    static final Color GOLD_DIM  = new Color(180, 140, 20);
    static final Color HOT_BLUE  = new Color(0, 160, 255);
    static final Color WHITE     = new Color(245, 245, 255);
    static final Color TEXT_DIM  = new Color(160, 180, 220);

    static final Font FONT_TITLE  = new Font("Georgia", Font.BOLD, 36);
    static final Font FONT_SUB    = new Font("Georgia", Font.ITALIC, 17);
    static final Font FONT_BTN    = new Font("Georgia", Font.BOLD, 18);
    static final Font FONT_SMALL  = new Font("SansSerif", Font.PLAIN, 12);

    // ── Sound ──────────────────────────────────────────────────────────────────
    private SoundManager soundManager;
    private JButton btnMute;

    // ── Animation ──────────────────────────────────────────────────────────────
    private Timer animTimer;
    private float glowPhase = 0f;
    private AnimatedPanel canvas;

    // ── Data ───────────────────────────────────────────────────────────────────
    private List<Question> questions;

    // ──────────────────────────────────────────────────────────────────────────

    public MenuScreen(List<Question> questions) {
        java.net.URL iconUrl = getClass().getClassLoader().getResource("icon.jpg");
        if (iconUrl != null) setIconImage(new ImageIcon(iconUrl).getImage());

        this.questions    = questions;
        this.soundManager = new SoundManager();

        setTitle("Ποιος Θέλει να Γίνει Εκατομμυριούχος;");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(800, 580);
        setMinimumSize(new Dimension(640, 460));
        setLocationRelativeTo(null);
        setResizable(true);

        buildUI();
        startAnimation();
        soundManager.playMenuMusic();
        setVisible(true);
    }

    // ── UI ─────────────────────────────────────────────────────────────────────

    private void buildUI() {
        canvas = new AnimatedPanel();
        canvas.setLayout(new GridBagLayout());
        setContentPane(canvas);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0; gbc.gridy = GridBagConstraints.RELATIVE;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.insets = new Insets(8, 0, 8, 0);

        // ── Title ──────────────────────────────────────────────────────────────
        JLabel lblTitle = new JLabel("ΠΟΙΟΣ ΘΕΛΕΙ ΝΑ ΓΙΝΕΙ");
        lblTitle.setFont(FONT_TITLE);
        lblTitle.setForeground(GOLD);

        JLabel lblTitle2 = new JLabel("ΕΚΑΤΟΜΜΥΡΙΟΥΧΟΣ;");
        lblTitle2.setFont(FONT_TITLE);
        lblTitle2.setForeground(WHITE);

        JLabel lblSub = new JLabel("Το εκπαιδευτικό quiz της σεζόν");
        lblSub.setFont(FONT_SUB);
        lblSub.setForeground(TEXT_DIM);

        // ── Diamond separator ──────────────────────────────────────────────────
        JLabel lblDiamond = new JLabel("*  *  *");
        lblDiamond.setFont(new Font("SansSerif", Font.BOLD, 14));
        lblDiamond.setForeground(GOLD_DIM);

        // ── Buttons ────────────────────────────────────────────────────────────
        JButton btnPlay = buildMenuButton("ΕΝΑΡΞΗ ΠΑΙΧΝΙΔΙΟΥ", GOLD, BG_DARK);
        btnPlay.addActionListener(e -> startGame());

        JButton btnScoreboard = buildMenuButton("ΠΙΝΑΚΑΣ ΑΠΟΤΕΛΕΣΜΑΤΩΝ", HOT_BLUE, BG_DARK);
        btnScoreboard.addActionListener(e -> ScoreboardScreen.show(this));

        JButton btnExit = buildMenuButton("ΕΞΟΔΟΣ", TEXT_DIM, BG_DARK);
        btnExit.addActionListener(e -> System.exit(0));

        // ── Mute button ────────────────────────────────────────────────────────
        btnMute = new JButton("Ηχος ON");
        styleMuteButton(btnMute);
        btnMute.addActionListener(e -> toggleMute());

        // ── Assemble ───────────────────────────────────────────────────────────
        canvas.add(lblTitle,   gbc);
        canvas.add(lblTitle2,  gbc);
        canvas.add(lblSub,     gbc);
        gbc.insets = new Insets(4, 0, 14, 0);
        canvas.add(lblDiamond, gbc);
        gbc.insets = new Insets(6, 0, 6, 0);
        canvas.add(btnPlay, gbc);
        canvas.add(btnScoreboard, gbc);
        canvas.add(btnExit, gbc);
        gbc.insets = new Insets(20, 0, 0, 0);
        canvas.add(btnMute, gbc);
    }

    private JButton buildMenuButton(String text, Color fg, Color bg) {
        JButton btn = new JButton(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color base = getModel().isRollover()
                    ? new Color(HOT_BLUE.getRed(), HOT_BLUE.getGreen(), HOT_BLUE.getBlue(), 60)
                    : new Color(30, 50, 110, 180);
                g2.setColor(base);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 40, 40);
                g2.setColor(fg.darker());
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawRoundRect(1, 1, getWidth()-2, getHeight()-2, 40, 40);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setFont(FONT_BTN);
        btn.setForeground(fg);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setOpaque(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(340, 52));
        return btn;
    }

    private void styleMuteButton(JButton btn) {
        btn.setFont(FONT_SMALL);
        btn.setForeground(TEXT_DIM);
        btn.setBackground(new Color(20, 30, 70));
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createLineBorder(new Color(60, 80, 140)));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(130, 30));
    }

    // ── Animation ──────────────────────────────────────────────────────────────

    private void startAnimation() {
        animTimer = new Timer(30, e -> {
            glowPhase += 0.04f;
            if (glowPhase > Math.PI * 2) glowPhase -= (float)(Math.PI * 2);
            canvas.repaint();
        });
        animTimer.start();
    }

    private class AnimatedPanel extends JPanel {
        AnimatedPanel() { setBackground(BG_DARK); }

        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth(), h = getHeight();

            RadialGradientPaint radial = new RadialGradientPaint(
                new java.awt.geom.Point2D.Float(w / 2f, h / 2f),
                Math.max(w, h) * 0.7f,
                new float[]{0f, 0.5f, 1f},
                new Color[]{BG_MID, BG_DARK, new Color(2, 4, 20)}
            );
            g2.setPaint(radial);
            g2.fillRect(0, 0, w, h);

            float pulse = (float)(0.7 + 0.3 * Math.sin(glowPhase));
            int r = (int)(Math.min(w, h) * 0.44f);
            int cx = w / 2, cy = h / 2;

            for (int i = 5; i >= 1; i--) {
                float alpha = pulse * (0.06f + i * 0.025f);
                g2.setColor(new Color(HOT_BLUE.getRed(), HOT_BLUE.getGreen(), HOT_BLUE.getBlue(),
                    Math.min(255, (int)(alpha * 255))));
                g2.setStroke(new BasicStroke(i == 1 ? 2f : 1f));
                int er = r + i * 22;
                g2.drawOval(cx - er, cy - er / 2, er * 2, er);
            }

            g2.setStroke(new BasicStroke(1.2f));
            g2.setColor(new Color(GOLD.getRed(), GOLD.getGreen(), GOLD.getBlue(), 40));
            for (int i = -3; i <= 3; i++) {
                int off = i * 60;
                g2.drawLine(cx - 300 + off, 0, cx + off, h);
                g2.drawLine(cx + 300 + off, 0, cx + off, h);
            }

            g2.dispose();
        }
    }

    // ── Sound controls ─────────────────────────────────────────────────────────

    private void toggleMute() {
        boolean nowMuted = !soundManager.isMuted();
        soundManager.setMuted(nowMuted);
        btnMute.setText(nowMuted ? "Ηχος OFF" : "Ηχος ON");
    }

    // ── Navigation ─────────────────────────────────────────────────────────────

    private void startGame() {
        animTimer.stop();
        soundManager.stopAll();
        soundManager.playStart();
        dispose();
        SwingUtilities.invokeLater(() -> new MillionaireGame(questions, soundManager));
    }

    // ── Entry point ────────────────────────────────────────────────────────────

    public static void main(String[] args) {
        String jsonPath = "questions.json";
        List<Question> questions;
        try {
            questions = QuestionLoader.loadFromFile(jsonPath);
            if (questions.isEmpty()) {
                JOptionPane.showMessageDialog(null, "Δεν βρέθηκαν ερωτήσεις.", "Σφάλμα", JOptionPane.ERROR_MESSAGE);
                return;
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Αδύνατη φόρτωση:\n" + e.getMessage(), "Σφάλμα", JOptionPane.ERROR_MESSAGE);
            return;
        }

        SwingUtilities.invokeLater(() -> {
            try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
            catch (Exception ignored) {}
            new MenuScreen(questions);
        });
    }
}
