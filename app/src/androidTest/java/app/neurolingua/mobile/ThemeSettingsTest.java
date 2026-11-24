package app.neurolingua.mobile;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.RadioGroup;

import androidx.test.core.app.ActivityScenario;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;

public class ThemeSettingsTest {
    @Test public void pickingAColorKeepsSettingsOpenAndRestoresThePaletteOnLaunch() {
        Context app = InstrumentationRegistry.getInstrumentation().getTargetContext();
        SharedPreferences prefs = app.getSharedPreferences("appearance", Context.MODE_PRIVATE);
        String previous = prefs.getString("color_theme", null);
        ColorTheme choice = ColorTheme.selected(app) == ColorTheme.OCEAN ? ColorTheme.ROSE : ColorTheme.OCEAN;
        try {
            try (ActivityScenario<MainActivity> screen = ActivityScenario.launch(MainActivity.class)) {
                screen.onActivity(activity -> {
                    activity.findViewById(R.id.settings).performClick();
                    activity.findViewById(choice.optionId).performClick();
                });
                InstrumentationRegistry.getInstrumentation().waitForIdleSync();
                screen.onActivity(activity -> assertSelected(activity, choice));
                screen.recreate();
                screen.onActivity(activity -> assertSelected(activity, choice));
            }
            try (ActivityScenario<MainActivity> screen = ActivityScenario.launch(MainActivity.class)) {
                screen.onActivity(activity -> {
                    int expected = ThemeColors.get(new ContextThemeWrapper(activity, choice.style), R.attr.paletteSurface);
                    ViewGroup content = activity.findViewById(android.R.id.content);
                    assertEquals(expected, ((ColorDrawable) content.getChildAt(0).getBackground()).getColor());
                    activity.findViewById(R.id.settings).performClick();
                    assertSelected(activity, choice);
                    activity.findViewById(R.id.home).performClick();
                    assertNotNull(activity.findViewById(R.id.start_test));
                });
            }
        } finally {
            // A null value removes the key, preserving the default on a fresh install.
            prefs.edit().putString("color_theme", previous).commit();
        }
    }

    private static void assertSelected(MainActivity activity, ColorTheme expected) {
        RadioGroup choices = activity.findViewById(R.id.theme_options);
        assertNotNull(choices);
        assertEquals(expected.optionId, choices.getCheckedRadioButtonId());
        assertEquals(expected, ColorTheme.selected(activity));
        assertEquals(ThemeColors.get(new ContextThemeWrapper(activity, expected.style), R.attr.paletteAccent),
                ThemeColors.get(activity, R.attr.paletteAccent));
    }

    @Test public void previewMatchesExamAndEveryPaletteChangesTheRibbonsInBothModes() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            Context app = InstrumentationRegistry.getInstrumentation().getTargetContext();
            for (int mode : new int[]{Configuration.UI_MODE_NIGHT_NO, Configuration.UI_MODE_NIGHT_YES}) {
                Configuration config = new Configuration(app.getResources().getConfiguration());
                config.uiMode = (config.uiMode & ~Configuration.UI_MODE_NIGHT_MASK) | mode;
                Context device = app.createConfigurationContext(config);
                Bitmap[] exams = new Bitmap[ColorTheme.values().length];
                for (ColorTheme palette : ColorTheme.presets()) {
                    Context themed = new ContextThemeWrapper(device, palette.style);
                    Bitmap exam = spiralFrame(themed, R.layout.screen_exam, R.id.exam_spiral);
                    Bitmap preview = spiralFrame(themed, R.layout.screen_settings, R.id.theme_spiral);
                    assertTrue("Settings should show the exam ribbons", preview.sameAs(exam));
                    for (Bitmap other : exams) {
                        if (other != null) assertFalse("Each palette needs its own ribbons", other.sameAs(exam));
                    }
                    exams[palette.ordinal()] = exam;
                }
            }
        });
    }

    private static Bitmap spiralFrame(Context context, int layout, int id) {
        View screen = LayoutInflater.from(context).inflate(layout, new FrameLayout(context), false);
        ExamSpiralView spiral = screen.findViewById(id);
        spiral.layout(0, 0, 144, 144);
        Bitmap frame = Bitmap.createBitmap(144, 144, Bitmap.Config.ARGB_8888);
        spiral.draw(new Canvas(frame));
        assertEquals(Color.TRANSPARENT, frame.getPixel(72, 72));
        return frame;
    }
}
