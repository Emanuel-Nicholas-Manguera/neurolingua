package app.neurolingua.mobile;

/** Converts playback levels and display timestamps into a continuous pose. Times use uptime milliseconds. */
final class SpiralMotion {
    static final double FULL_TURN = 2 * Math.PI;
    private static final double IDLE_TURN_MS = 4000;
    private static final int SAMPLE_TIMEOUT_MS = 180;
    private static final int ATTACK_MS = 29;
    private static final int RELEASE_MS = 97;

    private long lastFrame = -1;
    private long lastSample;
    private float target;
    private double level;
    private double phase;

    void sample(float value, long timeMs) {
        target = Float.isFinite(value) ? Math.max(0f, Math.min(1f, value)) : 0f;
        lastSample = timeMs;
    }

    void advance(long timeMs) {
        if (lastFrame >= 0) {
            long elapsed = Math.min(64, Math.max(0, timeMs - lastFrame));
            double requested = timeMs - lastSample < SAMPLE_TIMEOUT_MS ? target : 0;
            double responseMs = requested > level ? ATTACK_MS : RELEASE_MS;
            level += (requested - level) * (1 - Math.exp(-elapsed / responseMs));
            phase = (phase + FULL_TURN * elapsed / IDLE_TURN_MS * (1 + 1.5 * level)) % FULL_TURN;
        }
        lastFrame = timeMs;
    }

    void pause() {
        lastFrame = -1;
        target = 0;
        level = 0;
    }

    double phase() { return phase; }
    double level() { return level; }
}
