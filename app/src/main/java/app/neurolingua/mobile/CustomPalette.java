package app.neurolingua.mobile;

import android.graphics.Color;
import android.util.SparseIntArray;

/** Light and dark roles derived from one opaque sRGB color. */
final class CustomPalette {
    static final int DEFAULT_SEED = 0xFF8957BB;
    private final SparseIntArray colors = new SparseIntArray(23);

    CustomPalette(int seed, boolean dark) {
        seed |= 0xFF000000;
        int surface = dark ? blend(0xFF18181B, seed, .16f) : blend(Color.WHITE, seed, .105f);
        int variant = dark ? blend(0xFF252529, seed, .20f) : blend(Color.WHITE, seed, .19f);
        int ink = tone(seed, dark ? .90f : .018f);
        int accent = tone(seed, dark ? .60f : .087f);
        int heroText = tone(seed, .95f);
        int onAccent = dark ? tone(seed, .016f) : heroText;
        colors.put(R.attr.paletteSurface, surface);
        colors.put(R.attr.paletteSurfaceVariant, variant);
        colors.put(R.attr.paletteText, ink);
        colors.put(R.attr.paletteMuted, tone(seed, dark ? .57f : .082f));
        colors.put(R.attr.paletteAccent, accent);
        colors.put(R.attr.paletteOnAccent, onAccent);
        colors.put(R.attr.paletteOutline, tone(seed, dark ? .26f : .19f));
        colors.put(R.attr.paletteHeroText, heroText);
        colors.put(R.attr.paletteHeroButtonText, tone(seed, .055f));
        colors.put(R.attr.paletteHeroStart, tone(seed, .045f));
        colors.put(R.attr.paletteHeroEnd, tone(seed, .115f));
        colors.put(R.attr.paletteHeroLines, alpha(heroText, 82));
        colors.put(R.attr.paletteControlRipple, alpha(accent, 45));
        colors.put(R.attr.palettePrimaryRipple, alpha(onAccent, 45));
        colors.put(R.attr.paletteHeroRipple, alpha(tone(seed, .08f), 38));
        colors.put(R.attr.paletteLegacyNavigation, dark ? surface : ink);

        float[] hsv = new float[3];
        Color.colorToHSV(seed, hsv);
        int warm = shifted(hsv, 16);
        int cool = shifted(hsv, -19);
        // Draw the actual seed in the first fold. Companion folds remain visible at both extremes.
        colors.put(R.attr.spiralFirst, seed);
        colors.put(R.attr.spiralFirstLight, tone(seed, dark ? .72f : .38f));
        colors.put(R.attr.spiralSecond, tone(warm, dark ? .19f : .11f));
        colors.put(R.attr.spiralSecondLight, tone(warm, dark ? .67f : .44f));
        colors.put(R.attr.spiralThird, tone(cool, dark ? .145f : .085f));
        colors.put(R.attr.spiralThirdLight, tone(cool, dark ? .59f : .33f));
        colors.put(R.attr.spiralReflection, tone(seed, dark ? .38f : .27f));
    }

    int get(int attribute) {
        int index = colors.indexOfKey(attribute);
        if (index < 0) throw new IllegalArgumentException("Unknown color role " + attribute);
        return colors.valueAt(index);
    }

    private static int shifted(float[] hsv, int degrees) {
        return Color.HSVToColor(new float[]{(hsv[0] + degrees + 360) % 360, hsv[1], hsv[2]});
    }

    private static int alpha(int color, int opacity) {
        return (color & 0x00FFFFFF) | (opacity << 24);
    }

    private static int blend(int first, int last, float weight) {
        return Color.rgb(Math.round(Color.red(first) + weight * (Color.red(last) - Color.red(first))),
                Math.round(Color.green(first) + weight * (Color.green(last) - Color.green(first))),
                Math.round(Color.blue(first) + weight * (Color.blue(last) - Color.blue(first))));
    }

    /** Interpolate toward black or white to keep contrast predictable even for yellow and gray. */
    private static int tone(int seed, float luminance) {
        boolean lighten = Color.luminance(seed) < luminance;
        int end = lighten ? Color.WHITE : Color.BLACK;
        float low = 0, high = 1;
        for (int step = 0; step < 9; step++) {
            float mid = (low + high) / 2;
            if ((Color.luminance(blend(seed, end, mid)) < luminance) == lighten) low = mid;
            else high = mid;
        }
        return blend(seed, end, (low + high) / 2);
    }
}
