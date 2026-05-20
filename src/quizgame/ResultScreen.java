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

    private ResultScreen(JFrame owner, boolean won, String prize, String safeHaven) {
        super(owner, "Αποτέλεσμα", true);
        setUndecorated(true);
        setSize(540, 400);
        setLocationRelativeTo(owner);

        // ── Animated background ────────────────────────────────────────────────
        final float[] phase = {0f};
        final boolean[] animRunning = {true};

        JPanel bg = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth(), h = getHeight();

                // Radial gradient
                RadialGradientPaint rp = new RadialGradientPaint(
                    new Point2D.Float(w / 2f, h / 2f),
                    Math.max(w, h) * 0.75f,
                    new float[]{0f, 0.55f, 1f},
                    new Color[]{BG_MID, BG_DARK, new Color(2, 4, 20)}
                );
                g2.setPaint(rp);
                g2.fillRect(0, 0, w, h);

                // Pulsing concentric rings
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

                // Outer border
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
        content.setBorder(new EmptyBorder(44, 60, 36, 60));

        // Emoji
        JLabel lblEmoji = new JLabel(won ? "🏆" : "❌", SwingConstants.CENTER);
        lblEmoji.setFont(new Font("SansSerif", Font.PLAIN, 54));
        lblEmoji.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Title
        JLabel lblTitle = new JLabel(won ? "ΣΥΓΧΑΡΗΤΗΡΙΑ!" : "ΛΑΘΟΣ ΑΠΑΝΤΗΣΗ!", SwingConstants.CENTER);
        lblTitle.setFont(new Font("Georgia", Font.BOLD, 26));
        lblTitle.setForeground(won ? GOLD : WRONG);
        lblTitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Prize
        String prizeText = won
            ? "Κερδίσατε: " + prize
            : "Φεύγετε με: " + safeHaven + "  (ασφαλής βαθμίδα)";
        JLabel lblPrize = new JLabel(prizeText, SwingConstants.CENTER);
        lblPrize.setFont(new Font("Georgia", Font.BOLD, 18));
        lblPrize.setForeground(won ? WHITE : TEXT_DIM);
        lblPrize.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Subtitle
        String subText = won
            ? "Απαντήσατε σωστά σε όλες τις ερωτήσεις!"
            : "Η σωστή απάντηση εμφανίστηκε παραπάνω.";
        JLabel lblSub = new JLabel(subText, SwingConstants.CENTER);
        lblSub.setFont(new Font("Georgia", Font.ITALIC, 13));
        lblSub.setForeground(TEXT_DIM);
        lblSub.setAlignmentX(Component.CENTER_ALIGNMENT);

        // ── Buttons ────────────────────────────────────────────────────────────
        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 14, 0));
        btnRow.setOpaque(false);
        btnRow.setAlignmentX(Component.CENTER_ALIGNMENT);

        JButton btnRestart = buildBtn("🔄  Νέο παιχνίδι", new Color(30, 80, 200),  WHITE);
        JButton btnMenu    = buildBtn("🏠  Μενού",         new Color(20, 50, 120),  GOLD);
        JButton btnExit    = buildBtn("✕  Έξοδος",         new Color(80, 20, 20),   new Color(220, 80, 80));

        btnRestart.addActionListener(e -> { result = 0; anim.stop(); dispose(); });
        btnMenu   .addActionListener(e -> { result = 1; anim.stop(); dispose(); });
        btnExit   .addActionListener(e -> { result = 2; anim.stop(); dispose(); });

        btnRow.add(btnRestart);
        btnRow.add(btnMenu);
        btnRow.add(btnExit);

        content.add(lblEmoji);
        content.add(Box.createVerticalStrut(8));
        content.add(lblTitle);
        content.add(Box.createVerticalStrut(10));
        content.add(lblPrize);
        content.add(Box.createVerticalStrut(5));
        content.add(lblSub);
        content.add(Box.createVerticalStrut(30));
        content.add(btnRow);

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
        btn.setPreferredSize(new Dimension(148, 40));
        return btn;
    }

    /**
     * Shows the result dialog and returns the user's choice:
     *   0 = Νέο παιχνίδι
     *   1 = Μενού
     *   2 = Έξοδος
     */
    public static int show(JFrame owner, boolean won, String prize, String safeHaven) {
        ResultScreen dlg = new ResultScreen(owner, won, prize, safeHaven);
        dlg.setVisible(true); // blocks until disposed
        return dlg.result;
    }
}