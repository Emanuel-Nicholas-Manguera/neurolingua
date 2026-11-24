package app.neurolingua.mobile;

import android.app.AlertDialog;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Build;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

/** Bind a user-generated palette to native widgets. Presets use the same roles in XML. */
final class CustomThemeUi {
    private static final int[] CHECKED = {android.R.attr.state_checked};
    private static final int[] DISABLED = {-android.R.attr.state_enabled};
    private static final int[] DEFAULT = {};
    private final Context context;
    final CustomPalette colors;

    CustomThemeUi(Context context, CustomPalette colors) {
        this.context = context;
        this.colors = colors;
    }

    @SuppressWarnings("deprecation")
    void window(Window window) {
        int surface = colors.get(R.attr.paletteSurface);
        window.setBackgroundDrawable(new ColorDrawable(surface));
        window.setStatusBarColor(surface);
        window.setNavigationBarColor(Build.VERSION.SDK_INT >= 27 ? surface : colors.get(R.attr.paletteLegacyNavigation));
    }

    void apply(View view) {
        String role = view.getTag() instanceof String tag ? tag : "";
        if (view instanceof TextView text) {
            int textRole = switch (role) {
                case "muted" -> R.attr.paletteMuted;
                case "accent" -> R.attr.paletteAccent;
                case "heroText" -> R.attr.paletteHeroText;
                case "heroAction" -> R.attr.paletteHeroButtonText;
                case "primary" -> R.attr.paletteOnAccent;
                default -> R.attr.paletteText;
            };
            text.setTextColor(colors.get(textRole));
            if ("mode".equals(role)) {
                text.setTextColor(states(CHECKED, R.attr.paletteOnAccent, R.attr.paletteText));
            } else if ("colorOption".equals(role)) {
                Drawable indicator = text.getCompoundDrawablesRelative()[2];
                if (indicator != null) indicator.mutate().setTintList(states(CHECKED, R.attr.paletteAccent, R.attr.paletteMuted));
            } else {
                text.setCompoundDrawableTintList(ColorStateList.valueOf(colors.get(
                        "heroAction".equals(role) ? R.attr.paletteHeroButtonText : R.attr.paletteAccent)));
            }
        }
        if (view instanceof ImageView image) {
            image.setImageTintList(ColorStateList.valueOf(colors.get(
                    "heroLines".equals(role) ? R.attr.paletteHeroLines : R.attr.paletteAccent)));
        }
        switch (role) {
            case "outline" -> view.setBackground(background(R.drawable.button_outline));
            case "navigation" -> view.setBackground(background(R.drawable.button_navigation));
            case "primary" -> view.setBackground(background(R.drawable.button_primary));
            case "heroAction" -> view.setBackground(background(R.drawable.button_cream));
            case "variant" -> view.setBackground(background(R.drawable.surface_lilac));
            case "hero" -> view.setBackground(background(R.drawable.hero));
            case "divider" -> view.setBackgroundColor(colors.get(R.attr.paletteOutline));
            case "mode", "colorOption" -> {
                boolean mode = "mode".equals(role);
                StateListDrawable states = new StateListDrawable();
                states.addState(CHECKED, mode ? background(R.drawable.button_primary)
                        : button(R.drawable.button_outline, R.attr.paletteSurfaceVariant, R.attr.paletteControlRipple,
                                R.attr.paletteAccent, 2));
                states.addState(DEFAULT, background(R.drawable.button_outline));
                view.setBackground(states);
            }
        }
        if (view instanceof ViewGroup group) {
            for (int i = 0; i < group.getChildCount(); i++) apply(group.getChildAt(i));
        }
    }

    Drawable background(int resource) {
        if (resource == R.drawable.button_primary) {
            return button(resource, R.attr.paletteAccent, R.attr.palettePrimaryRipple, 0, 0);
        }
        if (resource == R.drawable.button_cream) {
            return button(resource, R.attr.paletteHeroText, R.attr.paletteHeroRipple, 0, 0);
        }
        if (resource == R.drawable.button_outline || resource == R.drawable.button_navigation) {
            return button(resource, R.attr.paletteSurface, R.attr.paletteControlRipple, R.attr.paletteOutline, 1);
        }
        GradientDrawable shape = (GradientDrawable) context.getDrawable(resource).mutate();
        if (resource == R.drawable.hero) {
            shape.setColors(new int[]{colors.get(R.attr.paletteHeroStart), colors.get(R.attr.paletteHeroEnd)});
        } else {
            shape.setColor(colors.get(R.attr.paletteSurfaceVariant));
        }
        return shape;
    }

    private RippleDrawable button(int resource, int fill, int rippleColor, int stroke, int widthDp) {
        RippleDrawable ripple = (RippleDrawable) context.getDrawable(resource).mutate();
        ripple.setColor(ColorStateList.valueOf(colors.get(rippleColor)));
        GradientDrawable shape = (GradientDrawable) ripple.getDrawable(0);
        shape.setColor(colors.get(fill));
        if (stroke != 0) {
            int pixels = Math.round(context.getResources().getDisplayMetrics().density * widthDp);
            shape.setStroke(pixels, colors.get(stroke));
        }
        return ripple;
    }

    private ColorStateList states(int[] state, int first, int fallback) {
        return new ColorStateList(new int[][]{state, DEFAULT}, new int[]{colors.get(first), colors.get(fallback)});
    }

    void input(EditText input) {
        input.setTextColor(colors.get(R.attr.paletteText));
        input.setHintTextColor(colors.get(R.attr.paletteMuted));
        input.setBackgroundTintList(ColorStateList.valueOf(colors.get(R.attr.paletteAccent)));
        input.setHighlightColor(colors.get(R.attr.paletteControlRipple));
    }

    void dialog(AlertDialog dialog, View content) {
        View decor = dialog.getWindow().getDecorView();
        Drawable background = decor.getBackground();
        if (background != null) background.mutate().setTint(colors.get(R.attr.paletteSurface));
        tintDialogChrome(decor, content);
    }

    private void tintDialogChrome(View view, View content) {
        if (view == content) return;
        if (view instanceof Button button) {
            button.setTextColor(states(DISABLED, R.attr.paletteMuted, R.attr.paletteAccent));
        } else if (view instanceof TextView label) {
            label.setTextColor(colors.get(R.attr.paletteText));
        } else if (view instanceof ViewGroup group) {
            for (int i = 0; i < group.getChildCount(); i++) tintDialogChrome(group.getChildAt(i), content);
        }
    }
}
