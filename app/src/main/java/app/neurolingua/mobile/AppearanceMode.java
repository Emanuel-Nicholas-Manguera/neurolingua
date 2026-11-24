package app.neurolingua.mobile;

import android.content.Context;
import android.content.res.Configuration;
import android.view.ContextThemeWrapper;

enum AppearanceMode {
    SYSTEM("system", Configuration.UI_MODE_NIGHT_UNDEFINED),
    LIGHT("light", Configuration.UI_MODE_NIGHT_NO),
    DARK("dark", Configuration.UI_MODE_NIGHT_YES);

    private static final String KEY = "display_mode";
    private final String key;
    private final int nightMode;

    AppearanceMode(String key, int nightMode) {
        this.key = key;
        this.nightMode = nightMode;
    }

    static AppearanceMode selected(Context context) {
        String saved = AppearancePreferences.of(context).getString(KEY, "");
        for (AppearanceMode mode : values()) {
            if (mode.key.equals(saved)) return mode;
        }
        return SYSTEM;
    }

    void save(Context context) {
        AppearancePreferences.of(context).edit().putString(KEY, key).apply();
    }

    void applyTo(ContextThemeWrapper activity) {
        if (this == SYSTEM) return;
        // A configuration delta leaves font size, locale and other device settings inherited.
        Configuration override = new Configuration();
        override.uiMode = nightMode;
        activity.applyOverrideConfiguration(override);
    }
}
