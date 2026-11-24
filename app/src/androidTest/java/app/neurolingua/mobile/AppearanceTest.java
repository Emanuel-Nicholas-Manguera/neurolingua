package app.neurolingua.mobile;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.ContextThemeWrapper;

import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;

public class AppearanceTest {
    @Test public void violetUsesTheSelectedSurfaceAndReadableControls() {
        checkBothModes(R.style.AppTheme, 0xFFF2EAFA, 0xFF2E2147);
    }

    @Test public void oceanUsesTheSelectedSurfaceAndReadableControls() {
        checkBothModes(R.style.OceanTheme, 0xFFE6F3F5, 0xFF132F3C);
    }

    @Test public void roseUsesTheSelectedSurfaceAndReadableControls() {
        checkBothModes(R.style.RoseTheme, 0xFFFAEAF0, 0xFF3B2232);
    }

    private static void checkBothModes(int style, int lightSurface, int darkSurface) {
        checkAppearance(style, Configuration.UI_MODE_NIGHT_NO, lightSurface);
        checkAppearance(style, Configuration.UI_MODE_NIGHT_YES, darkSurface);
    }

    private static void checkAppearance(int style, int nightMode, int expectedPaper) {
        Context app = InstrumentationRegistry.getInstrumentation().getTargetContext();
        Configuration config = new Configuration(app.getResources().getConfiguration());
        config.uiMode = (config.uiMode & ~Configuration.UI_MODE_NIGHT_MASK) | nightMode;
        Context themed = new ContextThemeWrapper(app.createConfigurationContext(config), style);
        boolean light = nightMode == Configuration.UI_MODE_NIGHT_NO;

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
        TypedArray dialog = new ContextThemeWrapper(themed, dialogStyle).obtainStyledAttributes(
                new int[]{android.R.attr.colorBackgroundFloating, android.R.attr.textColorPrimary});
        try {
            assertEquals(expectedPaper, dialog.getColor(0, Color.TRANSPARENT));
            assertReadable(dialog.getColor(1, Color.TRANSPARENT), expectedPaper);
        } finally {
            dialog.recycle();
        }
        checkButton(themed, R.style.Control_Primary, R.attr.paletteAccent);
        checkButton(themed, R.style.Control_Hero, R.attr.paletteHeroText);
        assertReadable(ThemeColors.get(themed, R.attr.paletteMuted), expectedPaper);
        assertReadable(ThemeColors.get(themed, R.attr.paletteText), ThemeColors.get(themed, R.attr.paletteSurfaceVariant));
        GradientDrawable hero = (GradientDrawable) themed.getDrawable(R.drawable.hero);
        int[] stops = hero.getColors();
        assertEquals(ThemeColors.get(themed, R.attr.paletteHeroStart), stops[0]);
        assertEquals(ThemeColors.get(themed, R.attr.paletteHeroEnd), stops[1]);
        for (int stop : stops) assertReadable(ThemeColors.get(themed, R.attr.paletteHeroText), stop);
    }

    private static void checkButton(Context themed, int style, int fill) {
        TypedArray button = themed.obtainStyledAttributes(style, new int[]{android.R.attr.textColor});
        try {
            assertReadable(button.getColor(0, Color.TRANSPARENT), ThemeColors.get(themed, fill));
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
