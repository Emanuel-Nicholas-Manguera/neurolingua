package app.neurolingua.mobile;

import android.animation.ValueAnimator;
import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.graphics.Canvas;
import android.os.Build;
import android.os.SystemClock;
import android.provider.Settings;
import android.util.AttributeSet;
import android.view.View;

/** The exam's decorative spiral. The adjacent text describes the current turn. */
public final class ExamSpiralView extends View {
    private final SpiralRenderer renderer;
    private final SpiralMotion motion = new SpiralMotion();
    private boolean visible;
    private boolean motionEnabled;
    private Runnable stopObservingMotion;

    public ExamSpiralView(Context context, AttributeSet attrs) {
        super(context, attrs);
        renderer = new SpiralRenderer(context);
    }

    /** Supply Maria's normalized playback level on the UI thread, with zero for silence. */
    public void setVoiceLevel(float level) {
        motion.sample(level, SystemClock.uptimeMillis());
    }

    @Override protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (Build.VERSION.SDK_INT >= 33) {
            ValueAnimator.DurationScaleChangeListener listener = scale -> setMotionEnabled(scale > 0);
            ValueAnimator.registerDurationScaleChangeListener(listener);
            stopObservingMotion = () -> ValueAnimator.unregisterDurationScaleChangeListener(listener);
        } else {
            ContentResolver settings = getContext().getContentResolver();
            String key = Settings.Global.ANIMATOR_DURATION_SCALE;
            ContentObserver observer = new ContentObserver(getHandler()) {
                @Override public void onChange(boolean selfChange) {
                    setMotionEnabled(Settings.Global.getFloat(settings, key, 1f) > 0f);
                }
            };
            settings.registerContentObserver(Settings.Global.getUriFor(key), false, observer);
            stopObservingMotion = () -> settings.unregisterContentObserver(observer);
        }
        setMotionEnabled(ValueAnimator.areAnimatorsEnabled());
    }

    @Override protected void onDetachedFromWindow() {
        stopObservingMotion.run();
        stopObservingMotion = null;
        motion.pause();
        super.onDetachedFromWindow();
    }

    private void setMotionEnabled(boolean enabled) {
        motionEnabled = enabled;
        if (!enabled) motion.pause();
        if (visible) invalidate();
    }

    @Override public void onVisibilityAggregated(boolean isVisible) {
        super.onVisibilityAggregated(isVisible);
        visible = isVisible;
        if (isVisible) setMotionEnabled(ValueAnimator.areAnimatorsEnabled());
        else motion.pause();
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (visible && motionEnabled) {
            motion.advance(getDrawingTime());
            postInvalidateOnAnimation();
        } else {
            motion.pause();
        }
        renderer.draw(canvas, getWidth(), getHeight(), motion.phase(), motion.level());
    }
}
