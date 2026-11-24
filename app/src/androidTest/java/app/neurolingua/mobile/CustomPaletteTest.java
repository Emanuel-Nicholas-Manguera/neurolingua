package app.neurolingua.mobile;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.graphics.Color;
import org.junit.Test;

public class CustomPaletteTest {
    @Test public void lightDarkAndNeutralColorsKeepReadableSurfacesAndTheChosenRibbon() {
        for (int red : new int[]{0, 85, 170, 255}) {
            for (int green : new int[]{0, 85, 170, 255}) {
                for (int blue : new int[]{0, 85, 170, 255}) {
                    int seed = Color.rgb(red, green, blue);
                    for (boolean dark : new boolean[]{false, true}) {
                        CustomPalette palette = new CustomPalette(seed, dark);
                        assertEquals(seed, palette.get(R.attr.spiralFirst));
                        for (int surface : new int[]{R.attr.paletteSurface, R.attr.paletteSurfaceVariant}) {
                            assertReadable(palette, R.attr.paletteText, surface);
                            assertReadable(palette, R.attr.paletteMuted, surface);
                            assertReadable(palette, R.attr.paletteAccent, surface);
                        }
                        assertReadable(palette, R.attr.paletteOnAccent, R.attr.paletteAccent);
                        assertReadable(palette, R.attr.paletteHeroText, R.attr.paletteHeroStart);
                        assertReadable(palette, R.attr.paletteHeroText, R.attr.paletteHeroEnd);
                        assertReadable(palette, R.attr.paletteHeroButtonText, R.attr.paletteHeroText);
                    }
                }
            }
        }
    }

    private static void assertReadable(CustomPalette palette, int foreground, int background) {
        float first = Color.luminance(palette.get(foreground));
        float second = Color.luminance(palette.get(background));
        float ratio = (Math.max(first, second) + .05f) / (Math.min(first, second) + .05f);
        assertTrue("Text contrast " + ratio + " for roles " + foreground + " and " + background, ratio >= 4.5f);
    }
}
