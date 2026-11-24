# Neurolingua

Neurolingua is an Android thesis app for developing a guided cognitive pre-screening workflow. This milestone uses three orientation questions to exercise the app and its records. The resulting practice score is out of three.

## Try the app

1. Open **Start Test** on Home. Enter an answer, review the text, and move to the next question. Returning Home offers **Resume test** for an unfinished session.
2. After **Finish test**, return Home and open **Review answers**. Mark each response using the reference for the date the test began. **Save score** becomes available when all three marks are present.
3. Tap the last scored record to see its per-question marks. Open **Help** to ask Maria for the last score, the missed questions, or the number of incorrect answers. A newer pending test is identified separately.

## Future AI and speech integration

The app currently uses manual input and reviewer marks. Sessions and transcripts are stored in the app's private SQLite database. It opens directly to Home and requires no runtime permissions.

- [AssessmentStore](app/src/main/java/app/neurolingua/mobile/AssessmentStore.java) saves the prompt, reference answer, input source, transcript and reviewer mark for each item. A future transcription tool can supply an answer to the same draft-and-confirm flow.
- [RecordAssistant](app/src/main/java/app/neurolingua/mobile/RecordAssistant.java) implements the local question-and-answer baseline used by Maria. The saved item marks supply the facts for the assistant. Training and model inference are separate work.
- The [exam spiral](app/src/main/java/app/neurolingua/mobile/ExamSpiralView.java) turns continuously while the exam is visible. A future voice player can send its output loudness (0–1) to `R.id.exam_spiral` using `setVoiceLevel(float)` on the UI thread. Send zero when playback pauses or ends. If updates stop, the glow and ribbon width settle back to idle. Android's system animation setting can disable motion.

## Run on Android

Open the repository root in Android Studio, sync the Gradle project, select a device, and run the `app` configuration. The app supports Android 8.0 and later. Building requires JDK 17 or 21, Android SDK Platform 36 and Build Tools 35.0.0.

From PowerShell, with the Android SDK configured and a USB-debugging device authorized:

```powershell
.\gradlew.bat :app:installDebug
adb shell am start -n app.neurolingua.mobile/.MainActivity
```

The device tests cover resumable sessions, mark finalization and Help answers. Run them on the connected test phone with the following commands:

```powershell
.\gradlew.bat :app:assembleDebug :app:assembleDebugAndroidTest :app:lintDebug
adb -d install -r app/build/outputs/apk/debug/app-debug.apk
adb -d install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb -d shell am instrument -w app.neurolingua.mobile.test/androidx.test.runner.AndroidJUnitRunner
```
