package app.neurolingua.mobile;

import static org.junit.Assert.assertEquals;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.test.core.app.ActivityScenario;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class CustomThemeTest {
    private Context app;
    private SharedPreferences prefs;
    private String originalMode, originalTheme;
    private Integer originalSeed;

    @Before public void rememberAppearance() {
        app = InstrumentationRegistry.getInstrumentation().getTargetContext();
        prefs = AppearancePreferences.of(app);
        originalMode = prefs.getString("display_mode", null);
        originalTheme = prefs.getString("color_theme", null);
        originalSeed = prefs.contains("custom_color") ? ColorTheme.customColor(app) : null;
    }

    @After public void restoreAppearance() {
        SharedPreferences.Editor edit = prefs.edit().putString("display_mode", originalMode)
                .putString("color_theme", originalTheme);
        if (originalSeed == null) edit.remove("custom_color");
        else edit.putInt("custom_color", originalSeed);
        edit.commit();
    }

    @Test public void customHomeUsesTheSameSeedInEitherModeAndRemembersItAfterAPreset() {
        int coral = 0xFFEF7052;
        ColorTheme.useCustom(app, coral);
        for (AppearanceMode mode : new AppearanceMode[]{AppearanceMode.LIGHT, AppearanceMode.DARK}) {
            mode.save(app);
            CustomPalette expected = new CustomPalette(coral, mode == AppearanceMode.DARK);
            try (ActivityScenario<MainActivity> screen = ActivityScenario.launch(MainActivity.class)) {
                screen.onActivity(activity -> {
                    assertEquals(ColorTheme.CUSTOM, ColorTheme.selected(activity));
                    ViewGroup content = activity.findViewById(android.R.id.content);
                    int surface = ((ColorDrawable) content.getChildAt(0).getBackground()).getColor();
                    assertEquals(expected.get(R.attr.paletteSurface), surface);
                    GradientDrawable hero = (GradientDrawable) activity.findViewById(R.id.assessment_card).getBackground();
                    assertEquals(expected.get(R.attr.paletteHeroStart), hero.getColors()[0]);
                    Button start = activity.findViewById(R.id.start_test);
                    assertEquals(expected.get(R.attr.paletteHeroButtonText), start.getCurrentTextColor());
                    assertEquals(coral, ThemeColors.get(activity, R.attr.spiralFirst));
                });
            }
        }
        ColorTheme.OCEAN.save(app);
        assertEquals(coral, ColorTheme.customColor(app));
        assertEquals(AppearanceMode.DARK, AppearanceMode.selected(app));
    }
}
