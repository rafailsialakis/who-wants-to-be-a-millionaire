package quizgame;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.geom.*;
import java.util.List;

/**
 * Animated scoreboard dialog — matches the "Who Wants to Be a Millionaire" dark theme.
 * Call ScoreboardScreen.show(owner) to display it modally.
 * Call ScoreboardScreen.show(owner, highlightName, highlightQ) to highlight a new entry.
 */
public class ScoreboardScreen extends JDialog {

    // ── Palette (reuse MenuScreen constants) ────────────────────────────────────
    private static final Color BG_DARK  = MenuScreen.BG_DARK;
    private static final Color BG_MID   = MenuScreen.BG_MID;
    private static final Color GOLD     = MenuScreen.GOLD;
    private static final Color GOLD_DIM = MenuScreen.GOLD_DIM;
    private static final Color HOT_BLUE = MenuScreen.HOT_BLUE;
    private static final Color WHITE    = MenuScreen.WHITE;
    private static final Color TEXT_DIM = MenuScreen.TEXT_DIM;

    private static final Color ROW_ODD      = new Color(12, 20, 65);
    private static final Color ROW_EVEN     = new Color(8, 14, 48);
    private static final Color ROW_HIGHLIGHT= new Color(35, 80, 20);
    private static final Color ROW_TOP3     = new Color(50, 35, 0);
    private static final Color CORRECT      = new Color(30, 200, 100);

    private static final Font FONT_TITLE = new Font("Georgia", Font.BOLD, 22);
    private static final Font FONT_HDR   = new Font("Georgia", Font.BOLD, 13);
    private static final Font FONT_ROW   = new Font("Georgia", Font.PLAIN, 13);
    private static final Font FONT_MEDAL = new Font("SansSerif", Font.PLAIN, 15);

    // ── Animation ───────────────────────────────────────────────────────────────
    private final float[] phase = {0f};
    private Timer animTimer;

    // ──────────────────────────────────────────────────────────────────────────

    private ScoreboardScreen(JFrame owner, String highlightName, int highlightQ) {
        super(owner, "Scoreboard", true);

        java.net.URL iconUrl = getClass().getClassLoader().getResource("icon.jpg");
        if (iconUrl != null) setIconImage(new ImageIcon(iconUrl).getImage());

        setUndecorated(true);
        setSize(620, 560);
        setLocationRelativeTo(owner);

        List<ScoreboardManager.Entry> entries = ScoreboardManager.load();

        JPanel bg = buildAnimBg();
        bg.setLayout(new BorderLayout());

        // ── Header ─────────────────────────────────────────────────────────────
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.setBorder(new EmptyBorder(22, 32, 10, 32));

        JLabel lblTitle = new JLabel("ΠΙΝΑΚΑΣ ΑΠΟΤΕΛΕΣΜΑΤΩΝ", SwingConstants.CENTER);
        lblTitle.setFont(FONT_TITLE);
        lblTitle.setForeground(GOLD);
        header.add(lblTitle, BorderLayout.CENTER);

        JLabel lblSub = new JLabel("Ταξινόμηση κατά αριθμό σωστών απαντήσεων", SwingConstants.CENTER);
        lblSub.setFont(new Font("Georgia", Font.ITALIC, 12));
        lblSub.setForeground(TEXT_DIM);
        header.add(lblSub, BorderLayout.SOUTH);

        // ── Table ──────────────────────────────────────────────────────────────
        JPanel tableWrap = new JPanel(new BorderLayout());
        tableWrap.setOpaque(false);
        tableWrap.setBorder(new EmptyBorder(4, 28, 8, 28));

        JPanel tableHeader = buildTableHeader();
        tableWrap.add(tableHeader, BorderLayout.NORTH);

        JPanel rows = new JPanel();
        rows.setLayout(new BoxLayout(rows, BoxLayout.Y_AXIS));
        rows.setOpaque(false);

        if (entries.isEmpty()) {
            JLabel empty = new JLabel("Δεν υπάρχουν εγγραφές ακόμα.", SwingConstants.CENTER);
            empty.setFont(new Font("Georgia", Font.ITALIC, 14));
            empty.setForeground(TEXT_DIM);
            empty.setAlignmentX(Component.CENTER_ALIGNMENT);
            rows.add(Box.createVerticalStrut(30));
            rows.add(empty);
        } else {
            for (int i = 0; i < entries.size(); i++) {
                ScoreboardManager.Entry e = entries.get(i);
                boolean isHighlight = (highlightName != null)
                    && e.name.equals(highlightName)
                    && e.questionsAnswered == highlightQ;
                rows.add(buildRow(i + 1, e, isHighlight));
            }
        }

        JScrollPane scroll = new JScrollPane(rows,
            JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
            JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setBorder(BorderFactory.createLineBorder(new Color(40, 60, 140), 1));
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        styleScrollBar(scroll);

        tableWrap.add(scroll, BorderLayout.CENTER);

        // ── Footer ─────────────────────────────────────────────────────────────
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.CENTER));
        footer.setOpaque(false);
        footer.setBorder(new EmptyBorder(6, 0, 18, 0));

        JButton btnClose = buildBtn("✕   Κλείσιμο", new Color(30, 50, 130), WHITE);
        btnClose.addActionListener(e -> { animTimer.stop(); dispose(); });
        footer.add(btnClose);

        bg.add(header,    BorderLayout.NORTH);
        bg.add(tableWrap, BorderLayout.CENTER);
        bg.add(footer,    BorderLayout.SOUTH);
        setContentPane(bg);

