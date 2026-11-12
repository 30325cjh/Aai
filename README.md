# Aai

Android floating virtual companion — first milestone.

## Current milestone
- Request Android's "Display over other apps" permission.
- Start a foreground service after permission is granted.
- Show a floating Aai launcher bubble over other apps.
- Tap the bubble to summon/dismiss a temporary character placeholder.
- Double-tap the bubble to open a placeholder chat screen.

The character artwork and rig are intentionally not included yet. Add the user's authorized character reference before preparing layered 2.5D assets.

## Build
Open this project in Android Studio (Giraffe or newer), install Android SDK 35, then sync and run the `app` configuration.

## Target experience
- Android floating overlay launcher
- 2.5D upper-body character (Live2D-style layered artwork / rig)
- Blink, mouth movement, expressions, head motion, hair physics
- AI conversation and permitted Android app/intent actions

## Notes
- Personal use does not automatically grant rights to third-party character artwork or remove SDK/runtime license terms. Use artwork you own or have permission to adapt, and review the chosen runtime's terms.
- Android restricts background launches and overlay behavior. The service uses a foreground notification and the user must explicitly grant overlay permission.
- This first milestone is a scaffold, not a finished APK. Device testing and Android-version-specific permission handling remain necessary.
