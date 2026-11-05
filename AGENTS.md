# AGENTS.md — AI & Contributor Engineering Guide

> **Target Audience:** Autonomous AI Coding Agents (Claude, Gemini, Antigravity, Codex) and Human Engineers.  
> **Purpose:** Prevent regressions, maintain architectural integrity, and enforce strict hardware/system invariants when maintaining or extending `ohmyAES`.

---

## 1. Project Overview & Architectural Model

`ohmyAES` (HyperPortal) ports the fluid, multimodal **ColorOS 17 Content Portal (DragShare / 内容传送门)** experience to **Xiaomi HyperOS 4 (Android 17)**.

It uses a dual-engine architecture:
```
+-------------------------------------------------------------------------------+
| Target Application Process (WeChat, Coolapk, Weibo, Telegram, etc.)            |
|                                                                               |
|  [View Hierarchy / Compose Tree]                                              |
|            |                                                                  |
|            v (Touch Event / Long-Press)                                       |
|  [com.miui.contentcatcher (AES Interceptor.smali)]                            |
|            | (Static Invocation)                                              |
|            v                                                                  |
|  [dev.dragshare.catcher.Bridge (classes2.dex)]                                |
|    - Deep View & Compose Semantics extraction                                 |
|    - WMS DragShadowBuilder & Dynamic Card Morphing                            |
|    - Threshold tracking (0.76f enter / 0.70f exit)                            |
|    - Native fly-back restoration (return false on ACTION_DROP)                |
+-------------------------------------------------------------------------------+
       | Intent (Payload) & Broadcast (Drag State 1/2/0)
       v
+-------------------------------------------------------------------------------+
| DragShareBridge Process (com.dragshare.portal)                                |
|                                                                               |
|  [PortalService]                                                              |
|    - ColorOS Aurora GlowView (AGSL RuntimeShader with modulo 60s time)        |
|    - Bottom Shelf Target Bar (Horizontally scrollable AppTargets)             |
|    - FileProvider caching & intent dispatch                                  |
+-------------------------------------------------------------------------------+
       |
+-------------------------------------------------------------------------------+
| Android system_server (com.android.server.wm)                                 |
|                                                                               |
|  [com.android.server.wm.DragState]                                            |
|            ^                                                                  |
|            | (LSPosed Hook via PortalSystemModule)                            |
|  SurfaceControl.Transaction.setMatrix (3D Perspective Tilt Matrix)            |
+-------------------------------------------------------------------------------+
```

---

## 2. Hard Invariants & Non-Negotiable Rules

Any AI agent modifying this codebase **MUST STRICTLY OBEY** the following rules:

### ⚠️ Rule 1: NEVER Automate Touch Events on the Physical Device
- **DO NOT** run `adb shell input tap`, `input swipe`, `input draganddrop`, or `monkey`.
- **Reason:** The user actively tests real finger gestures and haptics on physical hardware. Synthetic touch injection interferes with physical touch input, causes race conditions, and violates explicit user instructions (`你想测试什么就告诉我，我来帮你测，你只需要日志监听即可，不要自己操作`).
- **Required Workflow:** Build and install the APKs, instruct the human tester what to try, and passively monitor logs using `python tools/deploy.py --logs`.

### ⚠️ Rule 2: NEVER Use Full-Screen Fallback Screenshots
- **DO NOT** add OCR or full-screen screen capture fallback logic (`不要全量兜底截屏`).
- **Reason:** The extraction must be 100% semantic and multi-modal (true View tree, text, bitmaps, URLs, PFD streams, Compose semantics). Fullscreen screenshotting produces massive memory spikes, blurry graphics, privacy issues, and breaks ColorOS drag-and-drop parity.

