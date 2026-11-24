package app.neurolingua.mobile;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import android.animation.ValueAnimator;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.os.SystemClock;

import androidx.test.core.app.ActivityScenario;
import org.junit.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class ExamSpiralTest {
    @Test public void idleTurnsAtTheSameSpeedAcrossFrameRatesAndPausesOffscreen() {
        for (long interval : new long[]{8, 16, 32}) {
            SpiralMotion motion = new SpiralMotion();
            for (long t = 0; t < 1000; t += interval) motion.advance(t);
            motion.advance(1000);
            assertEquals(Math.PI / 2, motion.phase(), .00001);
            assertEquals(0, motion.level(), 0);
            motion.pause();
            motion.advance(90000);
            assertEquals(Math.PI / 2, motion.phase(), .00001);
            motion.advance(90016);
            assertTrue(motion.phase() > Math.PI / 2);
        }
    }

    @Test public void playbackLevelsAreClampedSmoothedAndReleasedAfterSilence() {
        SpiralMotion motion = new SpiralMotion();
        motion.advance(0);
        motion.sample(3f, 0);
        motion.advance(29);
        double peak = 1 - Math.exp(-1);
        assertEquals(peak, motion.level(), .00001);
        motion.sample(Float.NaN, 29);
        motion.advance(58);
        assertEquals(peak * Math.exp(-29.0 / 97), motion.level(), .00001);
        motion.sample(-1f, 58);
        motion.advance(87);
        assertEquals(peak * Math.exp(-58.0 / 97), motion.level(), .00001);
        motion.sample(1f, 87);
        for (int t = 103; t < 1200; t += 16) motion.advance(t);
        assertTrue(motion.level() < .001);
    }

    @Test public void examRibbonsMoveAtIdleExpandOnPlaybackAndKeepTheCenterOpen() throws Exception {
        assumeTrue(ValueAnimator.areAnimatorsEnabled());
        try (ActivityScenario<MainActivity> screen = ActivityScenario.launch(MainActivity.class)) {
            AtomicReference<ExamSpiralView> spiral = new AtomicReference<>();
            screen.onActivity(activity -> {
                // Exercise the actual exam layout without starting or editing a saved assessment.
                activity.setContentView(R.layout.screen_exam);
                spiral.set(activity.findViewById(R.id.exam_spiral));
            });
            Bitmap idle = frameAfter(spiral.get(), 180, false);
            Bitmap later = frameAfter(spiral.get(), 340, false);
            Bitmap speaking = frameAfter(spiral.get(), 480, true);
            Bitmap quiet = frameAfter(spiral.get(), 900, false);
            assertFalse("Idle frames must change", idle.sameAs(later));
            double peakRadius = ribbonRadius(speaking);
            assertTrue("Playback must expand the ribbons", peakRadius > ribbonRadius(idle) + .055);
            assertTrue("Stale playback must settle to idle", peakRadius > ribbonRadius(quiet) + .055);
        }
    }

    private static Bitmap frameAfter(ExamSpiralView view, int durationMs, boolean speaking) throws Exception {
        CompletableFuture<Bitmap> result = new CompletableFuture<>();
        long start = SystemClock.uptimeMillis();
        view.postOnAnimation(new Runnable() {
            @Override public void run() {
                if (speaking) view.setVoiceLevel(1f);
                if (!view.isLaidOut() || SystemClock.uptimeMillis() - start < durationMs) {
                    view.postOnAnimation(this);
                    return;
                }
                int side = Math.min(view.getWidth(), view.getHeight());
                Bitmap frame = Bitmap.createBitmap(160, 160, Bitmap.Config.ARGB_8888);
                Canvas canvas = new Canvas(frame);
                canvas.scale(160f / side, 160f / side);
                canvas.translate((side - view.getWidth()) / 2f, (side - view.getHeight()) / 2f);
                view.draw(canvas);
                result.complete(frame);
            }
        });
        return result.get(6, TimeUnit.SECONDS);
    }

    private static double ribbonRadius(Bitmap frame) {
        int half = frame.getWidth() / 2;
        double sum = 0;
        int count = 0;
        for (int y = 0; y < frame.getHeight(); y++) {
            for (int x = 0; x < frame.getWidth(); x++) {
                int alpha = Color.alpha(frame.getPixel(x, y));
                double radius = Math.hypot(x - half, y - half) / half;
                if (radius < .12) assertEquals("Keep the center transparent", 0, alpha);
                if (alpha > 100) {
                    sum += radius;
                    count++;
                }
            }
        }
        assertTrue("Ribbons must be visible", count > 100);
        return sum / count;
    }
}
