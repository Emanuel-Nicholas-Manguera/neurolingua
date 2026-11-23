package app.neurolingua.mobile;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

final class AssessmentStore extends SQLiteOpenHelper {
    static final String ACTIVE = "active";
    static final String PENDING = "pending";
    static final String SCORED = "scored";

    AssessmentStore(Context context) {
        this(context, "assessments.db");
    }

    AssessmentStore(Context context, String name) {
        super(context, name, null, 1);
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        db.setForeignKeyConstraintsEnabled(true);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE sessions (id INTEGER PRIMARY KEY, day TEXT NOT NULL, "
                + "status TEXT NOT NULL CHECK(status IN ('active','pending','scored')))");
        db.execSQL("CREATE UNIQUE INDEX one_active_test ON sessions(status) WHERE status='active'");
        db.execSQL("CREATE TABLE items (session_id INTEGER NOT NULL REFERENCES sessions(id), "
                + "position INTEGER NOT NULL, prompt TEXT NOT NULL, reference TEXT NOT NULL, "
                + "answer TEXT NOT NULL DEFAULT '', source TEXT NOT NULL DEFAULT '', "
                + "confirmed INTEGER NOT NULL DEFAULT 0, mark INTEGER CHECK(mark IN (0,1)), "
                + "PRIMARY KEY(session_id, position))");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        throw new IllegalStateException("Missing migration from " + oldVersion + " to " + newVersion);
    }

    long startOrResume(LocalDate day) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            long id = latestId(ACTIVE);
            if (id == 0) {
                ContentValues session = new ContentValues();
                session.put("day", day.toString());
                session.put("status", ACTIVE);
                id = db.insertOrThrow("sessions", null, session);
                addItem(db, id, 0, "What day of the week is it?",
                        day.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.ENGLISH));
                addItem(db, id, 1, "What month is it?",
                        day.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH));
                addItem(db, id, 2, "What year is it?", Integer.toString(day.getYear()));
            }
            db.setTransactionSuccessful();
            return id;
        } finally {
            db.endTransaction();
        }
    }

    private void addItem(SQLiteDatabase db, long id, int position, String prompt, String reference) {
        ContentValues row = new ContentValues();
        row.put("session_id", id);
        row.put("position", position);
        row.put("prompt", prompt);
        row.put("reference", reference);
        db.insertOrThrow("items", null, row);
    }

    long latestId(String status) {
        try (Cursor rows = getReadableDatabase().rawQuery(
                "SELECT id FROM sessions WHERE status=? ORDER BY id DESC LIMIT 1", new String[]{status})) {
            return rows.moveToFirst() ? rows.getLong(0) : 0;
        }
    }

    Assessment get(long id) {
        String[] key = {Long.toString(id)};
        SQLiteDatabase db = getReadableDatabase();
        LocalDate day;
        String status;
        try (Cursor rows = db.rawQuery("SELECT day,status FROM sessions WHERE id=?", key)) {
            if (!rows.moveToFirst()) return null;
            day = LocalDate.parse(rows.getString(0));
            status = rows.getString(1);
        }
        List<Assessment.Item> items = new ArrayList<>();
        try (Cursor rows = db.rawQuery("SELECT position,prompt,reference,answer,source,confirmed,mark "
                + "FROM items WHERE session_id=? ORDER BY position", key)) {
            while (rows.moveToNext()) {
                items.add(new Assessment.Item(rows.getInt(0), rows.getString(1), rows.getString(2),
                        rows.getString(3), rows.getString(4), rows.getInt(5) != 0,
                        rows.isNull(6) ? null : rows.getInt(6)));
            }
        }
        return new Assessment(id, day, status, Collections.unmodifiableList(items));
    }

    void saveDraft(long id, int position, String answer, String source) {
        ContentValues values = new ContentValues();
        values.put("answer", answer.trim());
        values.put("source", source);
        getWritableDatabase().update("items", values, "session_id=? AND position=? AND confirmed=0",
                new String[]{Long.toString(id), Integer.toString(position)});
    }

    void setReviewMark(long id, int position, int mark) {
        if (mark != 0 && mark != 1) throw new IllegalArgumentException("Expected a binary mark");
        ContentValues values = new ContentValues();
        values.put("mark", mark);
        getWritableDatabase().update("items", values, "session_id=? AND position=? AND confirmed=1 "
                        + "AND EXISTS(SELECT 1 FROM sessions WHERE id=items.session_id AND status='pending')",
                new String[]{Long.toString(id), Integer.toString(position)});
    }

    boolean finalizeScore(long id) {
        ContentValues values = new ContentValues();
        values.put("status", SCORED);
        return getWritableDatabase().update("sessions", values, "id=? AND status='pending' "
                        + "AND NOT EXISTS(SELECT 1 FROM items WHERE session_id=sessions.id AND mark IS NULL)",
                new String[]{Long.toString(id)}) == 1;
    }

    void confirm(long id, int position) {
        SQLiteDatabase db = getWritableDatabase();
        String sessionId = Long.toString(id);
        db.beginTransaction();
        try {
            db.execSQL("UPDATE items SET confirmed=1 WHERE session_id=? AND position=? "
                    + "AND confirmed=0 AND answer<>'' AND position=(SELECT MIN(position) FROM items "
                    + "WHERE session_id=? AND confirmed=0)", new Object[]{id, position, id});
            db.execSQL("UPDATE sessions SET status='pending' WHERE id=? AND status='active' "
                    + "AND NOT EXISTS(SELECT 1 FROM items WHERE session_id=? AND confirmed=0)",
                    new Object[]{sessionId, sessionId});
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }
}
