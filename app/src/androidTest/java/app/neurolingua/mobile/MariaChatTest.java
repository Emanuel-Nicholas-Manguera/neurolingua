package app.neurolingua.mobile;

import static org.junit.Assert.*;

import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.test.core.app.ActivityScenario;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;

public class MariaChatTest {
    @Test public void suggestionsAndTypedMessagesAppendToTheSameThread() {
        try (ActivityScenario<MainActivity> screen = ActivityScenario.launch(MainActivity.class)) {
            screen.onActivity(activity -> {
                activity.findViewById(R.id.help).performClick();
                LinearLayout messages = activity.findViewById(R.id.chat_messages);
                EditText input = activity.findViewById(R.id.chat_input);
                View send = activity.findViewById(R.id.chat_send);
                input.setText("  ");
                input.onEditorAction(EditorInfo.IME_ACTION_SEND);
                assertFalse(send.isEnabled());
                assertEquals(0, messages.getChildCount());
                input.setText("  What can you help me with?  ");

                int[] suggestions = {R.id.suggest_score, R.id.suggest_missed, R.id.suggest_count};
                for (int i = 0; i < suggestions.length; i++) {
                    TextView suggestion = activity.findViewById(suggestions[i]);
                    assertTrue(suggestion.isShown());
                    suggestion.performClick();
                    assertEquals(suggestion.getText().toString(), text(messages, i * 2));
                    assertFalse(text(messages, i * 2 + 1).isEmpty());
                    assertEquals(i * 2 + 2, messages.getChildCount());
                    assertFalse(activity.findViewById(R.id.chat_suggestions).isShown());
                    activity.findViewById(R.id.chat_suggestion_toggle).performClick();
                }
                assertFalse(activity.findViewById(R.id.chat_welcome).isShown());
                assertTrue(send.isEnabled());
                send.performClick();
                assertEquals("What can you help me with?", text(messages, 6));
                assertEquals(RecordAssistant.reply(text(messages, 6), null, false), text(messages, 7));
                assertEquals(Gravity.END, ((LinearLayout.LayoutParams) messages.getChildAt(6).getLayoutParams()).gravity);
                assertEquals(Gravity.START, ((LinearLayout.LayoutParams) messages.getChildAt(7).getLayoutParams()).gravity);
                assertEquals("", input.getText().toString());
                assertFalse(send.isEnabled());
            });
        }
    }

    @Test public void keyboardSendDraftAndConversationSurviveHomeAndRecreation() {
        String question = "What can I ask?";
        String draft = "My next question";
        try (ActivityScenario<MainActivity> screen = ActivityScenario.launch(MainActivity.class)) {
            screen.onActivity(activity -> {
                activity.findViewById(R.id.help).performClick();
                EditText input = activity.findViewById(R.id.chat_input);
                input.setText(question);
                input.onEditorAction(EditorInfo.IME_ACTION_SEND);
                input.setText(draft);
                activity.findViewById(R.id.chat_suggestion_toggle).performClick();
                activity.findViewById(R.id.home).performClick();
                activity.findViewById(R.id.help).performClick();
            });
            screen.recreate();
            screen.onActivity(activity -> {
                LinearLayout messages = activity.findViewById(R.id.chat_messages);
                assertEquals(2, messages.getChildCount());
                assertEquals(question, text(messages, 0));
                assertEquals(RecordAssistant.reply(question, null, false), text(messages, 1));
                assertEquals(draft, ((EditText) activity.findViewById(R.id.chat_input)).getText().toString());
                assertTrue(activity.findViewById(R.id.chat_suggestions).isShown());
            });
        }
    }

    @Test public void savedStateKeepsWholeRecentExchangesAndTheirOriginalAnswers() {
        MariaChat.Conversation thread = new MariaChat.Conversation(null);
        thread.messages.add("Old question");
        thread.messages.add(new String(new char[40_000]));
        thread.messages.add("Last score?");
        thread.messages.add("On 24 November, you scored 2 out of 3.");
        thread.draft = "Which questions";
        thread.suggestionsOpen = false;
        Bundle saved = thread.savedState();
        MariaChat.Conversation restored = new MariaChat.Conversation(saved);
        assertEquals(thread.messages.subList(2, 4), restored.messages);
        assertEquals(thread.draft, restored.draft);
        assertFalse(restored.suggestionsOpen);
        assertEquals(4, thread.messages.size());
    }

    @Test public void aShorterViewportFollowsTheLatestReplyUnlessReadingEarlierMessages() {
        try (ActivityScenario<MainActivity> screen = ActivityScenario.launch(MainActivity.class)) {
            screen.onActivity(activity -> {
                activity.findViewById(R.id.help).performClick();
                EditText input = activity.findViewById(R.id.chat_input);
                for (int i = 0; i < 14; i++) {
                    input.setText("What can you help with?");
                    activity.findViewById(R.id.chat_send).performClick();
                }
            });
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            screen.onActivity(activity -> {
                ScrollView scroll = activity.findViewById(R.id.chat_scroll);
                assertTrue(scroll.canScrollVertically(-1));
                assertFalse(scroll.canScrollVertically(1));
                View chat = (View) scroll.getParent();
                chat.setPadding(0, 0, 0, scroll.getHeight() / 3);
            });
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            screen.onActivity(activity -> {
                ScrollView scroll = activity.findViewById(R.id.chat_scroll);
                assertFalse(scroll.canScrollVertically(1));
                scroll.scrollTo(0, 0);
                View chat = (View) scroll.getParent();
                chat.setPadding(0, 0, 0, chat.getPaddingBottom() + 48);
            });
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            screen.onActivity(activity -> assertEquals(0, activity.findViewById(R.id.chat_scroll).getScrollY()));
        }
    }

    private static String text(LinearLayout messages, int position) {
        return ((TextView) messages.getChildAt(position)).getText().toString();
    }
}
