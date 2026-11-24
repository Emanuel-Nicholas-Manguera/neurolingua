package app.neurolingua.mobile;

import android.app.Activity;
import android.content.Context;
import android.text.InputFilter;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

final class MariaSheet {
    final DetailSheet sheet;
    final EditText input;
    private final AssessmentStore store;
    private final TextView exchange;
    String lastQuestion = "";

    MariaSheet(Activity activity, AssessmentStore store) {
        this.store = store;
        sheet = new DetailSheet(activity, activity.getString(R.string.ask_maria), activity.getString(R.string.help_intro));
        for (int label : new int[]{R.string.ask_score, R.string.ask_missed, R.string.ask_count}) {
            String question = activity.getString(label);
            Button suggestion = sheet.button(question, false);
            suggestion.setGravity(android.view.Gravity.START | android.view.Gravity.CENTER_VERTICAL);
            suggestion.setOnClickListener(v -> ask(question));
            sheet.add(suggestion, 9);
        }
        exchange = sheet.text("", 16, R.attr.paletteText);
        exchange.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        exchange.setBackground(ThemeColors.background(activity, R.drawable.surface_lilac));
        int pad = sheet.dp(13);
        exchange.setPadding(pad, pad, pad, pad);
        exchange.setVisibility(View.GONE);
        sheet.add(exchange, 12);

        LinearLayout compose = new LinearLayout(activity);
        compose.setGravity(android.view.Gravity.CENTER_VERTICAL);
        input = new EditText(activity);
        ThemeColors.styleInput(input);
        input.setHint(R.string.ask_hint);
        input.setTextSize(16);
        input.setFilters(new InputFilter[]{new InputFilter.LengthFilter(300)});
        input.setSingleLine(true);
        input.setImeOptions(EditorInfo.IME_ACTION_SEND);
        input.setOnEditorActionListener((v, action, keyEvent) -> {
            if (action != EditorInfo.IME_ACTION_SEND) return false;
            send();
            return true;
        });
        compose.addView(input, new LinearLayout.LayoutParams(0, -2, 1));
        Button send = sheet.button(activity.getString(R.string.send), true);
        send.setOnClickListener(v -> send());
        compose.addView(send);
        sheet.add(compose, 12);
    }

    private void send() {
        String question = input.getText().toString().trim();
        if (question.isEmpty()) {
            input.setError(sheet.activity.getString(R.string.ask_hint));
            return;
        }
        ask(question);
        input.setText("");
    }

    void ask(String question) {
        long scoredId = store.latestId(AssessmentStore.SCORED);
        boolean pending = store.latestId(AssessmentStore.PENDING) > scoredId;
        Assessment scored = scoredId == 0 ? null : store.get(scoredId);
        lastQuestion = question;
        String reply = RecordAssistant.reply(question, scored, pending);
        exchange.setText(sheet.activity.getString(R.string.exchange, question, reply));
        exchange.setVisibility(View.VISIBLE);
        if (input.hasFocus()) {
            InputMethodManager keyboard = (InputMethodManager) sheet.activity.getSystemService(Context.INPUT_METHOD_SERVICE);
            keyboard.hideSoftInputFromWindow(input.getWindowToken(), 0);
            sheet.body.requestFocus();
        }
    }
}
