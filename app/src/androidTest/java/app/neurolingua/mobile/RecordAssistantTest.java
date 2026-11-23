package app.neurolingua.mobile;

import static org.junit.Assert.*;

import org.junit.Test;
import java.time.LocalDate;
import java.util.Arrays;

public class RecordAssistantTest {
    private final Assessment record = new Assessment(9, LocalDate.of(2026, 11, 24), AssessmentStore.SCORED,
            Arrays.asList(new Assessment.Item(0, "What day is it?", "Tuesday", "Tuesday", "typed", true, 1),
                    new Assessment.Item(1, "What month is it?", "November", "December", "typed", true, 0),
                    new Assessment.Item(2, "What year is it?", "2026", "2026", "typed", true, 1)));

    @Test public void answersComeFromTheFinalizedItemMarks() {
        assertTrue(RecordAssistant.reply("What score did I get from the last test?", record, false).contains("2 out of 3"));
        assertTrue(RecordAssistant.reply("How many wrong answers did I do?", record, false).contains("missed 1 out of 3"));
        String missed = RecordAssistant.reply("What question did I get wrong?", record, false);
        assertTrue(missed.contains("What month is it?"));
        assertTrue(missed.contains("December"));
        assertFalse(missed.contains("What year is it?"));
    }

    @Test public void pendingAndEmptyStatesDoNotProduceAnInventedScore() {
        assertTrue(RecordAssistant.reply("Score?", record, true).startsWith("Your newest completed test is awaiting a score."));
        assertTrue(RecordAssistant.reply("Score?", null, true).contains("awaiting a score"));
        assertTrue(RecordAssistant.reply("Score?", null, false).contains("don’t have a scored test"));
        assertTrue(RecordAssistant.reply("How is the weather?", record, false).startsWith("I can look up"));
    }
}