### ⚠️ Rule 3: Native Cancel Fly-Back Animation Mechanics
- **Mechanism:** In Android's `WindowManagerService` (`DragState.java`), `createCancelAnimationLocked` only fires if `mDragResult == false`.
- **Invariant:**
  - `Bridge.java`: In `root.setOnDragListener`, `DragEvent.ACTION_DROP` **MUST ALWAYS `return false;`**.
  - `PortalService.java`: In `container.setOnDragListener`, `DragEvent.ACTION_DROP` **MUST `return false;`** unless the item was dropped squarely onto a valid, clickable `AppTarget`.
- **Failure Mode:** If either listener returns `true`, WMS marks the drag as "successfully consumed", skipping the cancel animation. The shadow abruptly vanishes instead of smoothly flying back to the source text/image position.

### ⚠️ Rule 4: Thresholds & Hysteresis Synchronization
To prevent flickering or oscillation when the user drags near the bottom of the screen:
- **Enter Hot Zone (Morph into card, expand shelf):** `screenHeight * 0.76f`
- **Exit Hot Zone (Expand back to original shadow, collapse shelf):** `screenHeight * 0.70f`
- Both `Bridge.java` and `PortalService.java` must respect this hysteresis gap (6% screen height).

### ⚠️ Rule 5: AGSL Shader Precision & Time Wrapping
- In `GlowView`:
  ```java
  // CORRECT:
  float time = (SystemClock.uptimeMillis() % 60000) / 1000.0f;

  // INCORRECT (Causes NaN / precision collapse after running for minutes):
  float time = (System.currentTimeMillis() - mStartTime) / 1000.0f;
  ```
- Mobile GPUs (Adreno / Mali) execute AGSL fragment shaders in 16-bit or 32-bit floating point. Unbounded time causes trigonometric functions (`sin`, `cos`) in noise rotation routines to glitch or output NaN, making the aurora glow disappear.

### ⚠️ Rule 6: Jetpack Compose & Hybrid View Penetration (Coolapk, etc.)
- Coolapk embeds traditional Android views inside Jetpack Compose:
  `ComposeView` $\rightarrow$ `AndroidComposeView` $\rightarrow$ `AndroidViewsHandler` $\rightarrow$ `ViewFactoryHolder` $\rightarrow$ `FixAppCompatTextView` / `ImageView`.
- Compose Semantics inspection only reveals empty containers for these nodes.
- **Invariant:** When encountering a `ViewGroup` (even inside `ComposeView`), recursively call `findDeepChildInGroup()` and bypass transparent touch-intercepting overlays (like `CanvasProxyView` / `Slidr` / `SwipeBack`).

### ⚠️ Rule 7: NEVER Reboot the Device or Soft Reboot Automatically
- **DO NOT** run `adb shell "su -c killall system_server"`, `python tools/deploy.py --soft-reboot`, `adb reboot`, or restart device processes on your own (`不要重启`).
- **Reason:** Rebooting disconnects ADB wireless debugging, drops dynamic wireless debugging ports, interrupts the user's active device usage, and clears testing state. 
- **Required Workflow:** After building and installing APKs (`python tools/deploy.py --install all --refresh-lsp`), only notify the user and monitor logs passively (`python tools/deploy.py --logs`). Never initiate any soft or hard reboot unless the user explicitly tells you to do so in that exact turn.

### ⚠️ Rule 8: Commit Code Changes Promptly to Git
- **Requirement:** Whenever functional modifications, features, or bug fixes are verified (e.g. compiling successfully, tests passing, or logic confirmed), commit the changes promptly via Git (`git add` and a concise, meaningful conventional commit message).
- **Reason:** Prevents loss of working context during multi-turn agent iterations, keeps uncommitted working-tree diffs focused and reviewable, and makes it trivial to isolate and roll back unexpected regressions.

