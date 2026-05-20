package quizgame;

import javax.swing.*;
import javax.swing.Timer;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

/**
 * Main game screen — Who Wants to Be a Millionaire style.
 * Features: 50-50 lifeline, Phone-an-LLM lifeline, prize ladder, dark cinematic theme.
 *
 * Answer flow:
 *   1. Player clicks option  → button turns PENDING (bright blue outline)
 *   2. playAnswerSuspense()  → suspense music starts, all buttons locked
 *   3. After SUSPENSE_MS ms → reveal: correct=green, wrong=red + playCorrect/playWrong
 *   4. resumeBackground()   → game music resumes
 */
public class MillionaireGame extends JFrame {

    // ── Palette ────────────────────────────────────────────────────────────────
    private static final Color BG       = MenuScreen.BG_DARK;
    private static final Color CARD     = new Color(12, 20, 65);
    private static final Color GOLD     = MenuScreen.GOLD;
    private static final Color GOLD_DIM = MenuScreen.GOLD_DIM;
    private static final Color HOT_BLUE = MenuScreen.HOT_BLUE;
    private static final Color WHITE    = MenuScreen.WHITE;
    private static final Color TEXT_DIM = MenuScreen.TEXT_DIM;
    private static final Color CORRECT  = new Color(30, 200, 100);
    private static final Color WRONG    = new Color(220, 40, 50);
    private static final Color ELIM     = new Color(50, 50, 80);
    private static final Color PENDING  = new Color(30, 90, 220);   // ← suspense highlight

    private static final Font FONT_Q     = new Font("Georgia", Font.BOLD, 18);
    private static final Font FONT_BTN   = new Font("Georgia", Font.PLAIN, 15);
    private static final Font FONT_LIFE  = new Font("SansSerif", Font.BOLD, 13);
    private static final Font FONT_PRIZE = new Font("SansSerif", Font.PLAIN, 12);
    private static final Font FONT_LABEL = new Font("Georgia", Font.BOLD, 13);

    /** How long (ms) the suspense music plays before the answer is revealed. */
    private static final int SUSPENSE_MS = 3000;

    // ── Prize ladder ───────────────────────────────────────────────────────────
    private static final String[] PRIZES = {
        "€100", "€200", "€300", "€500", "€1.000",
        "€2.000", "€4.000", "€8.000", "€16.000", "€32.000",
        "€64.000", "€125.000", "€250.000", "€500.000", "€1.000.000"
    };
    private static final int[] SAFE_HAVENS = {4, 9};

    // ── State ──────────────────────────────────────────────────────────────────
    private List<Question> questions;
    private int     currentIndex    = 0;
    private boolean fiftyFiftyUsed  = false;
    private boolean phoneUsed       = false;
    private boolean answered        = false;   // true once reveal is done
    private boolean suspensePlaying = false;   // true during the 3-s window

    // ── Sound ──────────────────────────────────────────────────────────────────
    private SoundManager soundManager;
    private JButton btnMute;

    // ── UI ─────────────────────────────────────────────────────────────────────
    private JLabel    lblQuestion;
    private JButton[] optionBtns  = new JButton[4];
    private boolean[] eliminated  = new boolean[4];
    private JButton   btnFiftyFifty, btnPhone, btnNext;
    private JPanel    ladderPanel;
    private JLabel[]  ladderLabels;

    // ──────────────────────────────────────────────────────────────────────────

    public MillionaireGame(List<Question> questions, SoundManager soundManager) {
        this.soundManager = soundManager;
        this.questions    = new ArrayList<>(questions);
        while (this.questions.size() < 15) this.questions.addAll(this.questions);
        Collections.shuffle(this.questions);

        setTitle("Ποιος Θέλει να Γίνει Εκατομμυριούχος;");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1050, 650);
        setMinimumSize(new Dimension(820, 520));
        setLocationRelativeTo(null);
        setResizable(true);

