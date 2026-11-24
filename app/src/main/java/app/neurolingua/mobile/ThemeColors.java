package app.neurolingua.mobile;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.TypedValue;
import android.widget.EditText;

final class ThemeColors {
    private ThemeColors() {}

    static int get(Context context, int attribute) {
        if (context instanceof MainActivity activity && activity.customUi != null) {
            return activity.customUi.colors.get(attribute);
        }
        TypedValue value = new TypedValue();
        if (!context.getTheme().resolveAttribute(attribute, value, true)
                || value.type < TypedValue.TYPE_FIRST_COLOR_INT || value.type > TypedValue.TYPE_LAST_COLOR_INT) {
            throw new IllegalArgumentException("Expected a palette color for "
                    + context.getResources().getResourceEntryName(attribute));
        }
        return value.data;
    }

    static Drawable background(Context context, int resource) {
        if (context instanceof MainActivity activity && activity.customUi != null) {
            return activity.customUi.background(resource);
        }
        return context.getDrawable(resource);
    }

    static void styleInput(EditText input) {
        if (input.getContext() instanceof MainActivity activity && activity.customUi != null) {
            CustomThemeUi.input(input, activity.customUi.colors);
        }
    }
}
