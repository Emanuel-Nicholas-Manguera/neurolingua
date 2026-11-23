package app.neurolingua.mobile;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;

import java.util.List;

final class VoiceInput {
    interface Events {
        void listening();
        void processing();
        void transcript(String answer);
        void failed(int message, boolean offerSystemService);
    }

    private final Context context;
    private final Handler main = new Handler(Looper.getMainLooper());
    private SpeechRecognizer active;
    private Runnable watchdog;

    VoiceInput(Context context) {
        this.context = context.getApplicationContext();
    }

    void start(boolean systemService, Events events) {
        cancel();
        if (!systemService && (Build.VERSION.SDK_INT < 31
                || !SpeechRecognizer.isOnDeviceRecognitionAvailable(context))) {
            events.failed(R.string.no_local_speech, SpeechRecognizer.isRecognitionAvailable(context));
            return;
        }
        try {
            SpeechRecognizer recognizer;
            if (!systemService && Build.VERSION.SDK_INT >= 31) {
                recognizer = SpeechRecognizer.createOnDeviceSpeechRecognizer(context);
            } else {
                recognizer = SpeechRecognizer.createSpeechRecognizer(context);
            }
            active = recognizer;
            Turn turn = new Turn(recognizer, events, systemService);
            recognizer.setRecognitionListener(turn);
            Intent request = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            request.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            request.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US");
            request.putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, !systemService);
            request.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3);
            deadline(12000, () -> turn.onError(SpeechRecognizer.ERROR_SERVER));
            recognizer.startListening(request);
        } catch (SecurityException | UnsupportedOperationException e) {
            cancel();
            events.failed(R.string.mic_unavailable, !systemService && SpeechRecognizer.isRecognitionAvailable(context));
        }
    }

    void finish() {
        if (active != null) active.stopListening();
    }

    private void deadline(long millis, Runnable task) {
        if (watchdog != null) main.removeCallbacks(watchdog);
        watchdog = task;
        main.postDelayed(task, millis);
    }

    void cancel() {
        if (watchdog != null) main.removeCallbacks(watchdog);
        watchdog = null;
        SpeechRecognizer recognizer = active;
        active = null;
        if (recognizer != null) {
            recognizer.cancel();
            recognizer.destroy();
        }
    }

    private final class Turn implements RecognitionListener {
        private final SpeechRecognizer owner;
        private final Events events;
        private final boolean system;

        Turn(SpeechRecognizer owner, Events events, boolean system) {
            this.owner = owner;
            this.events = events;
            this.system = system;
        }

        @Override public void onReadyForSpeech(Bundle params) {
            if (active != owner) return;
            Log.i("NeurolinguaSpeech", system ? "System recognizer ready" : "On-device recognizer ready");
            deadline(45000, () -> onError(SpeechRecognizer.ERROR_SPEECH_TIMEOUT));
            events.listening();
        }

        @Override public void onEndOfSpeech() {
            if (active != owner) return;
            deadline(10000, () -> onError(SpeechRecognizer.ERROR_NO_MATCH));
            events.processing();
        }

        @Override public void onResults(Bundle results) {
            if (active != owner) return;
            List<String> choices = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
            if (choices != null) {
                for (String choice : choices) {
                    if (choice != null && !choice.trim().isEmpty()) {
                        cancel();
                        Log.i("NeurolinguaSpeech", "Final transcript received");
                        events.transcript(choice.trim());
                        return;
                    }
                }
            }
            onError(SpeechRecognizer.ERROR_NO_MATCH);
        }

        @Override public void onError(int error) {
            if (active != owner) return;
            cancel();
            Log.i("NeurolinguaSpeech", "Recognizer ended with code " + error);
            boolean silence = error == SpeechRecognizer.ERROR_NO_MATCH || error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT;
            boolean missingLanguage = error == SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE
                    || error == SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED;
            int message = silence ? R.string.no_speech : missingLanguage ? R.string.no_english_model : R.string.mic_unavailable;
            events.failed(message, !silence && !system && SpeechRecognizer.isRecognitionAvailable(context));
        }

        @Override public void onBeginningOfSpeech() { }
        @Override public void onRmsChanged(float rmsdB) { }
        @Override public void onBufferReceived(byte[] buffer) { }
        @Override public void onPartialResults(Bundle results) { }
        @Override public void onEvent(int type, Bundle params) { }
    }
}
