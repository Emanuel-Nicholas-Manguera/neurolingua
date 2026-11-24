package app.neurolingua.mobile;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;

/** Draws three folded ribbons around an open center in normalized coordinates. */
final class SpiralRenderer {
    static final double FULL_TURN = 2 * Math.PI;
    private static final int SLICES = 80;
    private static final int COLUMNS = SLICES + 1;
    private static final double[] RIBBON_OFFSETS = {-.041, .013, .039};

    private final int[] shadows;
    private final int[] highlights;
    private final int reflection;
    private final Bitmap white = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
    private final double[] angles = new double[COLUMNS];
    private final float[] unitX = new float[COLUMNS];
    private final float[] unitY = new float[COLUMNS];
    private final double[] outline = new double[COLUMNS];

    // A column samples one angle. Rows step from the inside of a fold to its outer glow.
    // Soft, transparent edge rows give the mesh a smooth silhouette on both backgrounds.
    private final float[] vertices = new float[2 * 10 * COLUMNS];
    private final int[] colors = new int[10 * COLUMNS];

    SpiralRenderer(Context context) {
        shadows = new int[] {context.getColor(R.color.spiral_violet),
                context.getColor(R.color.spiral_rose), context.getColor(R.color.spiral_iris)};
        highlights = new int[] {context.getColor(R.color.spiral_violet_light),
                context.getColor(R.color.spiral_rose_light), context.getColor(R.color.spiral_iris_light)};
        reflection = context.getColor(R.color.spiral_reflection);
        white.eraseColor(Color.WHITE);
        for (int column = 0; column <= SLICES; column++) {
            double angle = FULL_TURN * (column % SLICES) / SLICES;
            angles[column] = angle;
            unitX[column] = (float) Math.cos(angle);
            unitY[column] = (float) Math.sin(angle);
        }
    }

    void draw(Canvas canvas, int width, int height, double phase) {
        float scale = Math.min(width, height) / 2f;
        int save = canvas.save();
        canvas.translate(width / 2f, height / 2f);
        canvas.scale(scale, scale);
        canvas.translate((float) (.027 * Math.sin(phase)), (float) (.021 * Math.cos(phase)));

        double radius = .535 + .020 * Math.sin(phase);
        double lowerAngle = 1.52 + .22 * Math.sin(phase);
        for (int column = 0; column < COLUMNS; column++) {
            double angle = angles[column];
            double edge = radius + .040 * Math.sin(2 * angle - phase + .25)
                    + .022 * Math.cos(3 * angle + phase - .9) + .009 * Math.sin(4 * angle - phase - .4);
            outline[column] = edge;
            double lower = .5 + .5 * Math.cos(angle - lowerAngle);
            double light = lower * lower * lower;
            vertex(0, column, .36 * edge, reflection, 0);
            vertex(1, column, .55 * edge, reflection, .025 * light);
            vertex(2, column, .70 * edge, reflection, .17 * light);
            vertex(3, column, .81 * edge, reflection, .43 * light);
            vertex(4, column, .92 * edge, reflection, .27 * light);
            vertex(5, column, .98 * edge, highlights[0], .25 * light);
            vertex(6, column, edge, shadows[0], .14);
            vertex(7, column, edge + .040, shadows[0], .10);
            vertex(8, column, edge + .075, shadows[0], .045);
            vertex(9, column, edge + .13, shadows[0], 0);
        }
        drawStrip(canvas, 9);
        for (int ribbon = 0; ribbon < RIBBON_OFFSETS.length; ribbon++) {
            drawRibbon(canvas, phase, ribbon);
        }
        canvas.restoreToCount(save);
    }

    private void drawRibbon(Canvas canvas, double phase, int ribbon) {
        double shift = phase + ribbon * 2.10;
        int shadow = shadows[ribbon];
        int highlight = highlights[ribbon];
        for (int column = 0; column < COLUMNS; column++) {
            double angle = angles[column];
            double outer = outline[column] + RIBBON_OFFSETS[ribbon]
                    + .033 * Math.sin(2 * angle + shift) + .015 * Math.cos(3 * angle - shift);
            double roll = .5 + .5 * Math.sin(2 * angle - shift + .57);
            double width = .017 + .060 * Math.pow(roll, 1.15);
            double inner = outer - width;
            double facing = .27 + .73 * (.5 + .5 * Math.cos(angle + shift - .2));
            double glint = .5 + .5 * Math.cos(2 * angle - shift - .4 * ribbon);
            glint = glint * glint * glint;
            int satin = mix(shadow, highlight, .35 + .4 * glint);
            int body = mix(shadow, highlight, .16 + .4 * glint);
            int rim = mix(shadow, highlight, .50 + .35 * glint);

            vertex(0, column, inner - .009, shadow, 0);
            vertex(1, column, inner, body, .42 + .22 * facing);
            vertex(2, column, inner + .24 * width, satin, .63 + .24 * facing);
            vertex(3, column, inner + .62 * width, body, .44 + .39 * facing);
            vertex(4, column, outer - .003, rim, .65 + .26 * facing);
            vertex(5, column, outer + .003, highlight, (.37 + .52 * glint) * (.65 + .35 * facing));
            vertex(6, column, outer + .015, shadow, .14 + .13 * facing);
            vertex(7, column, outer + .045, shadow, 0);
        }
        drawStrip(canvas, 7);
    }

    private void vertex(int row, int column, double radius, int rgb, double opacity) {
        int i = row * COLUMNS + column;
        vertices[2 * i] = (float) radius * unitX[column];
        vertices[2 * i + 1] = (float) radius * unitY[column];
        colors[i] = (rgb & 0x00ffffff) | ((int) (255 * opacity + .5) << 24);
    }

    private void drawStrip(Canvas canvas, int rows) {
        canvas.drawBitmapMesh(white, SLICES, rows, vertices, 0, colors, 0, null);
    }

    private static int mix(int from, int to, double amount) {
        return Color.rgb((int) (Color.red(from) + amount * (Color.red(to) - Color.red(from))),
                (int) (Color.green(from) + amount * (Color.green(to) - Color.green(from))),
                (int) (Color.blue(from) + amount * (Color.blue(to) - Color.blue(from))));
    }
}
