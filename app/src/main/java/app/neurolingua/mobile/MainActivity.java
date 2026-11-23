package app.neurolingua.mobile;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Insets;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.window.OnBackInvokedDispatcher;

import java.time.LocalDate;

public final class MainActivity extends Activity {
    private FrameLayout root;
    private AssessmentStore store;
    private PromptSpeaker speaker;
    private boolean foreground;
    private boolean readOnResume;
    private long sessionId;
    private String page = "home";
    private AlertDialog editor;
    private EditText answerInput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        root = new FrameLayout(this);
        root.setBackgroundColor(getColor(R.color.paper));
        if (Build.VERSION.SDK_INT >= 30) {
            getWindow().setDecorFitsSystemWindows(false);
            root.setOnApplyWindowInsetsListener((view, insets) -> {
                Insets space = insets.getInsets(WindowInsets.Type.systemBars()
                        | WindowInsets.Type.displayCutout() | WindowInsets.Type.ime());
                view.setPadding(space.left, space.top, space.right, space.bottom);
                return insets;
            });
        }
        setContentView(root);
        store = new AssessmentStore(this);
        speaker = new PromptSpeaker(this);
        if (Build.VERSION.SDK_INT >= 33) {
            getOnBackInvokedDispatcher().registerOnBackInvokedCallback(
                    OnBackInvokedDispatcher.PRIORITY_DEFAULT, this::goBack);
        }
        if (savedInstanceState != null) {
            sessionId = savedInstanceState.getLong("sessionId");
            page = savedInstanceState.getString("page", "home");
        }
        if (!"home".equals(page) && store.get(sessionId) != null) {
            showExam();
            if (savedInstanceState != null && savedInstanceState.containsKey("editing")
                    && "exam".equals(page)) {
                readOnResume = false;
                editAnswer(store.get(sessionId).currentItem(), savedInstanceState.getString("editing"));
            }
        } else {
            showHome();
        }
    }

    private void screen(String name, int layout) {
        speaker.cancel();
        readOnResume = false;
        page = name;
        root.removeAllViews();
        getLayoutInflater().inflate(layout, root, true);
    }

    private void showHome() {
        screen("home", R.layout.screen_home);
        boolean resume = store.latestId(AssessmentStore.ACTIVE) != 0;
        Button start = findViewById(R.id.start_test);
        start.setText(resume ? R.string.resume : R.string.start);
        start.setOnClickListener(v -> {
            sessionId = store.startOrResume(LocalDate.now());
            showExam();
        });
        if (store.latestId(AssessmentStore.PENDING) != 0) {
            findViewById(R.id.pending).setVisibility(View.VISIBLE);
        }
    }

    private void showExam() {
        Assessment session = store.get(sessionId);
        Assessment.Item item = session.currentItem();
        if (item == null) {
            screen("done", R.layout.screen_complete);
            findViewById(R.id.home).setOnClickListener(v -> showHome());
            return;
        }
        screen("exam", R.layout.screen_exam);
        findViewById(R.id.home).setOnClickListener(v -> showHome());
        TextView number = findViewById(R.id.question_number);
        number.setText(getString(R.string.progress_text, item.position() + 1, session.items().size()));
        ProgressBar progress = findViewById(R.id.progress);
        progress.setMax(session.items().size());
        progress.setProgress(item.position() + 1);
        TextView prompt = findViewById(R.id.prompt);
        prompt.setText(item.prompt());
        renderAnswer(session, item);
    }

    private void renderAnswer(Assessment session, Assessment.Item item) {
        boolean review = !item.answer().isEmpty();
        TextView status = findViewById(R.id.turn_status);
        TextView hint = findViewById(R.id.turn_hint);
        TextView transcript = findViewById(R.id.transcript);
        Button primary = findViewById(R.id.primary_action);
        Button secondary = findViewById(R.id.secondary_action);
        status.setText(review ? R.string.captured : R.string.ready);
        hint.setText(review ? R.string.check_typed : R.string.type_hint);
        secondary.setVisibility(review ? View.VISIBLE : View.GONE);
        if (review) {
            transcript.setVisibility(View.VISIBLE);
            transcript.setText(item.answer());
            secondary.setText(R.string.edit_answer);
            secondary.setOnClickListener(v -> editAnswer(item, item.answer()));
            boolean last = item.position() + 1 == session.items().size();
            primary.setText(last ? R.string.finish_test : R.string.next_question);
            primary.setOnClickListener(v -> {
                store.confirm(session.id(), item.position());
                showExam();
            });
        } else {
            secondary.setVisibility(View.VISIBLE);
            secondary.setText(R.string.repeat);
            secondary.setOnClickListener(v -> readPrompt(item));
            primary.setText(R.string.type_answer);
            primary.setOnClickListener(v -> editAnswer(item, ""));
            if (foreground) readPrompt(item);
            else readOnResume = true;
        }
    }

    private void readPrompt(Assessment.Item item) {
        TextView status = findViewById(R.id.turn_status);
        TextView hint = findViewById(R.id.turn_hint);
        status.setText(R.string.reading);
        hint.setText(R.string.listen_prompt);
        speaker.read(item.prompt(), played -> {
            status.setText(R.string.ready);
            hint.setText(played ? R.string.type_hint : R.string.playback_unavailable);
        });
    }

    private void editAnswer(Assessment.Item item, String initial) {
        speaker.cancel();
        readOnResume = false;
        FrameLayout fields = new FrameLayout(this);
        int margin = Math.round(24 * getResources().getDisplayMetrics().density);
        fields.setPadding(margin, margin / 2, margin, 0);
        answerInput = new EditText(this);
        answerInput.setHint(R.string.answer_hint);
        answerInput.setSingleLine(true);
        answerInput.setText(initial);
        answerInput.setSelection(answerInput.length());
        fields.addView(answerInput);
        editor = new AlertDialog.Builder(this).setTitle(R.string.answer_hint).setView(fields)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.review_answer, null).create();
        editor.setOnShowListener(dialog -> {
            editor.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                String text = answerInput.getText().toString().trim();
                if (text.isEmpty()) {
                    answerInput.setError(getString(R.string.enter_answer));
                    return;
                }
                store.saveDraft(sessionId, item.position(), text, "typed");
                editor.dismiss();
                showExam();
            });
            answerInput.requestFocus();
            InputMethodManager keyboard = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            keyboard.showSoftInput(answerInput, InputMethodManager.SHOW_IMPLICIT);
        });
        editor.setOnDismissListener(dialog -> {
            editor = null;
            answerInput = null;
        });
        editor.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        editor.show();
    }

    private void goBack() {
        if ("home".equals(page)) finish();
        else showHome();
    }

    @Override protected void onResume() {
        super.onResume();
        foreground = true;
        if (readOnResume && "exam".equals(page)) {
            readOnResume = false;
            readPrompt(store.get(sessionId).currentItem());
        }
    }

    @Override protected void onPause() {
        foreground = false;
        speaker.cancel();
        if ("exam".equals(page) && store.get(sessionId).currentItem().answer().isEmpty()) {
            ((TextView) findViewById(R.id.turn_status)).setText(R.string.ready);
            ((TextView) findViewById(R.id.turn_hint)).setText(R.string.type_hint);
        }
        super.onPause();
    }

    @SuppressWarnings("deprecation")
    @Override public void onBackPressed() {
        goBack();
    }

    @Override protected void onSaveInstanceState(Bundle state) {
        super.onSaveInstanceState(state);
        state.putLong("sessionId", sessionId);
        state.putString("page", page);
        if (editor != null && editor.isShowing()) state.putString("editing", answerInput.getText().toString());
    }

    @Override protected void onDestroy() {
        speaker.close();
        if (editor != null) editor.dismiss();
        store.close();
        super.onDestroy();
    }
}
