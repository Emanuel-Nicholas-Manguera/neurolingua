package app.neurolingua.mobile;

import android.app.Activity;
import android.graphics.Typeface;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

final class RecordSheet {
    static DetailSheet create(Activity activity, AssessmentStore store, Assessment session, Runnable onScored) {
        boolean reviewing = AssessmentStore.PENDING.equals(session.status());
        String title = activity.getString(reviewing ? R.string.review_test : R.string.test_record);
        DetailSheet sheet = new DetailSheet(activity, title, session.displayDate());
        if (reviewing) {
            sheet.add(sheet.text(activity.getString(R.string.mark_instructions), 16, R.color.ink), 12);
        } else {
            sheet.add(sheet.text(activity.getString(R.string.points, session.score(), session.items().size()),
                    25, R.color.plum), 10);
        }
        Button save = sheet.button(activity.getString(R.string.save_score), true);
        for (Assessment.Item item : session.items()) {
            sheet.divider();
            TextView prompt = sheet.text(item.prompt(), 17, R.color.ink);
            prompt.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            sheet.add(prompt, 10);
            sheet.add(sheet.text(activity.getString(R.string.recorded_answer, item.answer()), 16, R.color.ink), 5);
            if (reviewing) {
                sheet.add(sheet.text(activity.getString(R.string.reference_answer, item.reference()), 14, R.color.muted), 4);
                RadioGroup group = new RadioGroup(activity);
                group.setOrientation(LinearLayout.HORIZONTAL);
                RadioButton correct = option(sheet, group, R.string.correct);
                RadioButton incorrect = option(sheet, group, R.string.incorrect);
                if (item.mark() != null) group.check(item.mark() == 1 ? correct.getId() : incorrect.getId());
                group.setOnCheckedChangeListener((buttons, checked) -> {
                    store.setReviewMark(session.id(), item.position(), checked == correct.getId() ? 1 : 0);
                    enableWhenComplete(save, store.get(session.id()));
                });
                sheet.add(group, 2);
            } else {
                int outcome = item.mark() == 1 ? R.string.awarded : R.string.missed;
                sheet.add(sheet.text(activity.getString(outcome), 15, R.color.plum), 3);
            }
        }
        if (reviewing) {
            enableWhenComplete(save, session);
            save.setOnClickListener(v -> {
                if (store.finalizeScore(session.id())) {
                    sheet.dialog.dismiss();
                    onScored.run();
                }
            });
            sheet.add(save, 16);
        }
        return sheet;
    }

    private static RadioButton option(DetailSheet sheet, RadioGroup group, int label) {
        RadioButton choice = new RadioButton(sheet.activity);
        choice.setId(View.generateViewId());
        choice.setText(label);
        choice.setMinHeight(sheet.dp(48));
        group.addView(choice, new RadioGroup.LayoutParams(0, -2, 1));
        return choice;
    }

    private static void enableWhenComplete(Button save, Assessment session) {
        boolean complete = session.items().stream().allMatch(item -> item.mark() != null);
        save.setEnabled(complete);
        save.setAlpha(complete ? 1f : .55f);
    }
}
