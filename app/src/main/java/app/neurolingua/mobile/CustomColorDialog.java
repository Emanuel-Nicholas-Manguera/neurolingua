package app.neurolingua.mobile;

import android.app.AlertDialog;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;

import java.util.Locale;
import java.util.function.IntConsumer;
import java.util.regex.Pattern;

final class CustomColorDialog {
    private static final Pattern HEX = Pattern.compile("#?[0-9a-fA-F]{6}");
    private static final int[] HUES = {Color.RED, Color.YELLOW, Color.GREEN, Color.CYAN,
            Color.BLUE, Color.MAGENTA, Color.RED};

    final AlertDialog dialog;
    private final MainActivity activity;
    private final View content;
    private final EditText hex;
    private final TextView hexLabel;
    private final ExamSpiralView spiral;
    private final GradientDrawable preview = new GradientDrawable();
    private final SeekBar hue, saturation, brightness;
    private final GradientDrawable saturationTrack, brightnessTrack;
    private final TextView hueValue, saturationValue, brightnessValue;
    private final float[] hsv = new float[3];
    private final boolean dark;
    private int color;
    private boolean syncing;

    CustomColorDialog(MainActivity activity, Bundle draft, IntConsumer onSave) {
        this.activity = activity;
        dark = (activity.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK)
                == Configuration.UI_MODE_NIGHT_YES;
        color = draft == null ? ColorTheme.customColor(activity) : draft.getInt("color");
        Color.colorToHSV(color, hsv);
        if (draft != null) {
            hsv[0] = draft.getFloat("hue", hsv[0]);
            hsv[1] = draft.getFloat("saturation", hsv[1]);
            hsv[2] = draft.getFloat("brightness", hsv[2]);
        }
        content = activity.getLayoutInflater().inflate(R.layout.dialog_custom_color,
                activity.findViewById(android.R.id.content), false);
        if (activity.customUi != null) activity.customUi.apply(content);
        hex = content.findViewById(R.id.color_hex);
        hexLabel = content.findViewById(R.id.color_hex_label);
        spiral = content.findViewById(R.id.custom_spiral);
        preview.setCornerRadius(dp(16));
        content.findViewById(R.id.color_preview_card).setBackground(preview);
        hue = content.findViewById(R.id.hue_slider);
        saturation = content.findViewById(R.id.saturation_slider);
        brightness = content.findViewById(R.id.brightness_slider);
        hueValue = content.findViewById(R.id.hue_value);
        saturationValue = content.findViewById(R.id.saturation_value);
        brightnessValue = content.findViewById(R.id.brightness_value);
        configureSlider(hue, HUES);
        saturationTrack = configureSlider(saturation, new int[]{Color.WHITE, color});
        brightnessTrack = configureSlider(brightness, new int[]{Color.BLACK, color});

        hex.setText(draft == null ? format(color) : draft.getString("hex", format(color)));
        dialog = new AlertDialog.Builder(activity).setTitle(R.string.custom_color).setView(content)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.use_color, (picker, which) -> {
                    picker.dismiss();
                    onSave.accept(color);
                }).create();
        hex.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (!syncing) readHex();
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    void show() {
        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
                | WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        dialog.show();
        if (activity.customUi != null) activity.customUi.dialog(dialog, content);
        render(false);
        validateHex();
    }

    Bundle state() {
        Bundle state = new Bundle();
        state.putInt("color", color);
        state.putFloat("hue", hsv[0]);
        state.putFloat("saturation", hsv[1]);
        state.putFloat("brightness", hsv[2]);
        state.putString("hex", hex.getText().toString());
        return state;
    }

    static String format(int color) {
        return String.format(Locale.ROOT, "#%06X", color & 0xFFFFFF);
    }

    private GradientDrawable configureSlider(SeekBar slider, int[] stops) {
        GradientDrawable track = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, stops);
        track.setCornerRadius(dp(4));
        slider.setProgressTintList(null);
        slider.setThumbTintList(null);
        slider.setProgressDrawable(track);
        GradientDrawable thumb = new GradientDrawable();
        thumb.setShape(GradientDrawable.OVAL);
        thumb.setSize(dp(22), dp(22));
        slider.setThumb(thumb);
        slider.setKeyProgressIncrement(1);
        slider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar bar, int value, boolean user) {
                if (syncing) return;
                if (bar == hue) hsv[0] = value;
                else if (bar == saturation) hsv[1] = value / 100f;
                else hsv[2] = value / 100f;
                color = Color.HSVToColor(hsv);
                render(true);
            }
            @Override public void onStartTrackingTouch(SeekBar bar) {}
            @Override public void onStopTrackingTouch(SeekBar bar) {}
        });
        return track;
    }

    private boolean validateHex() {
        String text = hex.getText().toString().trim();
        boolean valid = HEX.matcher(text).matches();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(valid);
        hex.setError(!valid && text.length() >= 6 ? activity.getString(R.string.hex_error) : null);
        return valid;
    }

    private void readHex() {
        if (!validateHex()) return;
        String text = hex.getText().toString().trim();
        color = Color.parseColor(text.startsWith("#") ? text : "#" + text);
        float previousHue = hsv[0], previousSaturation = hsv[1];
        Color.colorToHSV(color, hsv);
        // A neutral has no hue, and black has no saturation. Keep the slider choices for the next edit.
        if (hsv[1] == 0) hsv[0] = previousHue;
        if (hsv[2] == 0) hsv[1] = previousSaturation;
        render(false);
    }

    private void render(boolean writeHex) {
        syncing = true;
        if (writeHex) {
            hex.setText(format(color));
            hex.setSelection(hex.length());
            validateHex();
        }
        hue.setProgress(Math.min(359, Math.round(hsv[0])));
        saturation.setProgress(Math.round(100 * hsv[1]));
        brightness.setProgress(Math.round(100 * hsv[2]));
        hueValue.setText(activity.getString(R.string.degrees_value, hue.getProgress()));
        saturationValue.setText(activity.getString(R.string.percent_value, saturation.getProgress()));
        brightnessValue.setText(activity.getString(R.string.percent_value, brightness.getProgress()));
        saturationTrack.setColors(new int[]{hsvColor(0, hsv[2]), hsvColor(1, hsv[2])});
        brightnessTrack.setColors(new int[]{Color.BLACK, hsvColor(hsv[1], 1)});
        for (SeekBar slider : new SeekBar[]{hue, saturation, brightness}) {
            GradientDrawable thumb = (GradientDrawable) slider.getThumb();
            thumb.setColor(color);
            thumb.setStroke(dp(2), Color.luminance(color) > .18f ? Color.BLACK : Color.WHITE);
        }
        CustomPalette palette = new CustomPalette(color, dark);
        preview.setColor(palette.get(R.attr.paletteSurface));
        preview.setStroke(dp(1), palette.get(R.attr.paletteOutline));
        hexLabel.setTextColor(palette.get(R.attr.paletteMuted));
        CustomThemeUi.input(hex, palette);
        spiral.preview(palette);
        syncing = false;
    }

    private int hsvColor(float saturation, float value) {
        return Color.HSVToColor(new float[]{hsv[0], saturation, value});
    }

    private int dp(int value) {
        return Math.round(value * activity.getResources().getDisplayMetrics().density);
    }
}
