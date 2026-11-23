package app.neurolingua.mobile;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

record Assessment(long id, LocalDate day, String status, List<Item> items) {
    record Item(int position, String prompt, String reference, String answer,
                String source, boolean confirmed, Integer mark) { }

    String displayDate() {
        return day.format(DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.ENGLISH));
    }

    int score() {
        if (!AssessmentStore.SCORED.equals(status)) throw new IllegalStateException("Score pending");
        int points = 0;
        for (Item item : items) points += item.mark();
        return points;
    }

    Item currentItem() {
        for (Item item : items) {
            if (!item.confirmed()) return item;
        }
        return null;
    }
}
