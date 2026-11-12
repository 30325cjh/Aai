# Aai

Android floating virtual companion — first milestone.

## Character direction
- Use the supplied blue-gray-haired, cyan-eyed character references as the visual guide.
- Omit the large weapon and all weapon props.
- Prioritize a waist-up / upper-body overlay avatar.
- Preserve the long slate-blue hair with teal highlights, blue-and-white jacket, white shirt, cyan tie, and dark pleated skirt where visible.
- Target a Live2D-style 2.5D rig with blink, gaze, mouth shapes, expressions, head X/Y/Z, breathing, and secondary hair motion.
- The generated expression/model-sheet image is a visual concept only; it is not a layered PSD/CLIP file or a rigged Live2D model.

## Current milestone
- Request Android's "Display over other apps" permission.
- Start a foreground service after permission is granted.
- Show a floating Aai launcher bubble over other apps.
- Tap the bubble to summon/dismiss a temporary character placeholder.
- Double-tap the bubble to reveal an inline Gemini-style chat bar beneath the character (no separate chat Activity).

## Download an APK from GitHub
1. Open the repository's **Actions** tab.
2. Select **Build Aai APK** and open the latest successful run.
3. Download **Aai-debug-apk** from the Artifacts section. GitHub artifacts are retained for 30 days.
4. For a versioned downloadable release, push a version tag (for example `v0.1.0`); the workflow attaches the debug APK to that GitHub Release.

The first workflow run starts automatically after relevant source changes are pushed, or can be started manually from Actions → Build Aai APK → Run workflow.

## Build locally
Open this project in Android Studio (Giraffe or newer), install Android SDK 35, then sync and run the `app` configuration.

## Target experience
- Android floating overlay launcher
- 2.5D upper-body character (Live2D-style layered artwork / rig; current UI uses a temporary character-stage placeholder)
- Blink, mouth movement, expressions, head motion, hair physics
- AI conversation and permitted Android app/intent actions

## Notes
- Personal use does not automatically grant rights to third-party character artwork or remove SDK/runtime license terms. Use artwork you own or have permission to adapt, and review the chosen runtime's terms.
- Android restricts background launches and overlay behavior. The service uses a foreground notification and the user must explicitly grant overlay permission.
- This first milestone is a scaffold, not a finished APK. Device testing and Android-version-specific permission handling remain necessary.