        // scroll to highlighted row
        if (highlightName != null && !entries.isEmpty()) {
            SwingUtilities.invokeLater(() -> {
                int idx = ScoreboardManager.rankOf(entries, highlightName, highlightQ) - 1;
                if (idx >= 0 && idx < rows.getComponentCount()) {
                    Rectangle r = rows.getComponent(idx).getBounds();
                    rows.scrollRectToVisible(r);
                }
            });
        }
    }

    // ── Animated background ────────────────────────────────────────────────────

    private JPanel buildAnimBg() {
        JPanel bg = new JPanel() {
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
                for (int i = 5; i >= 1; i--) {
                    int alpha = Math.min(255, (int)(pulse * (20 + i * 15)));
                    g2.setColor(new Color(GOLD.getRed(), GOLD.getGreen(), GOLD.getBlue(), alpha));
                    g2.setStroke(new BasicStroke(i == 1 ? 1.8f : 1f));
                    int er = 100 + i * 30;
                    g2.drawOval(cx - er, cy - er / 2, er * 2, er);
                }

                // border
                g2.setColor(GOLD_DIM);
                g2.setStroke(new BasicStroke(2f));
                g2.drawRect(1, 1, w - 2, h - 2);
                g2.dispose();
            }
        };
        bg.setBackground(BG_DARK);

        animTimer = new Timer(30, e -> { phase[0] += 0.04f; bg.repaint(); });
        animTimer.start();
        return bg;
    }

    // ── Table header row ───────────────────────────────────────────────────────

    private JPanel buildTableHeader() {
        JPanel hdr = new JPanel(new GridLayout(1, 5, 0, 0));
        hdr.setBackground(new Color(15, 25, 80));
        hdr.setBorder(new EmptyBorder(6, 4, 6, 4));

        String[] cols = {"#", "Όνομα", "Ερωτήσεις", "Έπαθλο", "Ημερομηνία"};
        int[]    wts  = {8, 28, 18, 22, 24};

        for (int i = 0; i < cols.length; i++) {
            JLabel lbl = new JLabel(cols[i], SwingConstants.CENTER);
            lbl.setFont(FONT_HDR);
            lbl.setForeground(GOLD);
            hdr.add(lbl);
        }
        return hdr;
    }

    // ── A single score row ─────────────────────────────────────────────────────

    private JPanel buildRow(int rank, ScoreboardManager.Entry e, boolean highlight) {
        JPanel row = new JPanel(new GridLayout(1, 5, 0, 0));

        Color base;
        if      (highlight)  base = ROW_HIGHLIGHT;
        else if (rank <= 3)  base = ROW_TOP3;
        else if (rank % 2 == 0) base = ROW_EVEN;
        else                 base = ROW_ODD;

        row.setBackground(base);
        row.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(30, 45, 100)),
            new EmptyBorder(5, 6, 5, 6)
        ));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));

        // Rank / medal
        String rankTxt = rank == 1 ? "🥇" : rank == 2 ? "🥈" : rank == 3 ? "🥉" : String.valueOf(rank);
        JLabel lblRank = label(rankTxt, rank <= 3 ? FONT_MEDAL : FONT_ROW,
                               rank <= 3 ? GOLD : TEXT_DIM, base);

        // Name
        Color nameFg = highlight ? CORRECT : (rank <= 3 ? GOLD : WHITE);
        JLabel lblName = label(e.name, FONT_ROW, nameFg, base);

        // Questions answered
        JLabel lblQ = label(String.valueOf(e.questionsAnswered), FONT_ROW,
                            highlight ? CORRECT : WHITE, base);

        // Prize
        JLabel lblPrize = label(e.prize, FONT_ROW, highlight ? CORRECT : TEXT_DIM, base);

        // Date
        JLabel lblDate = label(e.date, new Font("SansSerif", Font.PLAIN, 11),
                               new Color(120, 140, 180), base);

        row.add(lblRank);
        row.add(lblName);
        row.add(lblQ);
        row.add(lblPrize);
        row.add(lblDate);
        return row;
    }

    private JLabel label(String text, Font font, Color fg, Color bg) {
        JLabel lbl = new JLabel(text, SwingConstants.CENTER);
        lbl.setFont(font);
        lbl.setForeground(fg);
        lbl.setBackground(bg);
        lbl.setOpaque(true);
        return lbl;
    }

    // ── Styled close button ────────────────────────────────────────────────────

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

    // ── Scroll bar styling ─────────────────────────────────────────────────────

    private void styleScrollBar(JScrollPane scroll) {
        JScrollBar vsb = scroll.getVerticalScrollBar();
        vsb.setBackground(new Color(10, 18, 55));
        vsb.setUI(new javax.swing.plaf.basic.BasicScrollBarUI() {
            @Override protected void configureScrollBarColors() {
                thumbColor     = new Color(60, 90, 200);
                trackColor     = new Color(10, 18, 55);
            }
            @Override protected JButton createDecreaseButton(int o) { return zeroBtn(); }
            @Override protected JButton createIncreaseButton(int o) { return zeroBtn(); }
            private JButton zeroBtn() {
                JButton b = new JButton();
                b.setPreferredSize(new Dimension(0,0));
                return b;
            }
        });
    }

    // ── Static factory ─────────────────────────────────────────────────────────

    /** Show scoreboard without highlighting (from main menu). */
    public static void show(JFrame owner) {
        new ScoreboardScreen(owner, null, -1).setVisible(true);
    }

    /** Show scoreboard and highlight the player's new entry. */
    public static void show(JFrame owner, String playerName, int questionsAnswered) {
        new ScoreboardScreen(owner, playerName, questionsAnswered).setVisible(true);
    }
}
