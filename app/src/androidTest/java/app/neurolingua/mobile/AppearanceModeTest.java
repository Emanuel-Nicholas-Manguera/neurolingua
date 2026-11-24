package app.neurolingua.mobile;

import static org.junit.Assert.assertEquals;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.drawable.ColorDrawable;
import android.view.ViewGroup;

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
}
