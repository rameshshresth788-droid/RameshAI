# RAMESH AI

A private, personal, voice-first Android assistant — built to live entirely on
your phone and be managed from **Termux + GitHub Actions**, with no PC and no
APK rebuild required just to change an API key, model, prompt, or wake phrase.

> Speak naturally in Hindi/Hinglish (default) or English. Ask it to open apps,
> set reminders, read your routine, fetch news, answer questions, or automate
> supported phone actions — all through validated, whitelisted tool calls, never
> arbitrary code execution.

---

## 1. Features

- **Voice-first UI** — one animated orb, five states (idle/listening/thinking/
  speaking/error), minimal chrome.
- **Swappable voice engine** — `SpeechToTextEngine` / `TextToSpeechEngine`
  interfaces around Android's built-in SpeechRecognizer/TextToSpeech today;
  drop in another provider later without touching the rest of the app.
- **Swappable AI engine** — `AIProvider` interface with OpenAI-compatible,
  Gemini, and OpenRouter implementations, chosen purely by runtime config.
- **No-rebuild runtime configuration** — API key, base URL, model, system
  prompt, wake phrase, assistant name, language, and feature toggles all live
  in an encrypted on-device config file, editable from the Settings screen or
  from Termux via `tools/config.sh` (see below). Changing these never requires
  a new APK build.
- **Validated tool-call architecture** — the AI can only request one of a
  fixed, whitelisted set of tools (open app, open settings, create reminder,
  get news, etc.). Every call is validated before anything touches Android;
  unknown tools are rejected outright.
- **Reminders** — one-time, daily, weekly, via AlarmManager, survive reboot.
- **Routine** — a fully user-edited daily schedule, no medical claims.
- **News** — pluggable `NewsProvider`, clear "no internet" / "not configured"
  messaging, never fabricated headlines.
- **Optional Accessibility automation** — click/scroll/back/type, only for
  actions the user explicitly asked for, never silently enabled.
- **Optional Notification listener** — in-memory only, never persisted or
  uploaded except in direct response to a user's own request.
- **Foreground service** — for background wake-word listening, only runs if
  the user turns it on, always shows the required ongoing notification.
- **Two-tier memory** — short-term conversation context and long-term
  preferences, both visible/editable/deletable from the Memory screen.
- **Owner voice profile ("Train my voice")** — explicitly convenience-only,
  never treated as secure biometric authentication. Risky actions (sending a
  message, calls) always require an explicit confirmation step.

---

## 2. Requirements

