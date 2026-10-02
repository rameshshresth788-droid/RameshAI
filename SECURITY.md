# Security Notes for RAMESH AI

This document explains the trust boundaries and security tradeoffs made in
this project, so the owner (and anyone reviewing the code) knows exactly what
is and isn't protected.

## 1. Secrets

- API keys, model names, and prompts live in a runtime config file
  (`config/assistant.json` inside the app's private storage), encrypted at
  rest with Jetpack Security (AES-256-GCM, key held in the Android Keystore).
- This file is **never** committed to git — see `.gitignore`.
- The API key is never logged, never printed in full to any terminal or UI
  (see `RuntimeConfig.maskedApiKey()`), and never included in GitHub Actions
  build steps — the CI workflow does not need or accept any AI provider key.
- `tools/config.sh` sends the key over `127.0.0.1` only (loopback), and reads
  it back only in masked form.

## 2. The local config server (`ConfigHttpServer`)

- Binds explicitly to `127.0.0.1`. It is **not** reachable from Wi-Fi, mobile
  data, Bluetooth, or any other device — only processes running on the same
  phone (like Termux) can reach it, because loopback traffic never leaves the
  device's own kernel networking stack.
- It has no authentication of its own. On a phone where you don't trust other
  locally-installed apps, be aware that (in principle) another app running as
  a different Android app sandbox generally *cannot* reach another app's
  127.0.0.1-bound socket that's opened per-process in the same way a normal
  server would be reachable — Android's per-UID network namespacing differs
  by OS version and vendor. If you want to be extra cautious, only enable the
  background service (which keeps the server alive) when you're actively
  reconfiguring, and disable it afterward via Settings → Feature Toggles →
  "Background wake-word listening" (the server currently runs whenever the
  app process is alive, independent of that toggle — see the code comment in
  `RameshAIApplication` if you want to gate it further).
- The server never accepts remote/network requests, only whitelisted JSON
  fields on `POST /config`, and always returns a masked key.

## 3. Accessibility Service

- `RameshAccessibilityService` is never enabled by the app itself — the user
  must explicitly turn it on in Android Settings, where its purpose is
  described in plain language (see `res/xml/accessibility_service_config.xml`
  and `strings.xml`).
- It only acts when `ToolExecutor` calls it in direct response to a user's own
  spoken command, and only after any required confirmation step
  (`ToolDefinitions.ToolSpec.requiresConfirmation`).
- It does not continuously scrape or upload screen content; `onAccessibilityEvent`
  is intentionally a no-op observer.

## 4. Notification Listener

- Also never self-enabled; requires explicit Settings permission.
- Notifications are kept in a small in-memory ring buffer only (never written
  to disk, never sent anywhere) and are only surfaced back to the user when
  they explicitly ask ("last notification kya aaya?").

## 5. Voice "authentication"

- The optional owner voice profile (`VoiceProfileVerifier`) is explicitly
  **not** secure biometric authentication. It's a coarse local similarity
  check meant only to reduce accidental activation by other voices. It has
  not been evaluated for false-accept/false-reject rates and must never gate
  a sensitive action by itself.
- Sensitive/irreversible actions (sending a message, making a call, deleting
  data) always require an explicit confirmation step in-app; for anything
  more sensitive than that, extend `ToolExecutor` to require Android's own
  `BiometricPrompt`/device credential rather than relying on voice similarity.

## 6. Tool-call architecture

- The AI model never executes code directly. It can only emit a structured
  `{ "tool": "...", "arguments": {...} }` request.
- `ToolExecutor` checks the tool name against a closed whitelist
  (`ToolDefinitions.ALL`) and checks required arguments before dispatching to
  any real Android API. Unknown tool names are rejected outright, with no
  fallback "best effort" execution path.
- No root access, no accessibility-based bypass of Android permission dialogs,
  no hidden/private APIs.

## 7. CI / GitHub Actions

- The build workflow (`.github/workflows/build-apk.yml`) does not require or
  read any AI API key, and produces a debug APK by default.
- Signed release builds are opt-in and expect keystore material only via
  GitHub Actions **secrets** (never committed to the repo) — see the README
  section "Optional: signed release builds".

## 8. Reporting a concern

This is a personal, single-owner project template, not a maintained public
service — there's no dedicated security contact. If you fork this and deploy
it more broadly, review this document again and adjust the trust boundaries
(especially §2 and §5) to match your own threat model before doing so.
