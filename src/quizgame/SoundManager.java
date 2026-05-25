package quizgame;

import javax.sound.sampled.*;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Manages all game audio using javax.sound.sampled (built into every JDK).
 * Requires WAV files — convert MP3s with:  ffmpeg -i input.mp3 output.wav
 *
 * Expected files in resources/sounds/ (as classpath resources inside the JAR):
 *   menu.wav    – Main menu background music  (loops)
 *   game.wav    – In-game background music    (loops)
 *   answer.wav  – Suspense clip while answer is revealed (plays once)
 *   correct.wav – Correct answer jingle
 *   wrong.wav   – Wrong answer sound
 *   million.wav – Winning the million celebration
 *   milestone.wav – Safe-haven milestone sound
 *   start.wav   – Game start sound
 *
 * Answer flow used by MillionaireGame:
 *   1. Player clicks  → playAnswerSuspense()   (stops bg, plays answer.wav)
 *   2. After ~3 s     → playCorrect()/playWrong()
 *   3. Then           → resumeBackground()     (restarts game.wav loop)
 */
public class SoundManager {

    /** Currently active looping background track (filename, so we can restart it). */
    private String  bgFilename = null;
    private Clip    bgClip     = null;
    private Clip    fxClip     = null;

    private boolean muted  = false;
    private float   volume = 0.7f;   // 0.0 – 1.0

    // ── Public API ──────────────────────────────────────────────────────────────

    public void playMenuMusic() { playLoop("menu.wav"); }
    public void playGameMusic() { playLoop("game.wav"); }

    /**
     * Suspense sound played immediately after the player picks an answer.
     * Stops the background loop so the suspense clip is heard cleanly.
     * Call resumeBackground() after the reveal.
     */
    public void playAnswerSuspense() {
        stopBackground();
        playFX("answer.wav");
    }

    /** Played after a correct reveal. Does NOT resume background — caller does that. */
    public void playCorrect() {
        stopFX();
        playFX("correct.wav");
    }

    /** Played after a wrong reveal. Does NOT resume background — caller handles flow. */
    public void playWrong() {
        stopFX();
        playFX("wrong.wav");
    }

    /** Played when the player wins the million. Stops everything first. */
    public void playMillion() {
        stopAll();
        playFX("million.wav");
    }

    /** Played when the player hits a safe-haven milestone. */
    public void playMilestone() {
        stopFX();
        playFX("milestone.wav");
    }

    public void playStart() { stopBackground(); playFX("start.wav"); }

    /**
     * Restarts the background music loop from the beginning.
     * Safe to call even if no bg track has been set yet.
     */
    public void resumeBackground() {
        if (bgFilename == null) return;
        playLoop(bgFilename);
    }

    public void stopBackground() {
        if (bgClip != null) {
            bgClip.stop();
            bgClip.close();
            bgClip = null;
        }
    }

    public void stopAll() {
        stopBackground();
        stopFX();
    }

    public void setMuted(boolean m) {
        this.muted = m;
        if (bgClip != null) {
            if (m) bgClip.stop();
            else   bgClip.loop(Clip.LOOP_CONTINUOUSLY);
        }
        setClipVolume(fxClip, m ? 0f : volume);
    }

    public boolean isMuted() { return muted; }

    public void setVolume(float v) {
        this.volume = Math.max(0f, Math.min(1f, v));
        setClipVolume(bgClip, muted ? 0f : this.volume);
        setClipVolume(fxClip, muted ? 0f : this.volume);
    }

    // ── Internal helpers ────────────────────────────────────────────────────────

    /** Loads a WAV and starts it looping as background music. */
    private void playLoop(String filename) {
        stopBackground();
        bgFilename = filename;
        bgClip     = loadClip(filename);
        if (bgClip == null) return;
        setClipVolume(bgClip, muted ? 0f : volume);
        if (!muted) bgClip.loop(Clip.LOOP_CONTINUOUSLY);
    }

    /** Loads a WAV and plays it once as a sound effect. */
    private void playFX(String filename) {
        stopFX();
        fxClip = loadClip(filename);
        if (fxClip == null) return;
        setClipVolume(fxClip, muted ? 0f : volume);
        if (!muted) fxClip.start();
    }

    private void stopFX() {
        if (fxClip != null) {
            fxClip.stop();
            fxClip.close();
            fxClip = null;
        }
    }

    /**
     * Loads a WAV resource from the classpath (works both in IDE and inside JAR).
     * Normalises the format to PCM_SIGNED so any standard WAV variant is accepted.
     * Returns null (with a console message) if the file is missing or unsupported.
     */
    private Clip loadClip(String filename) {
        try (InputStream is = getClass()
                .getClassLoader()
                .getResourceAsStream("sounds/" + filename)) {

            if (is == null) {
                System.err.println("[SoundManager] Resource not found: sounds/" + filename);
                return null;
            }

            BufferedInputStream bis = new BufferedInputStream(is);
            AudioInputStream raw = AudioSystem.getAudioInputStream(bis);

            AudioFormat fmt = raw.getFormat();
            if (fmt.getEncoding() != AudioFormat.Encoding.PCM_SIGNED) {
                AudioFormat pcm = new AudioFormat(
                        AudioFormat.Encoding.PCM_SIGNED,
                        fmt.getSampleRate(),
                        16,
                        fmt.getChannels(),
                        fmt.getChannels() * 2,
                        fmt.getSampleRate(),
                        false
                );
                raw = AudioSystem.getAudioInputStream(pcm, raw);
            }

            Clip clip = AudioSystem.getClip();
            clip.open(raw);
            return clip;

        } catch (UnsupportedAudioFileException e) {
            System.err.println("[SoundManager] Unsupported format: "
                    + filename + " — must be WAV.");
        } catch (LineUnavailableException | IOException e) {
            System.err.println("[SoundManager] Cannot load "
                    + filename + ": " + e.getMessage());
        }
        return null;
    }

    /**
     * Sets a Clip's volume via MASTER_GAIN (dB).
     * Converts linear 0–1 to decibels; silences clip if vol == 0.
     */
    private void setClipVolume(Clip clip, float vol) {
        if (clip == null) return;
        try {
            FloatControl gain = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
            float dB = (vol <= 0f)
                ? gain.getMinimum()
                : (float)(20.0 * Math.log10(vol));
            gain.setValue(Math.max(gain.getMinimum(), Math.min(gain.getMaximum(), dB)));
        } catch (IllegalArgumentException ignored) {
            // Some audio lines don't expose MASTER_GAIN — safe to skip
        }
    }
}
