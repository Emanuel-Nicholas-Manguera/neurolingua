package app.neurolingua.mobile;

import static org.junit.Assert.assertEquals;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.drawable.ColorDrawable;
import android.view.ContextThemeWrapper;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import androidx.test.core.app.ActivityScenario;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class AppearanceModeTest {
    private Context app;
    private SharedPreferences prefs;
    private String originalMode;
    private String originalColor;

    @Before public void rememberAppearance() {
        app = InstrumentationRegistry.getInstrumentation().getTargetContext();
        prefs = AppearancePreferences.of(app);
        originalMode = prefs.getString("display_mode", null);
        originalColor = prefs.getString("color_theme", null);
    }

    @After public void restoreAppearance() {
        prefs.edit().putString("display_mode", originalMode).putString("color_theme", originalColor).commit();
    }

    @Test public void upgradingKeepsThePaletteAndLaunchesInTheChosenMode() {
        ColorTheme.OCEAN.save(app);
        prefs.edit().remove("display_mode").apply();
        int systemNight = app.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        assertOceanLaunch(AppearanceMode.SYSTEM, systemNight);
        AppearanceMode.LIGHT.save(app);
        assertOceanLaunch(AppearanceMode.LIGHT, Configuration.UI_MODE_NIGHT_NO);
        AppearanceMode.DARK.save(app);
        assertOceanLaunch(AppearanceMode.DARK, Configuration.UI_MODE_NIGHT_YES);
        AppearanceMode.SYSTEM.save(app);
        assertOceanLaunch(AppearanceMode.SYSTEM, systemNight);
        assertEquals(systemNight, app.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK);
    }

    private void assertOceanLaunch(AppearanceMode expectedMode, int expectedNight) {
        try (ActivityScenario<MainActivity> screen = ActivityScenario.launch(MainActivity.class)) {
            screen.onActivity(activity -> {
                Configuration actual = activity.getResources().getConfiguration();
                assertEquals(expectedNight, actual.uiMode & Configuration.UI_MODE_NIGHT_MASK);
                assertEquals(app.getResources().getConfiguration().fontScale, actual.fontScale, 0);
                assertEquals(app.getResources().getConfiguration().getLocales(), actual.getLocales());
                assertEquals(expectedMode, AppearanceMode.selected(activity));
                assertEquals(ColorTheme.OCEAN, ColorTheme.selected(activity));
                ViewGroup content = activity.findViewById(android.R.id.content);
                int background = ((ColorDrawable) content.getChildAt(0).getBackground()).getColor();
                assertEquals(expectedNight == Configuration.UI_MODE_NIGHT_YES ? 0xFF132F3C : 0xFFE6F3F5, background);
            });
        }
    }

    @Test public void settingsKeepsEachModeAndPaletteIndependent() {
        ColorTheme previous = ColorTheme.VIOLET;
        previous.save(app);
        AppearanceMode.SYSTEM.save(app);
        try (ActivityScenario<MainActivity> screen = ActivityScenario.launch(MainActivity.class)) {
            screen.onActivity(activity -> activity.findViewById(R.id.settings).performClick());
            for (AppearanceMode mode : AppearanceMode.values()) {
                tap(screen, mode.optionId);
                assertSelection(screen, mode, previous);
                for (ColorTheme palette : ColorTheme.values()) {
                    tap(screen, palette.optionId);
                    assertSelection(screen, mode, palette);
                    previous = palette;
                }
            }
            tap(screen, R.id.mode_light);
            tap(screen, R.id.theme_amber);
            screen.recreate();
            assertSelection(screen, AppearanceMode.LIGHT, ColorTheme.AMBER);
        }
        try (ActivityScenario<MainActivity> screen = ActivityScenario.launch(MainActivity.class)) {
            screen.onActivity(activity -> activity.findViewById(R.id.settings).performClick());
            assertSelection(screen, AppearanceMode.LIGHT, ColorTheme.AMBER);
        }
    }

    private static void tap(ActivityScenario<MainActivity> screen, int option) {
        screen.onActivity(activity -> activity.findViewById(option).performClick());
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
    }

    private void assertSelection(ActivityScenario<MainActivity> screen, AppearanceMode mode, ColorTheme palette) {
        screen.onActivity(activity -> {
            RadioGroup modes = activity.findViewById(R.id.appearance_options);
            RadioGroup colors = activity.findViewById(R.id.theme_options);
            assertEquals(mode.optionId, modes.getCheckedRadioButtonId());
            assertEquals(palette.optionId, colors.getCheckedRadioButtonId());
            assertEquals(mode, AppearanceMode.selected(activity));
            assertEquals(palette, ColorTheme.selected(activity));

            Configuration expected = new Configuration();
            expected.uiMode = switch (mode) {
                case LIGHT -> Configuration.UI_MODE_NIGHT_NO;
                case DARK -> Configuration.UI_MODE_NIGHT_YES;
                case SYSTEM -> app.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
            };
            assertEquals(expected.uiMode,
                    activity.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK);
            Context reference = new ContextThemeWrapper(app.createConfigurationContext(expected), palette.style);
            ViewGroup content = activity.findViewById(android.R.id.content);
            assertEquals(ThemeColors.get(reference, R.attr.paletteSurface),
                    ((ColorDrawable) content.getChildAt(0).getBackground()).getColor());
            RadioButton checked = modes.findViewById(mode.optionId);
            assertEquals(ThemeColors.get(reference, R.attr.paletteOnAccent), checked.getCurrentTextColor());
        });
    }
}
