package app.neurolingua.mobile;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Insets;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.window.OnBackInvokedDispatcher;

import java.time.LocalDate;

public final class MainActivity extends Activity {
    private FrameLayout root;
    private AssessmentStore store;
    private long sessionId;
    private String page = "home";
    private AlertDialog editor;
    private EditText answerInput;
    private DetailSheet detail;
    private MariaSheet maria;
    private long detailId;
    CustomThemeUi customUi;

    @Override protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        AppearanceMode.selected(base).applyTo(this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ColorTheme selected = ColorTheme.selected(this);
        setTheme(selected.style);
        if (selected == ColorTheme.CUSTOM) {
            boolean dark = (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK)
                    == Configuration.UI_MODE_NIGHT_YES;
            customUi = new CustomThemeUi(this, new CustomPalette(ColorTheme.customColor(this), dark));
        }
        super.onCreate(savedInstanceState);
        if (customUi != null) customUi.window(getWindow());
        root = new FrameLayout(this);
        root.setBackgroundColor(ThemeColors.get(this, R.attr.paletteSurface));
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
        if (Build.VERSION.SDK_INT >= 33) {
            getOnBackInvokedDispatcher().registerOnBackInvokedCallback(
                    OnBackInvokedDispatcher.PRIORITY_DEFAULT, this::goBack);
        }
        if (savedInstanceState != null) {
            sessionId = savedInstanceState.getLong("sessionId");
            page = savedInstanceState.getString("page", "home");
        }
        if ("settings".equals(page)) {
            showSettings();
        } else if (!"home".equals(page) && store.get(sessionId) != null) {
            showExam();
            if (savedInstanceState != null && savedInstanceState.containsKey("editing")
                    && "exam".equals(page)) {
                editAnswer(store.get(sessionId).currentItem(), savedInstanceState.getString("editing"));
            }
        } else {
            showHome();
            if (savedInstanceState != null) {
                long recordId = savedInstanceState.getLong("detailId");
                if (recordId != 0) openRecord(recordId);
                else if (savedInstanceState.getBoolean("helpOpen")) {
                    openHelp();
                    String last = savedInstanceState.getString("lastQuestion", "");
                    if (!last.isEmpty()) maria.ask(last);
                    maria.input.setText(savedInstanceState.getString("helpDraft", ""));
                }
            }
        }
    }

    private void screen(String name, int layout) {
        page = name;
        root.removeAllViews();
        getLayoutInflater().inflate(layout, root, true);
        if (customUi != null) customUi.apply(root.getChildAt(0));
    }

    private void showHome() {
        screen("home", R.layout.screen_home);
        findViewById(R.id.assessment_card).setClipToOutline(true);
        findViewById(R.id.help).setOnClickListener(v -> openHelp());
        findViewById(R.id.settings).setOnClickListener(v -> showSettings());
        boolean resume = store.latestId(AssessmentStore.ACTIVE) != 0;
        Button start = findViewById(R.id.start_test);
        start.setText(resume ? R.string.resume : R.string.start);
        start.setOnClickListener(v -> {
            sessionId = store.startOrResume(LocalDate.now());
            showExam();
        });
        long pending = store.latestId(AssessmentStore.PENDING);
        if (pending != 0) {
            findViewById(R.id.pending).setVisibility(View.VISIBLE);
            Button review = findViewById(R.id.review);
            review.setVisibility(View.VISIBLE);
            review.setOnClickListener(v -> openRecord(pending));
        }
        long scored = store.latestId(AssessmentStore.SCORED);
        if (scored != 0) {
            Assessment last = store.get(scored);
            findViewById(R.id.last_result).setVisibility(View.GONE);
            View record = findViewById(R.id.open_record);
            record.setVisibility(View.VISIBLE);
            ((TextView) findViewById(R.id.last_date)).setText(last.displayDate());
            ((TextView) findViewById(R.id.last_score)).setText(getString(R.string.score_fraction, last.score(), last.items().size()));
            record.setContentDescription(getString(R.string.score_summary, last.displayDate(), last.score(), last.items().size()));
            record.setOnClickListener(v -> openRecord(scored));
        }
    }

