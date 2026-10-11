[README.md](https://github.com/user-attachments/files/33308137/README.md)
<div align="center">

🛠️ Tech Stack

"Android" (https://img.shields.io/badge/Platform-Android-3DDC84?logo=android&logoColor=white)
"Gradle" (https://img.shields.io/badge/Build-Gradle-02303A?logo=gradle&logoColor=white)
"Kotlin DSL" (https://img.shields.io/badge/Build_Scripts-Kotlin_DSL-7F52FF?logo=kotlin&logoColor=white)
"Gemini API" (https://img.shields.io/badge/AI-Gemini_API-4285F4?logo=google&logoColor=white)

Languages & Technologies

- Kotlin DSL (".gradle.kts") — Gradle build configuration.
- Android — Mobile application platform.
- Gemini API — AI integration, where configured in the application.
- Speech Recognition & Text-to-Speech — Interpreter functionality, subject to implementation and device support.

  
# Anha.ai

### Breaking language barriers through AI-powered communication

**Anha.ai** is a multilingual translation project focused on making communication across Indian and international languages easier. It brings AI-assisted text translation together with a conversational Interpreter Mode that is being improved for more dependable two-way communication.

[Repository](https://github.com/Piyush0059/Anha) · [Report a Bug](https://github.com/Piyush0059/Anha/issues) · [Request a Feature](https://github.com/Piyush0059/Anha/issues)

</div>

---

## Overview

Language differences can make everyday conversations, learning, and cross-cultural communication harder than they need to be. Anha.ai is being developed to help people translate and understand language more easily through an accessible application experience.

The project includes an Android/Gradle codebase and configuration for Gemini-powered AI functionality. Its Interpreter Mode is an active area of development; reliability can vary by device, speech-recognition service, supported language, network connection, and API configuration.

> **Project status:** Work in progress. Please treat the current build as a developing project rather than a production-ready interpreting service. Interpreter speech recognition, translation, and spoken playback should be tested on the target device before relying on them in an important conversation.

## Core Capabilities

- **Multilingual translation** — designed for communication across Indian and international languages.
- **AI-assisted language processing** — the repository includes configuration for Gemini API integration.
- **Interpreter Mode** — intended to support two-way conversations by combining speech recognition, translation, and text-to-speech where the device and configured services support them. This mode is currently being improved and should be considered experimental until verified.
- **Spoken output** — audio playback and voice capabilities depend on the implementation, available language voices, device settings, and permissions.
- **Android project structure** — source and Gradle configuration are maintained in this repository.

Feature availability may differ between versions. A listed capability is not a guarantee that every language pair or device is currently supported.

## Repository Structure

```text
Anha/
├── app/                 # Application module
├── gradle/              # Gradle wrapper and supporting configuration
├── .env.example         # Placeholder for environment/API configuration
├── build.gradle.kts     # Root Gradle build configuration
├── settings.gradle.kts  # Gradle project settings
└── metadata.json        # Project metadata
```

The precise implementation and build requirements are defined by the source code and Gradle configuration in the repository.

## Getting Started

### Prerequisites

- Android Studio compatible with the project's Gradle and Android Gradle Plugin versions
- A compatible JDK as required by the Gradle configuration
- Git, if you are cloning the repository
- A Gemini API key if the features you are running require Gemini access
- An Android device or compatible emulator for device testing

### 1. Get the source code

```bash
git clone https://github.com/Piyush0059/Anha.git
cd Anha
```

Alternatively, use **Code → Download ZIP** on GitHub.

### 2. Open the project

Open the repository root in Android Studio and allow Gradle to sync. Resolve any SDK or JDK prompts using versions compatible with the project's Gradle files.

### 3. Configure API credentials safely

The repository contains an `.env.example` placeholder referring to `GEMINI_API_KEY`. It is not a real API key.

- When using Google AI Studio, configure the required secret through the project's supported **Secrets** configuration.
- For a local Android build, check the repository's Gradle Secrets Plugin and application configuration to determine the supported local secret mechanism.
- Never commit a real API key, password, signing key, or private credential to GitHub.
- If a credential has already been committed, revoke or rotate it; deleting it in a later commit does not remove it from Git history.

Do not assume that creating a `.env` file alone makes a key available to a native Android build—the actual project configuration determines how secrets are loaded.

### 4. Build and run

Use Android Studio's **Sync Project with Gradle Files** action, then select the `app` run configuration and run it on a compatible device or emulator. If the build fails, check the error output for required SDK, JDK, Gradle, or secret configuration.

Exact command-line build tasks and environment requirements should be confirmed against the Gradle wrapper and app-level build configuration before use.

## Testing Interpreter Mode

Interpreter behaviour can depend on device services and network access. When testing a build, check both directions of a language pair and verify each step independently:

1. Grant microphone permission when requested.
2. Select the source and target languages.
3. Speak a short, clear sentence and verify the recognized text.
4. Confirm that the translated text is actually returned by the translation service.
5. Play the translated result and confirm that the correct language voice is used, if available.
6. Repeat in the opposite direction.
7. Test permission denial, silence, network loss, unavailable language voices, and repeated taps.

Do not rely on Interpreter Mode for emergencies or high-stakes communication until its behaviour has been thoroughly tested.

## Known Limitations and Development Notes

- Interpreter Mode is under active improvement and may not work reliably on every device or language pair.
- Speech recognition and text-to-speech language support depend on the Android device and installed services.
- AI-assisted features may require a valid Gemini API configuration and network connectivity.
- API usage limits, service availability, latency, and provider behaviour can affect results.
- No downloadable release is currently listed in this repository. Check the repository's **Releases** section for future packaged builds.

## Security and Privacy

- Keep API credentials and signing materials out of source control.
- Avoid placing personal, sensitive, or confidential conversations into development builds unless you understand how the configured services process that data.
- Review the privacy terms of any AI, speech-recognition, or translation provider used by your build.
- Before distributing the app publicly, document its data handling, permissions, and privacy policy accurately.

## Contributing

Bug reports, clear reproduction steps, and practical feature suggestions are welcome.

1. Open an [issue](https://github.com/Piyush0059/Anha/issues) describing the problem or proposed improvement.
2. Include device/Android version, selected language pair, expected behaviour, and actual behaviour when relevant.
3. Never include API keys, private transcripts, or other sensitive information in issues or logs.

For code changes, keep changes focused and test the affected workflow before submitting them.

## License

No license is currently specified in this repository. Until a license is added, do not assume that the code is available for unrestricted reuse, modification, or redistribution. Add an appropriate `LICENSE` file if you intend to make the project open source under specific terms.

## Maintainer

**Piyush Kumar**  
GitHub: [@Piyush0059](https://github.com/Piyush0059)

---

<div align="center">

**Anha.ai — making multilingual communication more accessible.**

</div>
