package app.neurolingua.mobile;

import android.content.Context;

enum ColorTheme {
    VIOLET("violet", R.style.AppTheme, R.id.theme_violet),
    OCEAN("ocean", R.style.OceanTheme, R.id.theme_ocean),
    ROSE("rose", R.style.RoseTheme, R.id.theme_rose),
    FOREST("forest", R.style.ForestTheme, R.id.theme_forest),
    AMBER("amber", R.style.AmberTheme, R.id.theme_amber),
    SLATE("slate", R.style.SlateTheme, R.id.theme_slate);

    private static final String KEY = "color_theme";
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
}
