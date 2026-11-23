# neurolingua
Neurolingua is a voice-guided cognitive pre-screening prototype with spoken assessments and an assistant that answers questions about saved test results.

The first Android milestone uses three manually entered orientation answers. Each session stores the questions, reference date, answer text, input source and review status on the device. Speech tools and the team's AI models can be integrated in a later milestone.

## Run on Android

Open the repository root in Android Studio, sync the Gradle project, select a device, and run the `app` configuration. The app supports Android 8.0 and later. Building requires JDK 17 or 21, Android SDK Platform 36 and Build Tools 35.0.0.

From PowerShell, with the Android SDK configured and a USB-debugging device authorized:

```powershell
.\gradlew.bat :app:installDebug
adb shell am start -n app.neurolingua.mobile/.MainActivity
```
