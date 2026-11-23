package app.neurolingua.mobile;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;

import java.util.Locale;

final class PromptSpeaker {
    interface Completion {
        void finished(boolean played);
    }

    private final Context context;
    private final Handler main = new Handler(Looper.getMainLooper());
    private TextToSpeech engine;
    private boolean ready;
    private boolean closed;
    private long generation;
    private String question;
    private Completion completion;
    private final Runnable timeout = () -> complete(Long.toString(generation), false);

    PromptSpeaker(Context context) {
        this.context = context.getApplicationContext();
    }

    void read(String text, Completion callback) {
        cancel();
        question = text;
        completion = callback;
        main.postDelayed(timeout, 15000);
        if (engine == null) {
            engine = new TextToSpeech(context, result -> main.post(() -> initialize(result)));
        } else if (ready) {
            speak();
        }
    }

    private void initialize(int result) {
        if (closed) return;
        if (result != TextToSpeech.SUCCESS || engine.setLanguage(Locale.US) < TextToSpeech.LANG_AVAILABLE) {
            engine.shutdown();
            engine = null;
            complete(Long.toString(generation), false);
            return;
        }
        engine.setOnUtteranceProgressListener(new UtteranceProgressListener() {
            @Override public void onStart(String id) { }
            @Override public void onDone(String id) { main.post(() -> complete(id, true)); }
            @Override public void onError(String id) { main.post(() -> complete(id, false)); }
        });
        ready = true;
        if (completion != null) speak();
    }

    private void speak() {
        String id = Long.toString(generation);
        if (engine.speak(question, TextToSpeech.QUEUE_FLUSH, null, id) == TextToSpeech.ERROR) {
            complete(id, false);
        }
    }

    private void complete(String id, boolean played) {
        if (completion == null || !Long.toString(generation).equals(id)) return;
        Completion callback = completion;
        completion = null;
        main.removeCallbacks(timeout);
        if (!played && ready) engine.stop();
        Log.i("NeurolinguaSpeech", played ? "Prompt playback completed" : "Prompt playback unavailable");
        callback.finished(played);
    }

    void cancel() {
        generation++;
        completion = null;
        main.removeCallbacks(timeout);
        if (ready) engine.stop();
    }

    void close() {
        cancel();
        closed = true;
        if (engine != null) engine.shutdown();
    }
}
