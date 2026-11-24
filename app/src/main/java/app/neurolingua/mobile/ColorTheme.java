package app.neurolingua.mobile;

import android.content.Context;
import java.util.EnumSet;

enum ColorTheme {
    VIOLET("violet", R.style.AppTheme, R.id.theme_violet),
    OCEAN("ocean", R.style.OceanTheme, R.id.theme_ocean),
    ROSE("rose", R.style.RoseTheme, R.id.theme_rose),
    FOREST("forest", R.style.ForestTheme, R.id.theme_forest),
    AMBER("amber", R.style.AmberTheme, R.id.theme_amber),
    SLATE("slate", R.style.SlateTheme, R.id.theme_slate),
    CUSTOM("custom", R.style.AppTheme, R.id.theme_custom);

    private static final String KEY = "color_theme";
    private static final String SEED = "custom_color";
    private final String key;
    final int style;
    final int optionId;

    ColorTheme(String key, int style, int optionId) {
        this.key = key;
        this.style = style;
        this.optionId = optionId;
    }

    static ColorTheme selected(Context context) {
        String saved = AppearancePreferences.of(context).getString(KEY, "");
        for (ColorTheme theme : values()) {
            if (theme.key.equals(saved)) return theme;
        }
        return VIOLET;
    }

    void save(Context context) {
        AppearancePreferences.of(context).edit().putString(KEY, key).apply();
    }

    static EnumSet<ColorTheme> presets() {
        return EnumSet.complementOf(EnumSet.of(CUSTOM));
    }

    static int customColor(Context context) {
        return AppearancePreferences.of(context).getInt(SEED, CustomPalette.DEFAULT_SEED);
    }

    static void useCustom(Context context, int color) {
        AppearancePreferences.of(context).edit().putInt(SEED, color | 0xFF000000)
                .putString(KEY, CUSTOM.key).apply();
    }
}
