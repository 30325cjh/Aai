# Character asset pipeline

## Input needed
Provide a character image that you own or are authorized to adapt. A clean front-facing upper-body image is the best starting point. Multiple views and expression references improve reconstruction.

## 2.5D target
Prepare separated transparent layers for:
- rear hair
- face / neck / ears
- eye whites, irises, pupils, eyelids and brows
- mouth shapes
- front hair strands
- torso and clothing
- optional accessories

## Motion parameters
Plan a rig around eye blink, eye gaze, mouth open/close and vowel shapes, brow movement, head X/Y/Z, body breathing, and hair secondary motion. Emotion presets should map to combinations of brows, eyes, mouth and head pose.

## Android runtime
Keep the character renderer behind a small interface so the app can change runtimes without changing overlay behavior. Candidate formats/runtimes need a compatibility and license review before adoption. Personal use does not automatically waive third-party runtime terms.
