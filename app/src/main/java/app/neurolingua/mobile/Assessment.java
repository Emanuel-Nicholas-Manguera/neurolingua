package app.neurolingua.mobile;

import java.time.LocalDate;
import java.util.List;

record Assessment(long id, LocalDate day, String status, List<Item> items) {
    record Item(int position, String prompt, String reference, String answer,
                String source, boolean confirmed, Integer mark) { }

    Item currentItem() {
        for (Item item : items) {
            if (!item.confirmed()) return item;
        }
        return null;
    }
}
