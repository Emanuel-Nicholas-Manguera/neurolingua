package app.neurolingua.mobile;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.view.ContextThemeWrapper;

import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;

public class AppearanceTest {
    @Test public void systemAppearanceSelectsTheWindowPaletteAndButtonContrast() {
        checkAppearance(Configuration.UI_MODE_NIGHT_NO, Color.rgb(242, 234, 250), true);
        checkAppearance(Configuration.UI_MODE_NIGHT_YES, Color.rgb(46, 33, 71), false);
    }

    private static void checkAppearance(int nightMode, int expectedPaper, boolean light) {
        Context app = InstrumentationRegistry.getInstrumentation().getTargetContext();
        Configuration config = new Configuration(app.getResources().getConfiguration());
        config.uiMode = (config.uiMode & ~Configuration.UI_MODE_NIGHT_MASK) | nightMode;
        Context themed = new ContextThemeWrapper(app.createConfigurationContext(config),
                app.getApplicationInfo().theme);

        TypedArray window = themed.obtainStyledAttributes(new int[]{
                android.R.attr.windowBackground, android.R.attr.windowLightStatusBar,
                android.R.attr.alertDialogTheme});
        int dialogStyle;
        try {
            assertEquals(expectedPaper, window.getColor(0, Color.TRANSPARENT));
            assertEquals(light, window.getBoolean(1, !light));
            dialogStyle = window.getResourceId(2, 0);
        } finally {
            window.recycle();
        }
        TypedArray dialog = themed.obtainStyledAttributes(dialogStyle,
                new int[]{android.R.attr.colorBackgroundFloating, android.R.attr.textColorPrimary});
        try {
            assertEquals(expectedPaper, dialog.getColor(0, Color.TRANSPARENT));
            assertReadable(dialog.getColor(1, Color.TRANSPARENT), expectedPaper);
        } finally {
            dialog.recycle();
        }
        checkButton(themed, R.style.Control_Primary, R.color.plum);
        checkButton(themed, R.style.Control_Hero, R.color.cream);
        assertReadable(themed.getColor(R.color.muted), expectedPaper);
        assertReadable(themed.getColor(R.color.ink), themed.getColor(R.color.lilac));
        assertReadable(themed.getColor(R.color.cream), themed.getColor(R.color.hero_start));
        assertReadable(themed.getColor(R.color.cream), themed.getColor(R.color.hero_end));
    }

    private static void checkButton(Context themed, int style, int fill) {
        TypedArray button = themed.obtainStyledAttributes(style, new int[]{android.R.attr.textColor});
        try {
            assertReadable(button.getColor(0, Color.TRANSPARENT), themed.getColor(fill));
        } finally {
            button.recycle();
        }
    }

    private static void assertReadable(int foreground, int background) {
        assertEquals(255, Color.alpha(foreground));
        float a = Color.luminance(foreground);
        float b = Color.luminance(background);
        assertTrue("Text contrast below 4.5:1", (Math.max(a, b) + .05f) / (Math.min(a, b) + .05f) >= 4.5f);
    }
}
