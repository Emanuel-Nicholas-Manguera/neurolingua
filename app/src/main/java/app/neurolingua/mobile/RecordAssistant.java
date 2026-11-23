package app.neurolingua.mobile;

import java.util.Locale;
import java.util.regex.Pattern;

/** Local response baseline for the Help UI until the team's assistant model is available. */
final class RecordAssistant {
    private static final Pattern MISSED = Pattern.compile("\\b(wrong|missed|miss|incorrect|mistakes?|errors?)\\b");
    private static final Pattern COUNT = Pattern.compile("\\b(how many|number of|count|total number)\\b");
    private static final Pattern SCORE = Pattern.compile("\\b(scores?|results?|points?|marks?|grade)\\b|how did i do");

    static String reply(String question, Assessment scored, boolean newerTestPending) {
        String input = question.toLowerCase(Locale.ENGLISH).trim();
        boolean missed = MISSED.matcher(input).find();
        if (!missed && !SCORE.matcher(input).find()) {
            return "I can look up your last score, the questions you missed, or how many answers were incorrect.";
        }
        if (scored == null) {
            return newerTestPending
                    ? "Your completed test is awaiting a score. I can show your results once its answers have been reviewed."
                    : "You don’t have a scored test yet. Complete a test and have its answers reviewed first.";
        }
        String prefix = newerTestPending ? "Your newest completed test is awaiting a score. " : "";
        String when = scored.displayDate();
        if (!missed) {
            return prefix + "On " + when + ", you scored " + scored.score() + " out of " + scored.items().size() + ".";
        }
        int errors = scored.items().size() - scored.score();
        if (errors == 0) return prefix + "All your answers on " + when + " were marked correct.";
        if (COUNT.matcher(input).find()) {
            return prefix + "You missed " + errors + " out of " + scored.items().size() + " questions on " + when + ".";
        }
        StringBuilder response = new StringBuilder(prefix).append("Marked incorrect on ").append(when).append(":");
        for (Assessment.Item item : scored.items()) {
            if (item.mark() == 0) {
                response.append("\n\n").append(item.prompt()).append("\nYour answer: “").append(item.answer()).append("”");
            }
        }
        return response.toString();
    }

    private RecordAssistant() { }
}