### ⚠️ Rule 9: Release Signing & R8 Boundary
- **Local signing material:** `.sign/my-release-key.jks` (keystore) + `.sign/qianming.txt` (`ALIAS`, `KEY_PASSWORD`, `KEYSTORE_PASSWORD`). `.sign/` is **git-ignored**; never commit it, copy it into `dist/`, or print its passwords in logs/answers.
- **No debug fallback:** both the bridge release and the AES `ContentCatcher-patched.apk` must be signed with this keystore. `tools/config.py` and `bridge/build.gradle.kts` read it automatically; if it is missing the build must fail loudly rather than silently use a debug key. The old `signing/poc.jks` (alias `poc` / `pocpass`) is deleted.
- **Key swap on device:** `python tools/deploy.py --install all` replaced the poc-signed packages in place on this HyperOS device (`adb install -r -d`, `signatures` moved to the new key, app data preserved). Do **not** `adb uninstall` by default; only `tools/deploy.py` prints the uninstall remediation when a signature mismatch is actually rejected. After swapping the bridge APK, always run `--refresh-lsp` (the `/data/app/~~<hash>/` path changed).
- **Normal test builds run WITHOUT R8:** `python tools/build.py bridge` / `python tools/build.py all` produce unminified releases (only D8 dexing + signing). This keeps mapping-free logs and fast iteration.
- **R8 is a pre-release gate only:** `python tools/build.py bridge --r8` enables `isMinifyEnabled` via `-Phyperportal.minify=true` and writes `dist/HyperPortal-r8.apk` (mapping in `bridge/build/outputs/mapping/release/mapping.txt`). **AES is never R8-minified** — it is a single-Java-file D8 build. If R8 breaks anything, extend `bridge/proguard-rules.pro` (entry points, manifest components, JNI `TextSegmenter`, Xposed `HyperPortalEntry`) instead of disabling R8.
- **Release verification:** before shipping, confirm signatures with `apksigner verify --print-certs` and re-test the app + LSPosed scope.

---

## 3. Toolchain & Development Workflow

**DO NOT create versioned one-off scripts like `build_v45.py` or `patch_v45.py`!**  
All build and deployment tasks are unified under `tools/`:

| Task | Command |
| :--- | :--- |
| **Build Everything (no R8, default)** | `python tools/build.py all` |
| **Build Bridge & LSPosed (no R8)** | `python tools/build.py bridge` |
| **R8 Pre-release Build (bridge only)** | `python tools/build.py bridge --r8` → `dist/HyperPortal-r8.apk` |
| **Build AES Hook (always unminified)** | `python tools/build.py aes` |
| **Clean Build Cache** | `python tools/build.py clean` |
| **Install Both APKs** | `python tools/deploy.py --install all --refresh-lsp` |
| **Perform Soft Reboot** | `python tools/deploy.py --soft-reboot` |
| **Live Logcat Monitor** | `python tools/deploy.py --logs` |
| **One-Click Deploy All** | `python tools/deploy.py --all` |

### Source Locations:
- Edit AES Hook: `aes/src/dev/dragshare/catcher/Bridge.java`
- Edit Companion & Glow: `bridge/src/com/dragshare/portal/PortalService.java`
- Edit 3D Tilt Hook: `bridge/src/com/dragshare/portal/system/PortalSystemModule.java`

---

## 4. LSPosed & `system_server` Architecture Note

`PortalSystemModule` hooks `system_server` (scope: `system`).

### Why `system_server` Behaves Differently:
1. When `adb install -r` is executed on Android 12+, PackageManager assigns a new randomized directory hash (`/data/app/~~<random_hash>/.../base.apk`).
2. `system_server` is forked only **once** during device boot. It does not dynamically reload modified APKs.
3. Therefore:
   - **During development (after pushing new APK via ADB):** Run `python tools/deploy.py --refresh-lsp`. Wait for user instructions before any reboot (DO NOT auto-reboot, per Rule 7).
   - **In everyday usage (hard reboots / cold boots):** LSPosed retains the established valid path. `system_server` automatically loads the hook on boot. **No soft reboot is needed during normal daily use.**

---

## 5. Critical Code Locations

- **`MainActivity.kt`**: App shell only — a two-page `HorizontalPager` (首页 / 设置) with a
  blurred `NavigationBar` in the outer `Scaffold`. The bar's height is passed down to each page
  as `bottomPadding`; each page owns its own `Scaffold` + `TopAppBar` via `PortalPageScaffold`.
  Back goes to page 0 before leaving the app.
