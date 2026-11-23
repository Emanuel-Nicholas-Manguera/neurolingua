package app.neurolingua.mobile;

import static org.junit.Assert.*;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.time.LocalDate;

@RunWith(AndroidJUnit4.class)
public class AssessmentStoreTest {
    private AssessmentStore store;
    private final LocalDate day = LocalDate.of(2026, 11, 24);

    @Before public void setUp() {
        store = new AssessmentStore(InstrumentationRegistry.getInstrumentation().getTargetContext(), null);
    }

    @After public void close() {
        store.close();
    }

    @Test public void resumePreservesReferenceDateAndUnconfirmedTranscript() {
        long id = store.startOrResume(day);
        store.saveDraft(id, 0, " Tuesday ", "typed");
        assertEquals(id, store.startOrResume(day.plusDays(1)));
        Assessment session = store.get(id);
        assertEquals(day, session.day());
        assertEquals("Tuesday", session.currentItem().reference());
        assertEquals("Tuesday", session.currentItem().answer());
        assertEquals("typed", session.currentItem().source());
    }

    @Test public void onlyConfirmedAnswersCanCompleteAnUnscoredSession() {
        long id = store.startOrResume(day);
        store.confirm(id, 0);
        assertEquals(0, store.get(id).currentItem().position());
        store.saveDraft(id, 2, "2026", "spoken");
        store.confirm(id, 2);
        assertFalse(store.get(id).items().get(2).confirmed());
        for (int i = 0; i < 3; i++) {
            store.saveDraft(id, i, "response " + i, "typed");
            store.confirm(id, i);
            store.confirm(id, i);
            store.saveDraft(id, i, "late callback", "spoken");
            assertEquals("response " + i, store.get(id).items().get(i).answer());
        }
        assertNull(store.get(id).currentItem());
        assertEquals(id, store.latestId(AssessmentStore.PENDING));
        assertEquals(0, store.latestId(AssessmentStore.SCORED));
        assertNotEquals(id, store.startOrResume(day));
    }

    @Test public void incompleteMarksStayPendingAndFinalScoresAreStable() {
        long id = store.startOrResume(day);
        store.setReviewMark(id, 0, 1);
        assertNull(store.get(id).items().get(0).mark());
        assertFalse(store.finalizeScore(id));
        for (int i = 0; i < 3; i++) {
            store.saveDraft(id, i, "test response", "typed");
            store.confirm(id, i);
        }
        store.setReviewMark(id, 0, 1);
        assertFalse(store.finalizeScore(id));
        assertEquals(AssessmentStore.PENDING, store.get(id).status());
        store.setReviewMark(id, 1, 0);
        store.setReviewMark(id, 2, 1);
        assertTrue(store.finalizeScore(id));
        store.setReviewMark(id, 1, 1);
        assertFalse(store.finalizeScore(id));
        assertEquals(2, store.get(id).score());
    }
}
