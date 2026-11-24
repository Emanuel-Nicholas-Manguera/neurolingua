package app.neurolingua.mobile;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.View;

/** The exam's decorative spiral. The adjacent text describes the current turn. */
public final class ExamSpiralView extends View {
    private static final double IDLE_TURN_MS = 4000;
    private final SpiralRenderer renderer;
    private boolean visible;
    private long lastFrameMs;
    private double phase;

    public ExamSpiralView(Context context, AttributeSet attrs) {
        super(context, attrs);
        renderer = new SpiralRenderer(context);
    }

    @Override public void onVisibilityAggregated(boolean isVisible) {
        super.onVisibilityAggregated(isVisible);
        visible = isVisible;
        lastFrameMs = 0;
        if (isVisible) invalidate();
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (visible && ValueAnimator.areAnimatorsEnabled()) {
            long now = getDrawingTime();
            if (lastFrameMs != 0) {
                long elapsed = Math.min(64, Math.max(0, now - lastFrameMs));
                phase = (phase + SpiralRenderer.FULL_TURN * elapsed / IDLE_TURN_MS)
                        % SpiralRenderer.FULL_TURN;
            }
            lastFrameMs = now;
            postInvalidateOnAnimation();
        } else {
            lastFrameMs = 0;
        }
        renderer.draw(canvas, getWidth(), getHeight(), phase);
    }
}
