package app.neurolingua.mobile;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.pm.PackageManager;
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
    private VoiceInput microphone;
    private Assessment.Item captureItem;
    private boolean systemService;
    private boolean listenOnResume;
    private boolean requestedMicrophone;
    private static final int MICROPHONE_REQUEST = 1;
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
        microphone = new VoiceInput(this);
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
        microphone.cancel();
        listenOnResume = false;
        captureItem = null;
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
        hint.setText("spoken".equals(item.source()) ? R.string.check_transcript : R.string.check_typed);
        secondary.setVisibility(review ? View.VISIBLE : View.GONE);
        if (review) {
            transcript.setVisibility(View.VISIBLE);
            transcript.setText(item.answer());
            secondary.setText(R.string.try_again);
            secondary.setOnClickListener(v -> capture(item));
            Button alternate = findViewById(R.id.alternate_input);
            alternate.setText(R.string.edit_answer);
            alternate.setOnClickListener(v -> editAnswer(item, item.answer()));
            boolean last = item.position() + 1 == session.items().size();
            primary.setText(last ? R.string.finish_test : R.string.next_question);
            primary.setOnClickListener(v -> {
                store.confirm(session.id(), item.position());
                showExam();
            });
        } else {
            capture(item);
        }
    }

    private void capture(Assessment.Item item) {
        captureItem = item;
        findViewById(R.id.transcript).setVisibility(View.GONE);
        Button repeat = findViewById(R.id.secondary_action);
        repeat.setVisibility(View.VISIBLE);
        repeat.setText(R.string.repeat);
        repeat.setOnClickListener(v -> readPrompt(item));
        Button alternate = findViewById(R.id.alternate_input);
        alternate.setText(R.string.type_answer);
        alternate.setOnClickListener(v -> editAnswer(item, item.answer()));
        readyToSpeak(R.string.speak_hint, false);
        if (foreground) readPrompt(item);
        else readOnResume = true;
    }

    private void voiceState(int title, int message, int action, Runnable click) {
        ((TextView) findViewById(R.id.turn_status)).setText(title);
        ((TextView) findViewById(R.id.turn_hint)).setText(message);
        Button button = findViewById(R.id.primary_action);
        button.setText(action);
        button.setEnabled(click != null);
        button.setAlpha(click == null ? 0.65f : 1f);
        button.setOnClickListener(click == null ? null : v -> click.run());
    }

    private void readyToSpeak(int message, boolean offerSystem) {
        voiceState(R.string.ready, message, R.string.speak_answer, () -> openMicrophone(true));
        Button service = findViewById(R.id.system_speech);
        service.setVisibility(offerSystem ? View.VISIBLE : View.GONE);
        service.setOnClickListener(v -> new AlertDialog.Builder(this)
                .setTitle(R.string.system_speech).setMessage(R.string.service_detail)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.use_service, (dialog, which) -> {
                    systemService = true;
                    openMicrophone(true);
                }).show());
    }

    private void readPrompt(Assessment.Item item) {
        microphone.cancel();
        findViewById(R.id.system_speech).setVisibility(View.GONE);
        voiceState(R.string.reading, R.string.listen_prompt, R.string.speak_answer, null);
        speaker.read(item.prompt(), played -> {
            if (played) openMicrophone(false);
            else readyToSpeak(R.string.playback_unavailable, false);
        });
    }

    private void openMicrophone(boolean explicit) {
        if (!foreground || captureItem == null) return;
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            readyToSpeak(R.string.need_permission, false);
            if (explicit || !requestedMicrophone) {
                requestedMicrophone = true;
                requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, MICROPHONE_REQUEST);
            }
            return;
        }
        findViewById(R.id.system_speech).setVisibility(View.GONE);
        voiceState(R.string.starting_mic, R.string.wait, R.string.done_speaking, null);
        microphone.start(systemService, new VoiceInput.Events() {
            @Override public void listening() {
                voiceState(R.string.listening, R.string.speak_now, R.string.done_speaking, () -> {
                    processing();
                    microphone.finish();
                });
            }

            @Override public void processing() {
                voiceState(R.string.processing, R.string.wait, R.string.done_speaking, null);
            }

            @Override public void transcript(String answer) {
                store.saveDraft(sessionId, captureItem.position(), answer, "spoken");
                showExam();
            }

            @Override public void failed(int message, boolean offerSystem) {
                readyToSpeak(message, offerSystem);
            }
        });
    }

    @Override public void onRequestPermissionsResult(int code, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(code, permissions, results);
        if (code != MICROPHONE_REQUEST || captureItem == null) return;
        if (results.length > 0 && results[0] == PackageManager.PERMISSION_GRANTED) {
            if (foreground) openMicrophone(false);
            else listenOnResume = true;
        } else readyToSpeak(R.string.need_permission, false);
    }

    private void editAnswer(Assessment.Item item, String initial) {
        speaker.cancel();
        microphone.cancel();
        if (captureItem != null) readyToSpeak(R.string.speak_hint, false);
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
        if (listenOnResume && captureItem != null) {
            listenOnResume = false;
            openMicrophone(false);
        } else if (readOnResume && captureItem != null) {
            readOnResume = false;
            readPrompt(captureItem);
        }
    }

    @Override protected void onPause() {
        foreground = false;
        speaker.cancel();
        microphone.cancel();
        if (captureItem != null) readyToSpeak(R.string.speak_hint, false);
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
        microphone.cancel();
        if (editor != null) editor.dismiss();
        store.close();
        super.onDestroy();
    }
}
