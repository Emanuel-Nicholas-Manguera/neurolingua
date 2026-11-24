package app.neurolingua.mobile;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.view.ViewGroup;
import android.view.inspector.WindowInspector;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.SeekBar;

import androidx.test.core.app.ActivityScenario;
import androidx.test.filters.SdkSuppress;
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

    @Test @SdkSuppress(minSdkVersion = 29)
    public void pickerValidatesDraftsRestoresThemAndAppliesOnlyOnConfirmation() {
        ColorTheme.OCEAN.save(app);
        AppearanceMode.DARK.save(app);
        try (ActivityScenario<MainActivity> screen = ActivityScenario.launch(MainActivity.class)) {
            screen.onActivity(activity -> {
                activity.findViewById(R.id.settings).performClick();
                activity.findViewById(R.id.theme_custom).performClick();
                View picker = pickerWindow();
                ((EditText) picker.findViewById(R.id.color_hex)).setText("#FFzz");
                assertFalse(picker.findViewById(android.R.id.button1).isEnabled());
                assertEquals(ColorTheme.OCEAN, ColorTheme.selected(activity));
            });
            screen.recreate();
            screen.onActivity(activity -> {
                View picker = pickerWindow();
                EditText hex = picker.findViewById(R.id.color_hex);
                assertEquals("#FFzz", hex.getText().toString());
                assertFalse(picker.findViewById(android.R.id.button1).isEnabled());
                hex.setText("Ff0000");
                SeekBar hue = picker.findViewById(R.id.hue_slider);
                assertEquals(0, hue.getProgress());
                hue.setProgress(120);
                assertEquals("#00FF00", hex.getText().toString());
                ((SeekBar) picker.findViewById(R.id.saturation_slider)).setProgress(0);
                assertEquals("#FFFFFF", hex.getText().toString());
                ((SeekBar) picker.findViewById(R.id.brightness_slider)).setProgress(0);
                assertEquals("#000000", hex.getText().toString());
                hex.setText("#18b8aa");
                assertTrue(picker.findViewById(android.R.id.button1).isEnabled());
                picker.findViewById(android.R.id.button1).performClick();
            });
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            screen.onActivity(activity -> {
                assertTrue(((RadioButton) activity.findViewById(R.id.theme_custom)).isChecked());
                assertEquals(0xFF18B8AA, ColorTheme.customColor(activity));
                activity.findViewById(R.id.mode_light).performClick();
            });
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            screen.onActivity(activity -> {
                assertTrue(((RadioButton) activity.findViewById(R.id.theme_custom)).isChecked());
                activity.findViewById(R.id.theme_custom).performClick();
                View picker = pickerWindow();
                EditText hex = picker.findViewById(R.id.color_hex);
                assertEquals("#18B8AA", hex.getText().toString());
                hex.setText("#F06452");
                picker.findViewById(android.R.id.button2).performClick();
            });
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            screen.onActivity(activity -> {
                assertEquals(ColorTheme.CUSTOM, ColorTheme.selected(activity));
                assertEquals(0xFF18B8AA, ColorTheme.customColor(activity));
                assertEquals(AppearanceMode.LIGHT, AppearanceMode.selected(activity));
            });
        }
    }

    @Test public void assistantStaysBelowHomeContentWithAThemedIconInEitherMode() {
        int coral = 0xFFF06452;
        ColorTheme.useCustom(app, coral);
        for (AppearanceMode mode : new AppearanceMode[]{AppearanceMode.LIGHT, AppearanceMode.DARK}) {
            mode.save(app);
            try (ActivityScenario<MainActivity> screen = ActivityScenario.launch(MainActivity.class)) {
                InstrumentationRegistry.getInstrumentation().waitForIdleSync();
                screen.onActivity(activity -> {
                    ImageButton assistant = activity.findViewById(R.id.help);
                    ViewGroup home = (ViewGroup) assistant.getParent();
                    View scroll = activity.findViewById(R.id.home_scroll);
                    float density = activity.getResources().getDisplayMetrics().density;
                    assertEquals(Math.round(56 * density), assistant.getWidth());
                    assertEquals(assistant.getWidth(), assistant.getHeight());
                    int endMargin = home.getLayoutDirection() == View.LAYOUT_DIRECTION_RTL
                            ? assistant.getLeft() : home.getWidth() - assistant.getRight();
                    assertEquals(Math.round(20 * density), endMargin);
                    assertEquals(Math.round(16 * density), home.getHeight() - assistant.getBottom());
                    assertTrue(scroll.getBottom() <= assistant.getTop());
                    assertEquals("Ask Maria", assistant.getContentDescription().toString());
                    assertEquals(assistant.getContentDescription(), assistant.getTooltipText());
                    CustomPalette palette = new CustomPalette(coral, mode == AppearanceMode.DARK);
                    assertEquals(palette.get(R.attr.paletteOnAccent), assistant.getImageTintList().getDefaultColor());
                });
            }
        }
    }

    @android.annotation.TargetApi(29)
    private static View pickerWindow() {
        for (View root : WindowInspector.getGlobalWindowViews()) {
            if (root.findViewById(R.id.color_hex) != null) return root;
        }
        throw new AssertionError("Expected the custom color dialog");
    }
}
