package app.neurolingua.mobile;

import android.content.Context;
import android.util.TypedValue;

final class ThemeColors {
    private ThemeColors() {}

    static int get(Context context, int attribute) {
        TypedValue value = new TypedValue();
        if (!context.getTheme().resolveAttribute(attribute, value, true)
                || value.type < TypedValue.TYPE_FIRST_COLOR_INT || value.type > TypedValue.TYPE_LAST_COLOR_INT) {
            throw new IllegalArgumentException("Expected a palette color for "
                    + context.getResources().getResourceEntryName(attribute));
        }
        return value.data;
    }
}
