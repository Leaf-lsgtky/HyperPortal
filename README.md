# ohmyAES (DragShare)

> Bringing ColorOS Fluid Cloud / DragShare content portal interactive experience and fluid animations to Xiaomi HyperOS!
> Features system-level multimodal content extraction, ColorOS Aurora Glow AGSL shader, bottom rearrangement & shadow morphing, and LSPosed `system_server` 3D tilt transformation.

[English](README.md) | [简体中文](README_CN.md)

---

## 🌟 Highlights

- 🎯 **System-wide Multimodal Content Extraction (AES Bridge)**
  - Injected directly into Xiaomi HyperOS privileged system plugin `com.miui.contentcatcher`.
  - Deep penetration into **Classic View Hierarchy**, **Jetpack Compose Semantics**, and **AndroidViewsHandler Hybrid Layouts** (seamlessly supports Coolapk feed posts, WeChat, Weibo, Xiaohongshu, Telegram, etc.).
  - Extracts rich text, web content, high-resolution original images, and raw file streams (`ParcelFileDescriptor`).

- 🌈 **ColorOS Aurora Glow (AGSL RuntimeShader)**
  - Custom 1:1 hardware-accelerated fluid aurora shader built on **AGSL (Android Graphics Shading Language)**.
  - Multi-frequency noise displacement with smooth continuous touch progress transitions.

- 📱 **Adaptive Bottom Shelf & Rearrangement Animation**
  - **Dynamic Drag Shadow Morphing**: Drag shadow smoothly scales and transforms into a rounded-corner card when entering the bottom threshold (locked at 76% screen height).
  - **Native Cancel Fly-back Restoration**: Drops outside valid targets seamlessly revert to Android's native WindowManager cancel fly-back animation.

- 📐 **LSPosed `system_server` 3D Tilt Transformation**
  - Direct hook into `com.android.server.wm.DragState` in Android's `system_server`.
  - Manipulates the drag surface via `SurfaceControl.Transaction.setMatrix` at the SurfaceFlinger layer to generate authentic 3D spatial perspective tilt.

---

## 🏗️ Repository Layout

```
ohmyAES/
├── aes/                               # Application Extension Service (AES) Injection Component
│   ├── src/                           # Java source injected into AES classes2.dex
│   │   └── dev/dragshare/catcher/
│   │       └── Bridge.java            # Content extraction, touch hooks, gesture bridge
│   ├── smali_project/                 # Host Smali project for building classes.dex
│   ├── base/                          # Official base APK (Application Extension Service_5.1.7.apk)
│   └── patches/                       # Smali injector script
│
├── bridge/                            # Companion App & LSPosed Module
│   ├── AndroidManifest.xml
│   ├── src/
│   │   └── com/dragshare/portal/
│   │       ├── MainActivity.java
│   │       ├── PortalService.java     # Aurora GlowView, bottom shelf, drop interception
│   │       ├── PortalReceiver.java    # IPC broadcast synchronization
│   │       ├── PortalFileProvider.java# FileProvider for secure cross-app sharing
│   │       └── system/
│   │           └── PortalSystemModule.java # LSPosed module (system_server 3D tilt)
│   ├── assets/
│   │   └── oppo_glow.png              # Aurora noise texture
│   ├── resources/
│   │   └── META-INF/xposed/           # Modern LSPosed module descriptors
│   └── libs/                          # Compile-time Xposed API & Stubs
│
├── tools/                             # Unified Build & Deployment Toolchain
│   ├── config.py                      # Android SDK, Build-Tools, Apktool auto-detection
│   ├── build.py                       # High-speed dual-target builder
│   ├── deploy.py                      # One-click install, LSPosed sync, soft reboot, logcat
│   └── repack.py                      # Binary AXML versionCode modifier & DEX bundler
│
├── docs/                              # Reverse engineering and architecture documentation
│   ├── ARCHITECTURE.md
│   ├── COLOROS_VIEWDRAG_SPEC.md
│   └── COLOROS_COMPOSE_EXTRACTION_SPEC.md
│
├── signing/                           # Signing keystore
└── dist/                              # Output directory for release APKs
```

---

## 🚀 Building from Source

No complicated Gradle configuration or manual patching needed:

### 1. Prerequisites
- Python 3.8+
- JDK 11+
- Android SDK (with `build-tools` and `platforms`)

### 2. Build Commands

```bash
# Build both APKs (DragShareBridge.apk + ContentCatcher-patched.apk)
python tools/build.py all

# Build DragShareBridge only
python tools/build.py bridge

# Build AES patched APK only
python tools/build.py aes

# Clean build cache
python tools/build.py clean
```

Outputs are placed in `dist/`:
- `dist/DragShareBridge.apk`
- `dist/ContentCatcher-patched.apk`

---

## 📲 Deployment & Debugging

```bash
# Install both APKs and sync LSPosed scope
python tools/deploy.py --install all --refresh-lsp

# Soft reboot (killall system_server, takes ~5s)
python tools/deploy.py --soft-reboot

# Attach live logcat monitor
python tools/deploy.py --logs
```

---

## 📄 License

Licensed under the [Apache License, Version 2.0](LICENSE).
