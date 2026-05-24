# GymLogger

A simple Android app for logging workouts — lifting, cardio, and stretching.

I built this for myself. Sharing because someone asked.

## What it does

- Log lifting sets (exercise, weight, reps)
- Log cardio sessions (treadmill, running, cycling, other)
- Log stretching
- Workout templates you can reuse
- Built-in workout timer with a notification that keeps ticking when the app is backgrounded
- Calendar view of your training history
- 30-day stats and streaks
- Export your data as a file

## Privacy

No network access. No analytics. No accounts. Everything stays on your device.

## Install

Download `app-release.apk` from the [Releases page](../../releases) and install it on your Android device.

You'll need to allow "Install unknown apps" for whichever app you're downloading from (browser, file manager). Android prompts you for this the first time. After installing GymLogger, you can turn that permission back off if you want.

**Requirements:** Android 7.0 (API 24) or higher.

## Building from source

Standard Android Studio project.

```
git clone https://github.com/pokzen/GymLogger.git
cd GymLogger
```

Open in Android Studio, let Gradle sync, hit Run.

To build your own signed release APK, see Android's [signing your app](https://developer.android.com/studio/publish/app-signing) docs — you'll need to create your own keystore.

## Tech

Jetpack Compose, Room, Kotlin Coroutines, Material 3.

## License

[MIT](LICENSE). Do whatever, just keep the copyright notice.

## No warranty / no support

This is a personal project. It works for me. If it works for you, great. If it breaks, you can file an issue but I can't promise to fix it.
