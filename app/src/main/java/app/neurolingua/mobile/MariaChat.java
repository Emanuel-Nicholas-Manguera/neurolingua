package app.neurolingua.mobile;

import android.app.Activity;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.ArrayList;

final class MariaChat {
    static final class Conversation {
        final ArrayList<String> messages;
        String draft;
        boolean suggestionsOpen;

        Conversation(Bundle saved) {
            ArrayList<String> history = saved == null ? null : saved.getStringArrayList("messages");
            messages = history == null ? new ArrayList<>() : history;
            draft = saved == null ? "" : saved.getString("draft", "");
            suggestionsOpen = saved == null || saved.getBoolean("suggestionsOpen", true);
        }

        void ask(String question, AssessmentStore store) {
            long scoredId = store.latestId(AssessmentStore.SCORED);
            boolean pending = store.latestId(AssessmentStore.PENDING) > scoredId;
            Assessment scored = scoredId == 0 ? null : store.get(scoredId);
            String reply = RecordAssistant.reply(question, scored, pending);
            messages.add(question);
            messages.add(reply);
            suggestionsOpen = false;
        }

        Bundle savedState() {
            // Keep complete recent exchanges within Android's shared activity-state budget.
            int start = messages.size();
            int remaining = 32_000;
            while (start >= 2) {
                int size = messages.get(start - 2).length() + messages.get(start - 1).length() + 16;
                if (size > remaining) break;
                remaining -= size;
                start -= 2;
            }
            Bundle saved = new Bundle();
            saved.putStringArrayList("messages", new ArrayList<>(messages.subList(start, messages.size())));
            saved.putString("draft", draft);
            saved.putBoolean("suggestionsOpen", suggestionsOpen);
            return saved;
        }
    }

    private final Activity activity;
    private final AssessmentStore store;
    private final Conversation conversation;
    private final ScrollView scroll;
    private final LinearLayout messages;
    private final EditText input;
    private final ImageButton send;
    private final Button suggestionToggle;

    MariaChat(Activity activity, AssessmentStore store, Conversation conversation) {
        this.activity = activity;
        this.store = store;
        this.conversation = conversation;
        scroll = activity.findViewById(R.id.chat_scroll);
        messages = activity.findViewById(R.id.chat_messages);
        input = activity.findViewById(R.id.chat_input);
        send = activity.findViewById(R.id.chat_send);
        suggestionToggle = activity.findViewById(R.id.chat_suggestion_toggle);
        scroll.addOnLayoutChangeListener((view, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
            int oldHeight = oldBottom - oldTop;
            int contentHeight = scroll.getChildAt(0).getHeight();
            if (bottom - top < oldHeight && scroll.getScrollY() + oldHeight >= contentHeight - dp(2)) {
                scroll.scrollTo(0, contentHeight);
            }
        });

        for (int id : new int[]{R.id.suggest_score, R.id.suggest_missed, R.id.suggest_count}) {
            Button suggestion = activity.findViewById(id);
            suggestion.setOnClickListener(v -> ask(suggestion.getText().toString()));
        }
        suggestionToggle.setOnClickListener(v -> {
            conversation.suggestionsOpen = !conversation.suggestionsOpen;
            showSuggestions();
            if (conversation.suggestionsOpen) scrollToLatest();
        });

        ThemeColors.styleInput(input);
        input.setText(conversation.draft);
        input.setSelection(input.length());
        input.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable text) {
                conversation.draft = text.toString();
                updateSend();
            }
        });
        input.setOnEditorActionListener((v, action, key) -> {
            if (key != null && key.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
                if (key.getAction() == KeyEvent.ACTION_DOWN && key.getRepeatCount() == 0) sendDraft();
                return true;
            }
            if (action != EditorInfo.IME_ACTION_SEND) return false;
            sendDraft();
            return true;
        });
        send.setOnClickListener(v -> sendDraft());
        updateSend();

        for (int i = 0; i < conversation.messages.size(); i += 2) addExchange(i, false);
        showConversation();
        if (!conversation.messages.isEmpty()) scrollToLatest();
    }

    private void updateSend() {
        boolean hasText = !conversation.draft.trim().isEmpty();
        send.setEnabled(hasText);
        send.setAlpha(hasText ? 1f : 0.42f);
    }

    private void sendDraft() {
        String question = conversation.draft.trim();
        if (question.isEmpty()) return;
        ask(question);
        input.setText("");
    }

    private void ask(String question) {
        conversation.ask(question, store);
        showConversation();
        addExchange(conversation.messages.size() - 2, true);
        scrollToLatest();
    }

    private void addExchange(int index, boolean announce) {
        addBubble(conversation.messages.get(index), true, false);
        addBubble(conversation.messages.get(index + 1), false, announce);
    }

    private void addBubble(String text, boolean outgoing, boolean announce) {
        TextView bubble = new TextView(activity);
        bubble.setTextSize(16);
        bubble.setTextColor(ThemeColors.get(activity, outgoing ? R.attr.paletteOnAccent : R.attr.paletteText));
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(ThemeColors.get(activity, outgoing ? R.attr.paletteAccent : R.attr.paletteSurfaceVariant));
        shape.setCornerRadius(dp(18));
        bubble.setBackground(shape);
        bubble.setPadding(dp(15), dp(10), dp(15), dp(10));
        bubble.setMaxWidth(dp(480));
        LinearLayout.LayoutParams layout = new LinearLayout.LayoutParams(-2, -2);
        layout.gravity = outgoing ? Gravity.END : Gravity.START;
        layout.topMargin = dp(outgoing ? 14 : 6);
        if (outgoing) layout.setMarginStart(dp(40));
        else layout.setMarginEnd(dp(40));
        if (announce) bubble.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        messages.addView(bubble, layout);
        bubble.setText(text);
        bubble.setContentDescription(activity.getString(outgoing ? R.string.you_said : R.string.maria_replied, text));
    }

    private void showConversation() {
        boolean empty = conversation.messages.isEmpty();
        activity.findViewById(R.id.chat_welcome).setVisibility(empty ? View.VISIBLE : View.GONE);
        messages.setVisibility(empty ? View.GONE : View.VISIBLE);
        showSuggestions();
    }

    private void showSuggestions() {
        boolean open = conversation.suggestionsOpen;
        activity.findViewById(R.id.chat_suggestions).setVisibility(open ? View.VISIBLE : View.GONE);
        suggestionToggle.setContentDescription(activity.getString(open ? R.string.hide_questions : R.string.show_questions));
        suggestionToggle.getCompoundDrawablesRelative()[2].setLevel(open ? 10_000 : 0);
    }

    private void scrollToLatest() {
        scroll.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override public void onGlobalLayout() {
                scroll.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                scroll.scrollTo(0, scroll.getChildAt(0).getHeight());
            }
        });
    }

    private int dp(int value) {
        return Math.round(value * activity.getResources().getDisplayMetrics().density);
    }
}
