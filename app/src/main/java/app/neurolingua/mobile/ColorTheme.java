package app.neurolingua.mobile;

import android.content.Context;
import android.content.SharedPreferences;

enum ColorTheme {
    VIOLET("violet", R.style.AppTheme, R.id.theme_violet),
    OCEAN("ocean", R.style.OceanTheme, R.id.theme_ocean),
    ROSE("rose", R.style.RoseTheme, R.id.theme_rose);

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
        String saved = preferences(context).getString(KEY, "");
        for (ColorTheme theme : values()) {
            if (theme.key.equals(saved)) return theme;
        }
        return VIOLET;
    }

    void save(Context context) {
        preferences(context).edit().putString(KEY, key).apply();
    }

    private static SharedPreferences preferences(Context context) {
        return context.getSharedPreferences("appearance", Context.MODE_PRIVATE);
    }
}
