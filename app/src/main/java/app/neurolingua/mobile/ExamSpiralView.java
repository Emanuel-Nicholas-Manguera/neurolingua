package app.neurolingua.mobile;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.View;

/** The exam's decorative spiral. The adjacent text describes the current turn. */
public final class ExamSpiralView extends View {
    private final SpiralRenderer renderer;
    private final SpiralMotion motion = new SpiralMotion();
    private boolean visible;

    public ExamSpiralView(Context context, AttributeSet attrs) {
        super(context, attrs);
        renderer = new SpiralRenderer(context);
    }

    /** Supply Maria's normalized playback level on the UI thread, with zero for silence. */
    public void setVoiceLevel(float level) {
        motion.sample(level, SystemClock.uptimeMillis());
    }

    @Override public void onVisibilityAggregated(boolean isVisible) {
        super.onVisibilityAggregated(isVisible);
        visible = isVisible;
        if (isVisible) invalidate();
        else motion.pause();
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (visible && ValueAnimator.areAnimatorsEnabled()) {
            motion.advance(getDrawingTime());
            postInvalidateOnAnimation();
        } else {
            motion.pause();
        }
        renderer.draw(canvas, getWidth(), getHeight(), motion.phase(), motion.level());
    }
}
