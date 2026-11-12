# Aai

Android floating character companion.

## Current implementation

- The temporary vector placeholder has been replaced with a transparent cut-out derived from the supplied ARIS character model-sheet image.
- The original character colors and proportions are retained as far as the single flattened reference permits.
- The renderer applies subtle breathing, idle sway, blink, and expression/talking overlays.
- The large weapon from the separate full-body reference is not used; the character cut-out is taken from the model-sheet's central figure.
- The source references are flattened images, not separated PSD/CLIP/Live2D layers. Therefore hair physics, independently moving limbs, eye gaze, and full Live2D deformation are not available in this milestone. Those require original separated transparent parts or manual reconstruction.

## Overlay and controls

- User-granted "Display over other apps" permission is required.
- The foreground service shows a small launcher bubble. Tap it to summon or dismiss ARIS.
- The character uses its own compact transparent overlay window; the chat bar is a separate compact window below the character.
- Areas outside those window rectangles are configured not to be touch-modal and should pass touches to the app underneath.
- Long-press the launcher bubble or use the foreground notification's **오버레이 종료** action to close the overlay.
- Double-tap the character to show/hide the inline command bar.
- Drag with one finger to move; pinch with two fingers to resize. Placement and scale persist across launches.
- Commands: `/웃다`, `/슬프다`, `/놀라다`, `/울다`, `/기본`.
- App launch examples: `크롬 열어줘`, `유튜브 열어줘`, `설정 열어줘`.
- Web search example: `검색해 고양이`.
- No AI backend and no voice output are included.

## Important touch limitation

Android delivers touch to an overlay window using its rectangular window bounds; transparent pixels inside that rectangle are not guaranteed to pass through. Aai keeps the character window tightly sized around the artwork and uses `FLAG_NOT_TOUCH_MODAL` so touches outside the character window pass through. A true pixel-perfect irregular touch region is not guaranteed by this implementation. If the overlay ever behaves unexpectedly, use the notification action or long-press the launcher bubble to stop it.

## Download an APK

1. Open the repository's **Actions** tab.
2. Select **Build Aai APK** and open the latest successful run.
3. Download **Aai-debug-apk** from the Artifacts section. Artifacts are retained for 30 days.
4. Push a version tag to publish a GitHub Release if desired.

## Build locally

Open in Android Studio, install Android SDK 35 and JDK 17, then run `gradle assembleDebug`.

## Artwork and licensing

The included transparent cut-out is a best-effort extraction from the user-provided flattened model-sheet image. It is not a true Live2D model or separated layer source. Use only artwork you own or are authorized to adapt, and review applicable character-art and runtime terms.