- **`ui/HomePage.kt`**: Status only, no switches. The big card has four states — paused
  (master switch off), ready (all three hooks), waiting for an AES handshake, and not activated
  — laid out like KernelSU / HyperMusicCover / InstallerX / MiuiBackGestureHook: title+summary
  top-left, working mode bottom-left, oversized icon bleeding off the bottom-right. Tapping it
  (or the top-bar refresh) re-probes.
- **`data/PortalStatus.kt`** + **`data/LsposedStatus.kt`** + **`data/PortalStatusStore.kt`**:
  where the home page's answers come from. `LsposedStatus` binds the libxposed service and reads
  `runningTargets` (the only authority on which processes the module is injected into; a target
  counts as system_server by name `system`/`android` or uid 1000). `PortalStatusStore` records
  the AES handshake — `Bridge.ensureConfig()` calls `getConfig` on `PortalFileProvider`, whose
  `call()` records the caller's uid. **SystemUI and system_server also call `getConfig`**, so
  they are filtered out of the AES record; they have their own signals (runningTargets, and
  SystemUI's `reportSystemUiActive`).
- **`ui/SettingsPage.kt`**: The screen the app used to open on, unchanged in content.
- **`Bridge.java`**:
  - `findDeepTouchView(View root, float rawX, float rawY)`: Target view hit testing.
  - `findDeepChildInGroup(ViewGroup group, ...)`: Jetpack Compose / AndroidViewsHandler hybrid tree penetration.
  - `updateSystemShadow(boolean inHotZone)`: ColorOS card morphing animation.
  - `notifyPortalDragState(Context ctx, int state, float y)`: Continuous motion IPC dispatch.
  - `root.setOnDragListener(...)`: Window-level drag event handling and cancel fly-back trigger.
- **`PortalService.java`**:
  - `GlowView`: Custom AGSL RuntimeShader view rendering ColorOS Aurora glow.
  - `setPhase1Progress(float progress)`: Glow intensity ramp (Phase 1).
  - `setPhase2()`: Bottom shelf expansion (Phase 2).
  - `container.setOnDragListener(...)`: Drop interception & target delivery.
- **`PortalSystemModule.java`**:
  - `hookDragState(XC_LoadPackage.LoadPackageParam lpparam)`: Attaches to `com.android.server.wm.DragState`.
  - `SurfaceControl.Transaction.setMatrix(...)`: Applies 3D rotation matrix to `mSurfaceControl`.
- **`TextSegmentationActivity.kt`**:
  - Compose BigBang overlay shell; launched from the SystemUI dock's “文本分词” target.
  - Word-chip selection/copy/search/share live in `BoomChipPage` / `BoomActionHandler`.
- **`TextSegmenter.kt`**:
  - `cppjieba` JNI wrapper; copies `assets/dict/*.utf8` into `filesDir/dragshare_jieba` on first use.
  - JNI class path must stay `com/github/leaf/hyperportal/bigbang/TextSegmenter` in `text_segmenter_jni.cpp`.
- **`PortalSystemUiModule.java`**:
  - `buildTargets(...)`: Text branch adds the built-in “文本分词” target before “搜索”.
  - `ACTION_DROP`: stores the drop coordinates so the BigBang chips animate from the drop point.
  - Image targets never build `file://` URIs; they stage a bitmap through `PortalFileProvider` and ask `PortalService` to dispatch the share as the HyperPortal UID.
- **`PortalShareContract.java` / `PortalService.dispatchImageShare(...)`**:
  - Cross-process contract for image shares from the SystemUI module.
  - `PortalService` owns `PortalFileProvider`, so it is the only place that grants the target URI and starts the target's `ACTION_SEND` activity.
- **`PortalFileProvider.java`**:
  - `grantToCommonTargets(...)` grants staged image URIs to common share targets and `com.android.systemui`; keep the provider writable only through the incoming namespace.
