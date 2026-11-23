package app.neurolingua.mobile;

import android.annotation.SuppressLint;
import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;
import org.vosk.Model;
import org.vosk.Recognizer;
import org.vosk.android.StorageService;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Streams one microphone answer into the bundled recognizer; audio never goes to a file. */
final class OfflineVoiceInput {
    private static final int SAMPLE_RATE = 16000;
    private static final int CHUNK_SAMPLES = SAMPLE_RATE / 10;
    private final Context context;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private volatile Turn current;
    private Model model; // Accessed only by the serial worker.

    OfflineVoiceInput(Context context) {
        this.context = context.getApplicationContext();
    }

    private static final class Turn {
        final VoiceInput.Events events;
        volatile boolean finish;
        Turn(VoiceInput.Events events) {
            this.events = events;
        }
    }

    void start(VoiceInput.Events events) {
        Turn turn = new Turn(events);
        current = turn;
        worker.execute(() -> record(turn));
    }

    // Activity recreation can briefly overlap two workers on the first asset unpack.
    private static synchronized String prepareAssets(Context context) throws IOException {
        return StorageService.sync(context, "vosk-model-small-en-us-0.15", "english-speech");
    }

    @SuppressLint("MissingPermission") // The Activity has checked RECORD_AUDIO immediately before this turn.
    private void record(Turn turn) {
        AudioRecord mic = null;
        try {
            if (current != turn) return;
            if (model == null) model = new Model(prepareAssets(context));
            if (current != turn) return;
            int minimum = AudioRecord.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT);
            if (minimum <= 0) throw new IOException("Unsupported recording format");
            mic = new AudioRecord(MediaRecorder.AudioSource.VOICE_RECOGNITION, SAMPLE_RATE,
                    AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT,
                    Math.max(minimum, CHUNK_SAMPLES * 4));
            try (Recognizer decoder = new Recognizer(model, SAMPLE_RATE)) {
                if (current != turn) return;
                mic.startRecording();
                if (mic.getRecordingState() != AudioRecord.RECORDSTATE_RECORDING) {
                    throw new IOException("Microphone did not start");
                }
                publish(turn, () -> {
                    Log.i("NeurolinguaSpeech", "Bundled English recognizer listening");
                    turn.events.listening();
                });
                String text = decode(turn, mic, decoder);
                if (current == turn) {
                    if (text.isEmpty()) publish(turn, () -> turn.events.failed(R.string.no_speech, false));
                    else publish(turn, () -> {
                        Log.i("NeurolinguaSpeech", "Final transcript received from bundled recognizer");
                        turn.events.transcript(text);
                    });
                }
            }
        } catch (IOException | JSONException | RuntimeException | LinkageError e) {
            Log.w("NeurolinguaSpeech", "Offline recognition unavailable: " + e.getClass().getSimpleName());
            publish(turn, () -> turn.events.failed(R.string.mic_unavailable, true));
        } finally {
            if (mic != null) {
                if (mic.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) mic.stop();
                mic.release();
            }
        }
    }

    private String decode(Turn turn, AudioRecord mic, Recognizer decoder) throws IOException, JSONException {
        short[] samples = new short[CHUNK_SAMPLES];
        long deadline = SystemClock.elapsedRealtime() + 45000;
        while (current == turn && !turn.finish && SystemClock.elapsedRealtime() < deadline) {
            int count = mic.read(samples, 0, samples.length, AudioRecord.READ_NON_BLOCKING);
            if (count < 0) throw new IOException("Microphone read failed");
            if (count == 0) {
                SystemClock.sleep(15);
                continue;
            }
            if (decoder.acceptWaveForm(samples, count)) {
                String phrase = textOf(decoder.getResult());
                if (!phrase.isEmpty()) return phrase;
            }
        }
        return current == turn ? textOf(decoder.getFinalResult()) : "";
    }

    private static String textOf(String result) throws JSONException {
        return new JSONObject(result).optString("text").trim();
    }

    private void publish(Turn turn, Runnable event) {
        main.post(() -> {
            if (current == turn) event.run();
        });
    }

    void finish() {
        Turn turn = current;
        if (turn != null) turn.finish = true;
    }

    void cancel() {
        current = null;
    }

    void close() {
        cancel();
        worker.execute(() -> { if (model != null) model.close(); });
        worker.shutdown();
    }
}
