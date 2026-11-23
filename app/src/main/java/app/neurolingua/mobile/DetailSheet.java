package app.neurolingua.mobile;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/** A scrollable bottom sheet shared by record details and app help. */
final class DetailSheet {
    final Activity activity;
    final Dialog dialog;
    final LinearLayout body;

    DetailSheet(Activity activity, String heading, String subtitle) {
        this.activity = activity;
        dialog = new Dialog(activity);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        ScrollView scroll = new ScrollView(activity);
        GradientDrawable background = new GradientDrawable();
        background.setColor(activity.getColor(R.color.paper));
        int r = dp(24);
        background.setCornerRadii(new float[]{r, r, r, r, 0, 0, 0, 0});
        scroll.setBackground(background);
        body = new LinearLayout(activity);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(dp(22), dp(16), dp(22), dp(22));
        body.setFocusableInTouchMode(true);
        scroll.addView(body);
        dialog.setContentView(scroll);
        LinearLayout header = new LinearLayout(activity);
        header.setGravity(Gravity.CENTER_VERTICAL);
        TextView title = text(heading, 27, R.color.ink);
        title.setTypeface(Typeface.SERIF);
        header.addView(title, new LinearLayout.LayoutParams(0, -2, 1));
        Button close = button(activity.getString(R.string.close), false);
        close.setOnClickListener(v -> dialog.dismiss());
        header.addView(close);
        add(header, 0);
        add(text(subtitle, 15, R.color.muted), 6);
    }

    int dp(int value) {
        return Math.round(value * activity.getResources().getDisplayMetrics().density);
    }

    void add(View child, int topMargin) {
        LinearLayout.LayoutParams layout = new LinearLayout.LayoutParams(-1, -2);
        layout.topMargin = dp(topMargin);
        body.addView(child, layout);
    }

    TextView text(String value, int size, int color) {
        TextView label = new TextView(activity);
        label.setText(value);
        label.setTextSize(size);
        label.setTextColor(activity.getColor(color));
        return label;
    }

    Button button(String label, boolean primary) {
        Button button = new Button(activity);
        button.setText(label);
        button.setAllCaps(false);
        button.setTextSize(16);
        button.setLetterSpacing(0);
        button.setTextColor(activity.getColor(primary ? R.color.on_plum : R.color.plum));
        button.setMinHeight(dp(48));
        button.setStateListAnimator(null);
        button.setPadding(dp(14), dp(8), dp(14), dp(8));
        button.setBackgroundResource(primary ? R.drawable.button_primary : R.drawable.button_outline);
        return button;
    }

    void divider() {
        View rule = new View(activity);
        rule.setBackgroundColor(activity.getColor(R.color.line));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, dp(1));
        params.topMargin = dp(14);
        body.addView(rule, params);
    }

    void show() {
        Window window = dialog.getWindow();
        window.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
        window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
                | WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        dialog.show();
        int width = Math.min(activity.getResources().getDisplayMetrics().widthPixels, dp(540));
        window.setLayout(width, WindowManager.LayoutParams.WRAP_CONTENT);
    }
}
