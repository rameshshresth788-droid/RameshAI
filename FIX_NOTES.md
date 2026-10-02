# RAMESH AI Voice/Background Fixes

- Home screen now has an animated mic/orb UI and startup greeting: "Yes master, aapko kya help chahiye?"
- Auto-listening remains active while the app is visible; no repeated mic taps are needed after microphone permission is granted.
- When Background continuous listening is enabled, the app uses a foreground microphone service after the activity leaves the foreground.
- Background service listens to normal speech directly (no wake-word required) and resumes listening after every response/error.
- TTS prefers a compatible female-labelled Android voice when the device TTS engine exposes one, with a slightly higher pitch fallback. Android does not expose a universal voice-gender API, so the exact voice depends on installed TTS voices.
- OpenAI-compatible providers retry once without tool definitions when a model returns HTTP 400 for unsupported function calling. This helps OpenRouter/free models still work for normal chat.
- Added Female AI voice setting and enabled background continuous listening by default.
- API keys remain runtime-configurable; no key is embedded in source.

Build note: the included GitHub Actions workflow should be used for APK builds. Local build was not run here because this environment cannot download the Gradle distribution.