    private void showSettings() {
        screen("settings", R.layout.screen_settings);
        findViewById(R.id.home).setOnClickListener(v -> showHome());
        RadioGroup modes = findViewById(R.id.appearance_options);
        modes.check(AppearanceMode.selected(this).optionId);
        for (AppearanceMode mode : AppearanceMode.values()) {
            modes.findViewById(mode.optionId).setOnClickListener(v -> {
                if (AppearanceMode.selected(this) != mode) {
                    mode.save(this);
                    recreate();
                }
            });
        }

        RadioGroup choices = findViewById(R.id.theme_options);
        choices.check(ColorTheme.selected(this).optionId);
        int size = Math.round(30 * getResources().getDisplayMetrics().density);
        for (ColorTheme theme : ColorTheme.presets()) {
            RadioButton option = choices.findViewById(theme.optionId);
            Context palette = new ContextThemeWrapper(this, theme.style);
            GradientDrawable swatch = new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{
                    ThemeColors.get(palette, R.attr.spiralFirst), ThemeColors.get(palette, R.attr.spiralSecondLight),
                    ThemeColors.get(palette, R.attr.spiralThird)});
            swatch.setShape(GradientDrawable.OVAL);
            swatch.setSize(size, size);
            option.setCompoundDrawablesRelativeWithIntrinsicBounds(
                    swatch, null, option.getCompoundDrawablesRelative()[2], null);
            option.setOnClickListener(v -> {
                if (ColorTheme.selected(this) != theme) {
                    theme.save(this);
                    recreate();
                }
            });
        }
    }

    private void openRecord(long id) {
        Assessment session = store.get(id);
        if (session == null || AssessmentStore.ACTIVE.equals(session.status())) return;
        detailId = id;
        DetailSheet record = RecordSheet.create(this, store, session, this::showHome);
        if (AssessmentStore.SCORED.equals(session.status())) {
            Button ask = record.button(getString(R.string.ask_maria), true);
            ask.setOnClickListener(v -> {
                record.dialog.dismiss();
                openHelp();
            });
            record.add(ask, 16);
        }
        showDetail(record);
    }

    private void openHelp() {
        detailId = 0;
        maria = new MariaSheet(this, store);
        showDetail(maria.sheet);
    }

    private void showDetail(DetailSheet sheet) {
        detail = sheet;
        detail.dialog.setOnDismissListener(dialog -> {
            if (detail == sheet) {
                detail = null;
                detailId = 0;
                maria = null;
            }
        });
        detail.show();
    }

    private void showExam() {
        Assessment session = store.get(sessionId);
        Assessment.Item item = session.currentItem();
        if (item == null) {
            screen("done", R.layout.screen_complete);
            findViewById(R.id.home).setOnClickListener(v -> showHome());
            return;
        }
        if (root.findViewById(R.id.exam_spiral) == null) screen("exam", R.layout.screen_exam);
        findViewById(R.id.home).setOnClickListener(v -> showHome());
        showProgress(item.position() + 1, session.items().size());
        TextView prompt = findViewById(R.id.prompt);
        prompt.setText(item.prompt());
        renderAnswer(session, item);
    }

    private void showProgress(int currentQuestion, int questionCount) {
        TextView number = findViewById(R.id.question_number);
        number.setText(getString(R.string.progress_text, currentQuestion, questionCount));
        LinearLayout track = findViewById(R.id.progress);
        track.removeAllViews();
        int gap = Math.round(5 * getResources().getDisplayMetrics().density);
        for (int i = 0; i < questionCount; i++) {
            GradientDrawable fill = new GradientDrawable();
            fill.setColor(ThemeColors.get(this, i < currentQuestion ? R.attr.paletteAccent : R.attr.paletteOutline));
            fill.setCornerRadius(track.getLayoutParams().height / 2f);
            View segment = new View(this);
            segment.setBackground(fill);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.MATCH_PARENT, 1f);
            if (i > 0) params.setMarginStart(gap);
            track.addView(segment, params);
        }
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
        transcript.setVisibility(review ? View.VISIBLE : View.GONE);
        transcript.setText(item.answer());
        if (review) {
            secondary.setText(R.string.edit_answer);
            secondary.setOnClickListener(v -> editAnswer(item, item.answer()));
            boolean last = item.position() + 1 == session.items().size();
            primary.setText(last ? R.string.finish_test : R.string.next_question);
            primary.setOnClickListener(v -> {
                store.confirm(session.id(), item.position());
                showExam();
            });
        } else {
            primary.setText(R.string.type_answer);
            primary.setOnClickListener(v -> editAnswer(item, ""));
        }
    }

    private void editAnswer(Assessment.Item item, String initial) {
        FrameLayout fields = new FrameLayout(this);
        int margin = Math.round(24 * getResources().getDisplayMetrics().density);
        fields.setPadding(margin, margin / 2, margin, 0);
        answerInput = new EditText(this);
        ThemeColors.styleInput(answerInput);
        answerInput.setHint(R.string.answer_hint);
        answerInput.setSingleLine(true);
        answerInput.setText(initial);
        answerInput.setSelection(answerInput.length());
        fields.addView(answerInput);
        editor = new AlertDialog.Builder(this).setTitle(R.string.answer_hint).setView(fields)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.review_answer, null).create();
        editor.setOnShowListener(dialog -> {
            if (customUi != null) customUi.dialog(editor, fields);
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

    // Android 8–12 dispatch Back here. The API 33+ callback is registered in onCreate.
    @android.annotation.SuppressLint("GestureBackNavigation")
    @SuppressWarnings("deprecation")
    @Override public void onBackPressed() {
        goBack();
    }

    @Override protected void onSaveInstanceState(Bundle state) {
        super.onSaveInstanceState(state);
        state.putLong("sessionId", sessionId);
        state.putString("page", page);
        state.putLong("detailId", detailId);
        if (maria != null) {
            state.putBoolean("helpOpen", true);
            state.putString("helpDraft", maria.input.getText().toString());
            state.putString("lastQuestion", maria.lastQuestion);
        }
        if (editor != null && editor.isShowing()) state.putString("editing", answerInput.getText().toString());
    }

    @Override protected void onDestroy() {
        if (detail != null) detail.dialog.dismiss();
        if (editor != null) editor.dismiss();
        store.close();
        super.onDestroy();
    }
}
