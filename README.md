# My Music (Android Studio starter)

A minimal dark-theme Android starter app with:
- Media3 playback service for background playback
- Play a direct HTTPS audio URL
- Download a direct audio file URL to `Music/My Music`
- A quality preference selector UI

## Important limitations
- This starter accepts **direct audio-file URLs** only. It does not extract audio from YouTube or bypass platform restrictions.
- The quality selector is currently a preference UI only. It does not transcode audio. Choosing 320 kbps cannot improve a lower-quality source.
- Download only audio you own or have permission to download.
- This is source code, not a compiled APK.

## Build
1. Install Android Studio.
2. Open this folder as a project.
3. Let Gradle sync and install any requested Android SDK components.
4. Connect an Android phone with USB debugging enabled, or use an emulator.
5. Press Run.

If Gradle reports a dependency/version issue, sync using a recent Android Studio version and an installed JDK 17.