        buildUI();
        loadQuestion();
        soundManager.playGameMusic();
        setVisible(true);
    }

    // ── UI construction ────────────────────────────────────────────────────────

    private void buildUI() {
        JPanel root = new JPanel(new BorderLayout(0, 0));
        root.setBackground(BG);
        setContentPane(root);

        root.add(buildTopBar(), BorderLayout.NORTH);
        root.add(buildCenter(), BorderLayout.CENTER);
        root.add(buildLadder(), BorderLayout.EAST);
    }

    private JPanel buildTopBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(new Color(8, 15, 50));
        bar.setBorder(new EmptyBorder(10, 18, 10, 18));

        JLabel title = new JLabel("⬡  ΠΟΙΟΣ ΘΕΛΕΙ ΝΑ ΓΙΝΕΙ ΕΚΑΤΟΜΜΥΡΙΟΥΧΟΣ;");
        title.setFont(new Font("Georgia", Font.BOLD, 14));
        title.setForeground(GOLD);

        JPanel lifePanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        lifePanel.setOpaque(false);

        btnFiftyFifty = buildLifelineBtn("50 : 50");
        btnFiftyFifty.addActionListener(e -> doFiftyFifty());

        btnPhone = buildLifelineBtn("📞 LLM");
        btnPhone.addActionListener(e -> doPhoneLLM());

        btnMute = new JButton(soundManager.isMuted() ? "🔇 OFF" : "🔊 ON");
        btnMute.setFont(FONT_LIFE);
        btnMute.setBackground(new Color(25, 35, 80));
        btnMute.setForeground(TEXT_DIM);
        btnMute.setFocusPainted(false);
        btnMute.setBorder(BorderFactory.createLineBorder(new Color(60, 80, 140)));
        btnMute.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnMute.addActionListener(e -> toggleMute());

        lifePanel.add(btnFiftyFifty);
        lifePanel.add(btnPhone);
        lifePanel.add(Box.createHorizontalStrut(12));
        lifePanel.add(btnMute);

        bar.add(title,     BorderLayout.WEST);
        bar.add(lifePanel, BorderLayout.EAST);

        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setBackground(BG);
        wrap.add(bar, BorderLayout.CENTER);
        JSeparator sep = new JSeparator();
        sep.setForeground(new Color(40, 60, 140));
        wrap.add(sep, BorderLayout.SOUTH);
        return wrap;
    }

    private JPanel buildCenter() {
        JPanel center = new JPanel();
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
        center.setBackground(BG);
        center.setBorder(new EmptyBorder(30, 36, 20, 24));

        JPanel qCard = new JPanel(new BorderLayout());
        qCard.setBackground(CARD);
        qCard.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(40, 70, 180), 1),
            new EmptyBorder(20, 24, 20, 24)
        ));
        qCard.setAlignmentX(Component.LEFT_ALIGNMENT);
        qCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 120));

        lblQuestion = new JLabel("<html><body style='width:480px'></body></html>");
        lblQuestion.setFont(FONT_Q);
        lblQuestion.setForeground(WHITE);
        qCard.add(lblQuestion, BorderLayout.CENTER);

        center.add(qCard);
        center.add(Box.createVerticalStrut(22));

        String[] labels = {"Α", "Β", "Γ", "Δ"};
        JPanel optGrid = new JPanel(new GridLayout(2, 2, 12, 12));
        optGrid.setOpaque(false);
        optGrid.setAlignmentX(Component.LEFT_ALIGNMENT);

        for (int i = 0; i < 4; i++) {
            optionBtns[i] = buildOptionBtn(labels[i]);
            final int idx = i;
            optionBtns[i].addActionListener(e -> handleAnswer(idx));
            optGrid.add(optionBtns[i]);
        }

        center.add(optGrid);
        center.add(Box.createVerticalStrut(18));

        btnNext = new JButton("Επόμενη ερώτηση  →");
        btnNext.setFont(FONT_LABEL);
        btnNext.setBackground(new Color(30, 80, 200));
        btnNext.setForeground(WHITE);
        btnNext.setFocusPainted(false);
        btnNext.setBorderPainted(false);
        btnNext.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnNext.setPreferredSize(new Dimension(220, 40));
        btnNext.setMaximumSize(new Dimension(220, 40));
        btnNext.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnNext.setEnabled(false);
        btnNext.addActionListener(e -> nextQuestion());
        center.add(btnNext);

        return center;
    }

    private JPanel buildLadder() {
        ladderPanel = new JPanel();
        ladderPanel.setLayout(new BoxLayout(ladderPanel, BoxLayout.Y_AXIS));
        ladderPanel.setBackground(new Color(8, 14, 48));
        ladderPanel.setBorder(new EmptyBorder(10, 14, 10, 14));
        ladderPanel.setPreferredSize(new Dimension(170, 0));

        ladderLabels = new JLabel[PRIZES.length];

        for (int i = PRIZES.length - 1; i >= 0; i--) {
            JLabel lbl = new JLabel(PRIZES[i], SwingConstants.CENTER);
            lbl.setFont(FONT_PRIZE);
            lbl.setForeground(isSafeHaven(i) ? GOLD : TEXT_DIM);
            lbl.setOpaque(true);
            lbl.setBackground(new Color(8, 14, 48));
            lbl.setBorder(new EmptyBorder(3, 8, 3, 8));
            lbl.setMaximumSize(new Dimension(Integer.MAX_VALUE, 26));
            lbl.setAlignmentX(Component.CENTER_ALIGNMENT);
            ladderLabels[i] = lbl;
            ladderPanel.add(lbl);
        }

        return ladderPanel;
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private JButton buildLifelineBtn(String text) {
        JButton btn = new JButton(text);
        btn.setFont(FONT_LIFE);
        btn.setBackground(new Color(20, 40, 120));
        btn.setForeground(GOLD);
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createLineBorder(GOLD_DIM));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(100, 32));
        return btn;
    }

    private JButton buildOptionBtn(String label) {
        JButton btn = new JButton() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getBackground());
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 30, 30);
                g2.dispose();
                super.paintComponent(g);
            }
            @Override protected void paintBorder(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(HOT_BLUE.darker());
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawRoundRect(1, 1, getWidth()-2, getHeight()-2, 30, 30);
                g2.dispose();
            }
        };
        btn.setFont(FONT_BTN);
        btn.setForeground(WHITE);
        btn.setBackground(CARD);
        btn.setContentAreaFilled(false);
        btn.setOpaque(false);
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(300, 54));

        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                if (btn.isEnabled() && !answered && !suspensePlaying)
                    btn.setBackground(new Color(20, 40, 120));
            }
            public void mouseExited(MouseEvent e) {
                if (!answered && !suspensePlaying) btn.setBackground(CARD);
            }
        });
        return btn;
    }

    private boolean isSafeHaven(int i) {
        for (int sh : SAFE_HAVENS) if (sh == i) return true;
        return false;
    }

    // ── Game logic ─────────────────────────────────────────────────────────────

    private void loadQuestion() {
        if (currentIndex >= Math.min(questions.size(), PRIZES.length)) {
            showResults(true);
            return;
        }

        answered        = false;
        suspensePlaying = false;
        Arrays.fill(eliminated, false);

        Question q = questions.get(currentIndex);
        lblQuestion.setText("<html><body style='width:480px'>" + q.getQuestion() + "</body></html>");

        String[] labels = {"  Α.  ", "  Β.  ", "  Γ.  ", "  Δ.  "};
        String[] opts   = q.getOptions();
        for (int i = 0; i < 4; i++) {
            optionBtns[i].setText(labels[i] + opts[i]);
            optionBtns[i].setBackground(CARD);
            optionBtns[i].setForeground(WHITE);
            optionBtns[i].setEnabled(true);
        }

        updateLadder();
        btnNext.setEnabled(false);
        if (currentIndex == Math.min(questions.size(), PRIZES.length) - 1) {
            btnNext.setText("Αποτελέσματα  →");
        }
    }

    /**
     * Called when the player clicks an answer button.
     *
     * Phase 1 – Suspense (immediate):
     *   • Highlight the chosen button in PENDING blue
     *   • Lock all buttons
     *   • Start suspense music
     *
     * Phase 2 – Reveal (after SUSPENSE_MS):
     *   • Colour chosen button green/red
     *   • If wrong, reveal the correct button in green after 600 ms
     *   • Play correct/wrong sound
     *   • Resume background music
     *   • Enable "Next" (or auto-navigate away on wrong)
     */
    private void handleAnswer(int sel) {
        if (answered || suspensePlaying) return;

        suspensePlaying = true;

        // ── Phase 1: lock UI, highlight selection, play suspense ──────────────
        for (JButton b : optionBtns) b.setEnabled(false);

        optionBtns[sel].setBackground(PENDING);   // chosen option glows blue

        soundManager.playAnswerSuspense();          // pauses bg, plays suspense clip

        // ── Phase 2: reveal after SUSPENSE_MS ────────────────────────────────
        Timer revealTimer = new Timer(SUSPENSE_MS, ev -> revealAnswer(sel));
        revealTimer.setRepeats(false);
        revealTimer.start();
    }

    /** Reveals the correct/wrong colours and plays the appropriate sound. */
    private void revealAnswer(int sel) {
        answered        = true;
        suspensePlaying = false;

        Question q       = questions.get(currentIndex);
        boolean  correct = q.isCorrect(sel);

        optionBtns[sel].setBackground(correct ? CORRECT : WRONG);

        if (correct) {
            soundManager.playCorrect();
            soundManager.resumeBackground();
            btnNext.setEnabled(true);
        } else {
            soundManager.playWrong();

            // Show the correct answer 600 ms later, then navigate away
            Timer showCorrect = new Timer(600, e2 ->
                optionBtns[q.getCorrectIndex()].setBackground(CORRECT));
            showCorrect.setRepeats(false);
            showCorrect.start();

            Timer goResults = new Timer(2400, e2 -> showResults(false));
            goResults.setRepeats(false);
            goResults.start();
        }
    }

    private void nextQuestion() {
        currentIndex++;
        soundManager.resumeBackground();   // ensure bg music is running between questions
        loadQuestion();
    }

    private void updateLadder() {
        for (int i = 0; i < ladderLabels.length; i++) {
            if (i == currentIndex) {
                ladderLabels[i].setForeground(GOLD);
                ladderLabels[i].setBackground(new Color(20, 50, 140));
                ladderLabels[i].setFont(new Font("SansSerif", Font.BOLD, 13));
            } else if (i < currentIndex) {
                ladderLabels[i].setForeground(CORRECT);
                ladderLabels[i].setBackground(new Color(8, 14, 48));
                ladderLabels[i].setFont(FONT_PRIZE);
            } else {
                ladderLabels[i].setForeground(isSafeHaven(i) ? GOLD : TEXT_DIM);
                ladderLabels[i].setBackground(new Color(8, 14, 48));
                ladderLabels[i].setFont(FONT_PRIZE);
            }
        }
        ladderPanel.revalidate();
        ladderPanel.repaint();
    }

    // ── Lifelines ──────────────────────────────────────────────────────────────

    private void doFiftyFifty() {
        if (fiftyFiftyUsed || answered || suspensePlaying) return;
        fiftyFiftyUsed = true;
        btnFiftyFifty.setEnabled(false);
        btnFiftyFifty.setForeground(ELIM);

        Question q = questions.get(currentIndex);
        int correct = q.getCorrectIndex();

        List<Integer> wrong = new ArrayList<>();
        for (int i = 0; i < 4; i++) if (i != correct) wrong.add(i);
        Collections.shuffle(wrong);

        for (int i = 0; i < 2; i++) {
            int idx = wrong.get(i);
            eliminated[idx] = true;
            optionBtns[idx].setEnabled(false);
            optionBtns[idx].setBackground(ELIM);
            optionBtns[idx].setForeground(new Color(80, 80, 100));
        }
    }

    private void doPhoneLLM() {
        if (phoneUsed || answered || suspensePlaying) return;
        phoneUsed = true;
        btnPhone.setEnabled(false);
        btnPhone.setForeground(ELIM);

        Question q        = questions.get(currentIndex);
        String[] labels   = {"Α", "Β", "Γ", "Δ"};
        String correctLbl = labels[q.getCorrectIndex()];
        String correctTxt = q.getOptions()[q.getCorrectIndex()];

        JDialog ringing = new JDialog(this, "📞 Κλήση...", true);
        ringing.setSize(380, 180);
        ringing.setLocationRelativeTo(this);
        ringing.setLayout(new BorderLayout());
        ringing.getContentPane().setBackground(CARD);
        ringing.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);

        JLabel lblRing = new JLabel(
            "<html><center><b style='font-size:15px'>📞 Κλήση προς LLM...</b><br><br>"
            + "<i>Παρακαλώ μην κλείσετε τη γραμμή</i></center></html>",
            SwingConstants.CENTER);
        lblRing.setForeground(WHITE);
        lblRing.setFont(new Font("Georgia", Font.PLAIN, 14));
        ringing.add(lblRing, BorderLayout.CENTER);

        Timer t = new Timer(1500, ev -> {
            ringing.dispose();
            showPhoneAnswer(correctLbl, correctTxt);
        });
        t.setRepeats(false);
        t.start();

        ringing.setVisible(true);
    }

    private void showPhoneAnswer(String label, String answer) {
        String[] intros = {
            "Εεε... να σου πω, σίγουρα είναι...",
            "Κοίτα, έχω 100% certainty, είναι...",
            "Με βεβαιότητα 0.9997 σου λέω...",
            "Το ξέρω χωρίς καν να σκεφτώ, είναι...",
            "Μην ακούς τον άλλο, η απάντηση είναι...",
        };
        String intro = intros[new Random().nextInt(intros.length)];

        JDialog dialog = new JDialog(this, "📞 Ο LLM μίλησε!", true);
        dialog.setSize(430, 260);
        dialog.setLocationRelativeTo(this);
        dialog.getContentPane().setBackground(CARD);
        dialog.setLayout(new BorderLayout(0, 0));

        JPanel header = new JPanel();
        header.setBackground(new Color(15, 30, 90));
        header.setBorder(new EmptyBorder(12, 16, 12, 16));
        JLabel lblH = new JLabel("📞  Phone-an-LLM");
        lblH.setFont(new Font("Georgia", Font.BOLD, 15));
        lblH.setForeground(GOLD);
        header.add(lblH);

        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBackground(CARD);
        body.setBorder(new EmptyBorder(22, 28, 18, 28));

        JLabel lblIntro = new JLabel("<html><i>\"" + intro + "\"</i></html>");
        lblIntro.setFont(new Font("Georgia", Font.ITALIC, 14));
        lblIntro.setForeground(TEXT_DIM);
        lblIntro.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lblAnswer = new JLabel("  " + label + "  —  " + answer);
        lblAnswer.setFont(new Font("Georgia", Font.BOLD, 20));
        lblAnswer.setForeground(CORRECT);
        lblAnswer.setAlignmentX(Component.LEFT_ALIGNMENT);
        lblAnswer.setBorder(new EmptyBorder(14, 0, 0, 0));

        body.add(lblIntro);
        body.add(lblAnswer);

        JButton close = new JButton("Ευχαριστώ, φίλε!");
        close.setFont(FONT_LIFE);
        close.setBackground(new Color(30, 80, 200));
        close.setForeground(WHITE);
        close.setFocusPainted(false);
        close.setBorderPainted(false);
        close.setCursor(new Cursor(Cursor.HAND_CURSOR));
        close.addActionListener(e -> dialog.dispose());

        JPanel foot = new JPanel();
        foot.setBackground(CARD);
        foot.setBorder(new EmptyBorder(0, 0, 14, 0));
        foot.add(close);

        dialog.add(header, BorderLayout.NORTH);
        dialog.add(body,   BorderLayout.CENTER);
        dialog.add(foot,   BorderLayout.SOUTH);
        dialog.setVisible(true);
    }

    // ── Results ────────────────────────────────────────────────────────────────

    private void showResults(boolean won) {
        soundManager.stopAll();
        if (won) soundManager.playMillion();

        String prize = currentIndex > 0 ? PRIZES[Math.min(currentIndex - 1, PRIZES.length - 1)] : "€0";
        if (won) prize = PRIZES[PRIZES.length - 1];

        String safeHaven = "€0";
        for (int sh : SAFE_HAVENS) {
            if (currentIndex > sh) safeHaven = PRIZES[sh];
        }

        int choice = ResultScreen.show(this, won, prize, safeHaven);
        if      (choice == 0) restartGame();
        else if (choice == 1) goToMenu();
        else                  System.exit(0);
    }

    private void restartGame() {
        dispose();
        Collections.shuffle(questions);
        SwingUtilities.invokeLater(() -> new MillionaireGame(questions, soundManager));
    }

    private void goToMenu() {
        dispose();
        SwingUtilities.invokeLater(() -> new MenuScreen(questions));
    }

    // ── Sound ──────────────────────────────────────────────────────────────────

    private void toggleMute() {
        boolean nowMuted = !soundManager.isMuted();
        soundManager.setMuted(nowMuted);
        btnMute.setText(nowMuted ? "🔇 OFF" : "🔊 ON");
    }
}