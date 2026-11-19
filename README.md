<!-- IKOKO HERNESS · Project README -->

<div align="center">

  <img src="Duck-ai-image-2026-11-19-08-09.jpeg" alt="IKOKO HERNESS app artwork" width="220" />

  <h1>IKOKO HERNESS</h1>
  <p><strong>Your AI-powered mobile workspace.</strong></p>
  <p>AI Agent · Skills · Projects · 3D Studio · Video Tools</p>

  <p>
    <a href="https://github.com/gikoko611/Ikoko-hernes/actions"><img src="https://img.shields.io/github/actions/workflow/status/gikoko611/Ikoko-hernes/android.yml?branch=main&style=for-the-badge&label=ANDROID%20BUILD" alt="Android build status"></a>
    <img src="https://img.shields.io/badge/Platform-Android-3DDC84?style=for-the-badge&logo=android&logoColor=white" alt="Android">
    <img src="https://img.shields.io/badge/ABI-ARMv7-orange?style=for-the-badge" alt="ARMv7">
    <img src="https://img.shields.io/badge/License-TBD-lightgrey?style=for-the-badge" alt="License to be determined">
  </p>

  <p>
    <a href="#-overview">Overview</a> ·
    <a href="#-features">Features</a> ·
    <a href="#-compatibility">Compatibility</a> ·
    <a href="#-build-the-apk">Build APK</a> ·
    <a href="#-project-status">Project status</a>
  </p>

</div>

---

## ✨ Overview

**IKOKO HERNESS** is an Android-first productivity and creative workspace that brings AI-assisted coding, reusable skills, project files, 3D experiments and basic video utilities together in one app.

The project is designed with a dark **Midnight Gold** interface and targets accessible 32-bit Android devices, including the Redmi 9A.

## 🚀 Features

| Module | What it does |
|---|---|
| 🤖 AI Agent | Chat with an OpenAI-compatible API provider |
| 🔌 Provider Manager | Configure API base URL, API key and model; discover available models where supported |
| 🧩 Skills Manager | Import, browse and remove skill files |
| 📁 Project Workspace | Create, open, edit and save local project files |
| ⌨️ Command Console | Provides a small set of built-in, restricted utility commands |
| 🧊 3D Studio | Lightweight Three.js preview workspace |
| 🎬 Video Tools | Select MP4 files and perform basic stream-copy trimming/remuxing |
| 🔐 Key Protection | Stores provider credentials using Android Keystore-backed encryption |

> **Please note:** Some advanced capabilities are planned rather than fully implemented. The current 3D area is a lightweight preview, not a full Blender renderer. Video tools are basic MP4 stream-copy operations, not a full multi-track editor or transcoder. AI chat currently uses request/response calls rather than autonomous agent execution.

## 📱 Compatibility

| Item | Target |
|---|---|
| Android | Android 6.0+ (API 23+) |
| Recommended test device | Redmi 9A |
| Android version target | Android 10 |
| CPU architecture | **ARMv7 32-bit (armeabi-v7a)** |
| Package ID | com.ikoko.herness |
| App version | 1.0.0 |

The build configuration filters native ABI packaging to ARMv7. As this app is currently Java-based, it does not bundle a separate native NDK library.

## 🛠️ Build the APK

APK builds run through **GitHub Actions** — no local Android Studio installation is required.

1. Open the [Actions page](https://github.com/gikoko611/Ikoko-hernes/actions).
2. Select **IKOKO HERNESS Full ARMv7 Build**.
3. Choose **Run workflow** if you want to start a build manually.
4. Open the completed workflow run.
5. Download the artifact named **IKOKO-HERNESS-v1.0.0-ARMv7**.

The workflow builds both Debug and Release APK variants and uploads them as artifacts. The Release build currently uses the debug signing configuration for testing; it is **not a production release signature**.

## 🔑 Configure an AI provider

1. Open the Provider Manager in the app.
2. Enter an OpenAI-compatible API base URL.
3. Add your API key.
4. Load or enter a model supported by that provider.
5. Save the provider and test the connection.

API availability, model access, pricing and free-tier limits are controlled by the provider. Keep your API key private and do not commit it to this repository.

## 🧭 Project status

- [x] Android app project and GitHub Actions build workflow
- [x] Uploaded artwork connected as the app launcher icon
- [x] ARMv7 build configuration
- [x] AI provider configuration and chat foundation
- [x] Skills and local project workspace foundations
- [x] Lightweight 3D preview and basic MP4 trimming
- [ ] Full autonomous agent/tool execution
- [ ] Full Blender rendering pipeline
- [ ] Advanced multi-track video and audio editing

## 📂 Repository

- **Source:** [gikoko611/Ikoko-hernes](https://github.com/gikoko611/Ikoko-hernes)
- **Builds:** [GitHub Actions](https://github.com/gikoko611/Ikoko-hernes/actions)
- **Issues:** [Report a bug or request a feature](https://github.com/gikoko611/Ikoko-hernes/issues)

## 👑 Credits

<div align="center">

  **IKOKO HERNESS**  
  Built with ambition by **G.I KO KO**

  <sub>AI-assisted creation · Mobile-first development · ARMv7 support</sub>

</div>