- An Android phone (this is where you'll install and run the app).
- [Termux](https://f-droid.org/packages/com.termux/) (install from F-Droid, not
  the Play Store version, which is unmaintained).
- A GitHub account and a repository to push this project to.
- An API key from an OpenAI-compatible provider, Google Gemini, or OpenRouter
  (only needed to actually talk to an AI model — the app builds and runs
  without one, it'll just tell you the AI isn't configured yet).

No Android Studio and no desktop computer are required for the normal
workflow. Android Studio is only useful if you want to edit code with a full
IDE instead of a text editor in Termux.

---

## 3. First-time setup

```bash
# In Termux:
pkg update -y
pkg install -y git curl jq

git clone <your-repo-url>
cd RameshAI
bash tools/setup.sh
```

Push the project to your own GitHub repository if you haven't already:

```bash
git remote set-url origin https://github.com/<you>/<your-repo>.git
bash tools/git-push.sh
```

This triggers **GitHub Actions**, which builds the debug APK automatically.

---

## 4. Getting the APK

1. Go to your repository on GitHub → **Actions** tab.
2. Open the latest "Build RAMESH AI APK" run.
3. Download the `ramesh-ai-debug-apk` artifact (a zip containing the `.apk`).
4. Transfer/download it to your phone and install it (you'll need to allow
   "install unknown apps" for your browser/file manager once).

---

## 5. Configuring the AI (no rebuild needed)

After installing and opening the app once (so its process/config server is
running), configure it from Termux:

```bash
bash tools/config.sh
```

This gives you a menu to set the AI provider, base URL, API key, model,
assistant name, wake phrase, language, and system prompt. Everything is sent
to the app over `127.0.0.1:8765` — a loopback-only local server, reachable
only by processes on your own phone (see `SECURITY.md`). No ADB, no PC.

You can also configure everything from the app's own **Settings** screen if
you prefer tapping over typing.

To just view the current (masked) configuration:

```bash
bash tools/config.sh show
```

**Important distinction:**

| Change type                                   | Requires APK rebuild? |
|------------------------------------------------|:----------------------:|
| API key, model, provider, prompt, wake phrase   | ❌ No — runtime config |
| Kotlin code, UI, permissions, Gradle config      | ✅ Yes — push to GitHub |

---

## 6. Android permissions this app can request

| Permission | Why | Required? |
|---|---|---|
| Microphone | Voice commands | Yes, for voice features |
| Notifications | Reminder alerts, foreground service notice | Yes on Android 13+ |
| Schedule exact alarms | Reminders fire on time | Recommended |
| Accessibility service | Click/scroll/type automation (e.g. sending a message) | Optional |
| Notification access | "What's my last notification?" | Optional |
| Modify system settings | Brightness control | Optional |
| Overlay | Reserved for future in-call orb overlay | Optional, unused by default |

None of these are silently enabled. Each optional permission has a clear
in-app explanation and an easy way to turn it back off.

---

## 7. Enabling Accessibility / Notification access

Settings screen → **Phone Control** section → tap **Enable** next to the
permission you want. This opens the relevant Android system settings page
directly; find "RAMESH AI" in the list and turn it on.

---

## 8. Project structure

```
app/src/main/java/com/rameshai/
  ai/            AIProvider abstraction (OpenAI/Gemini/OpenRouter/Local)
  voice/         SpeechToTextEngine / TextToSpeechEngine, wake word, voice profile
  tools/         Tool whitelist + validated executor + real Android actions
  accessibility/ RameshAccessibilityService (opt-in automation)
  notifications/ RameshNotificationService (opt-in, in-memory only)
  reminders/     Room DB + AlarmManager scheduler + receivers
  routine/       User-edited daily routine
  news/          NewsProvider abstraction
  memory/        Short-term + long-term memory, Room-backed
  settings/      Onboarding/UI prefs (DataStore)
  config/        Encrypted runtime config + loopback config HTTP server
  apps/          Installed-app discovery/matching
  core/          AssistantOrchestrator (voice->AI->tools->voice) + foreground service
  ui/            Compose screens, orb animation, navigation

tools/           Termux scripts (setup, config, git-push, run-check, logs, backup)
.github/workflows/build-apk.yml   CI build, no secrets required
```

---

## 9. Optional: signed release builds

By default, CI only builds a debug APK (self-signed, fine for personal use).
If you want a signed release build in CI, add these **repository secrets**
(Settings → Secrets and variables → Actions) and set the repository **variable**
`BUILD_RELEASE=true`:

- `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`

Never commit a keystore file or its passwords to the repository.

---

## 10. Troubleshooting

- **"AI API key set nahi hai"** — run `bash tools/config.sh` and set your key
  (option 3), or set it from Settings in the app.
- **`tools/config.sh` can't reach the app** — open the RAMESH AI app at least
  once, or enable background listening in Settings so its service keeps
  running.
- **Speech recognition errors** — check microphone permission is granted, and
  that you have a data/Wi-Fi connection (on-device recognition quality varies
  by phone).
- **Reminders don't fire** — check Settings → Phone Control, and make sure
  battery optimization isn't killing the app; some OEMs (Xiaomi, Oppo, etc.)
  need an extra "autostart"/"no restrictions" toggle in their own battery
  settings.
- **"WhatsApp installed nahi hai"** — the assistant only opens apps it can
  find on your device; check the exact app name.

---

## 11. Design principles (why it's built this way)

- Never claims to have performed an action the Android system hasn't
  confirmed.
- Never bypasses Android permissions, never uses root, never hides a service.
- Never hard-codes secrets into source code or commits them to git.
- Never requires an APK rebuild for configuration changes.
- Every AI-requested action passes through a closed, validated tool whitelist.

See `SECURITY.md` for more detail on the trust boundaries in this project.
