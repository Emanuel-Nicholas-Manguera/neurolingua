package app.neurolingua.mobile;

import android.content.Context;
import android.content.SharedPreferences;

final class AppearancePreferences {
    private AppearancePreferences() {}

    static SharedPreferences of(Context context) {
        return context.getSharedPreferences("appearance", Context.MODE_PRIVATE);
    }
}
