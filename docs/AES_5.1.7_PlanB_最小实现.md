# AES 5.1.7 + Plan B 最小实现记录

> 日期：2026-10-30  
> 范围：只验证“改 AES APK 的 classes.dex / classes2.dex，代码能否进入目标应用进程”这一件事。  
> 不修改 `长按快捷操作_内容获取机制分析.md`；本文是独立复核记录。  
> 整屏 ViewExtract 能力已确认存在，但本期不做。

---

## 1. 结论摘要

1. `miui-framework.jar` 的 `android.app.ActivityImpl.onCreate()` → `miui.contentcatcher.InterceptorProxy.create()` →
   `InterceptorFactory.initInterceptorClass()` → `createPackageContext("com.miui.contentcatcher", 3)` →
   `Class.forName("com.miui.contentcatcher.Interceptor", true, classLoader)` 已确认。
   **AES APK 的 DEX 会被 PathClassLoader 加载进每个目标应用进程**。
2. 本机 AES 是 **5.1.7 / versionCode 520260911**，位于
   `/product/app/ContentCatcherOS4/ContentCatcherOS4.apk`，`sharedUserId=android.uid.system`，签名证书
   `CN=MIUI, O= Xiaomi`。
3. 5.1.7 的混淆名与旧文档中的 4.1.9 不同：
   - 命中遍历：`Interceptor.d(View, PointF, PointF, ArrayList, HashMap)`
   - 长按入口：`Interceptor.l(PointF, PointF, View)`
   - 内容收口：`ViewContentFetcher.fetchViewContent(View, int, boolean)`
   - PageConfig：`Interceptor.n()` 内的 `ContentCatcherManager.getPageConfig(Token)`
   - 上报：`d/c.b(...)` / `Interceptor.l(...)` 内的 `ContentCatcherManager.onContentCatched(Content)`
4. **Plan B（绕过 AES 服务，直接投递）** 的最小代码已生成：
   - `classes2.dex` 只有 `dev.dragshare.catcher.Bridge`
   - `classes.dex` 有 5 处静态转发
   - 成品：`poc/build/apk/ContentCatcher-poc.apk`
   - 静态检查已确认 classes2.dex 存在、Bridge 被 classes.dex 引用、versionCode 已改为 999999999。
5. 实机安装/加载验证尚未完成：ADB 设备在打包完成后断开，需要重新连接后执行第 8 节命令。

---

## 2. 设备与版本事实

| 项 | 值 |
|---|---|
| Android | 17 / HyperOS |
| AES 包 | `com.miui.contentcatcher` |
| AES 版本 | `5.1.7` / versionCode `520260911` |
| APK 路径 | `/product/app/ContentCatcherOS4/ContentCatcherOS4.apk` |
| UID | `1000`，`sharedUserId=android.uid.system` |
| 签名 | `CN=MIUI, OU=MIUI, O=Xiaomi, L=Beijing, ST=Beijing, C=CN` |
| 传送门 | `com.miui.contentextension` 5.1.5，`/product/priv-app/MIUIContentExtension` |
| 原设置 | `content_catcher_network_enabled_content_extension=0`、`open_content_extension_clipboard_mode=0`、`support_long_press_mode=1` |
| 原状态 | AES 进程未运行 |

### 2.1 AES 进程生命周期（关键）

`miui-services.jar` 中的 `com.android.server.contentcatcher.WakeUpContentCatcherManager`：

- `isExistContentExtension()` 只检查 `com.miui.contentextension` 是否安装；
- `functionEnable()` 检查 `taplus_clipboard_mode` 或 `content_catcher_network_enabled_content_extension` 是否为 1；
- 只有两者都满足时才 `startContentCatcherService()`。

实机验证：把 `content_catcher_network_enabled_content_extension` 设为 1 后，
`com.miui.contentcatcher` 进程和 `miui.contentcatcher.ContentCatcherService` Binder 立即出现；
恢复为 0 后进程可能因 system_server 绑定继续存活，重启后恢复原状。

另外 `InterceptorProxy.dispatchTouchEvent()` 还有一道门：

```text
mContentExtensionEnabled == false -> return
```

它来自 `ContentCatcherManager.getContentExtensionEnabled()`
→ `SecurityManager.getContentExtensionEnabled()`，实际读取
`Settings.System.open_content_extension_clipboard_mode`。所以即使 AES 进程存在，
这个设置不为 1，目标应用里的触摸也不会进入 AES。

**结论：如果卸载 `com.miui.contentextension`，原版 AES 不会被 system_server 启动。**
Plan B 的 PoC 不依赖 AES 服务，但仍需要 `mContentExtensionEnabled=1`，否则
`InterceptorProxy.dispatchTouchEvent()` 不会调用 `Interceptor`。

#### 2.1.1 为什么“自制长按”仍然受传送门开关影响

`Bridge.ping()` 并不是独立注册的 `ViewRootImpl` 触摸监听器；它的插桩点是
`com.miui.contentcatcher.Interceptor.dispatchTouchEvent()`。调用链是：

```text
ActivityImpl.onCreate()
  -> InterceptorProxy.create()
  -> InterceptorProxy.dispatchTouchEvent()
       if (!mContentExtensionEnabled) return false;   // 传送门开关在这里拦截
  -> Interceptor.dispatchTouchEvent()
       -> Bridge.ping()                               // 自制 500ms fallback
```

因此 `Bridge` 里的 500ms 定时器只是 AES 已收到触摸后的兜底，无法绕过框架层的
`mContentExtensionEnabled` 门。传送门 5.1.5 的 `TaplusSettingUtils.setTaplusEnableStatus()`
会调用 `ContentCatcherUtil.setLongPressEnableStatus()`，把
`content_catcher_network_enabled_content_extension` 写成 0，并通过
`refreshKeepCatcherProcessAliveEnable()` 重算
`open_content_extension_clipboard_mode`（手机上当 `taplus_clipboard_mode` 也为 0 时即为 0）。
这就是“传送门关掉后模块不生效”的真正原因；它与长按触发器是否自制无关。

`PortalSystemModule` 现在在 system_server 中 Hook
`com.miui.server.SecurityManagerService.getContentExtensionEnabled()`，对普通应用 UID
返回 `true`（系统 UID 仍走原逻辑），因此传送门开关关闭时 AES 触摸门仍然打开，
不需要改写用户设置。该 Hook 位于 system_server，安装新 bridge APK 后必须重启一次
才会加载；普通应用进程则只需重新打开。

---

## 3. Framework 加载链（已确认）

反编译 `miui-framework.jar`：

```text
android.app.ActivityImpl.onCreate(Activity, IBinder)
  -> if (mInterceptor == null) mInterceptor = InterceptorProxy.create(activity)
  -> if (mInterceptor != null) mInterceptor.notifyActivityCreate()

miui.contentcatcher.InterceptorProxy.create(Activity)
  -> addMiuiApplication()
  -> 黑名单检查（Launcher / FallbackHome / CryptKeeper / Gallery HomePage / Camera）
  -> new InterceptorProxy(activity)
  -> InterceptorHandler 延迟创建 Interceptor

miui.contentcatcher.InterceptorFactory.initInterceptorClass()
  -> getInterceptorPackageInfo()
  -> AppGlobals.getInitialApplication().createPackageContext("com.miui.contentcatcher", 3)
  -> Class.forName("com.miui.contentcatcher.Interceptor", true, context.getClassLoader())
```

`createPackageContext(..., 3)` 的 flag 是
`CONTEXT_INCLUDE_CODE (1) | CONTEXT_IGNORE_SECURITY (2)`，即把 AES APK 的 DEX
作为代码加载进当前目标应用进程，而不是 IPC 到 AES 进程。

AES 5.1.7 APK 只有 `classes.dex`（286,136 字节），没有 `classes2.dex`。
本 PoC 新增 `classes2.dex`，PathClassLoader 会按 MultiDex 规则加载。

---

## 4. AES 5.1.7 关键方法

| 作用 | 方法 |
|---|---|
| 触摸入口 | `Interceptor.dispatchTouchEvent(MotionEvent, View, Activity)` |
| 命中遍历 | `Interceptor.d(View, PointF, PointF, ArrayList, HashMap)` |
| 长按入口 | `Interceptor.l(PointF, PointF, View)` |
| PageConfig 获取 | `Interceptor.n()` 内 `ContentCatcherManager.getPageConfig(Token)` |
| 内容提取 | `ViewContentFetcher.fetchViewContent(View, int, boolean)` |
| 原生文本回退 | `ViewContentFetcher.fetchNativeViewContent(View, boolean)` |
| 上报 | `d/c.b(...)` 内 `ContentCatcherManager.onContentCatched(Content)` |
| 长按检测 | `u/a.c(MotionEvent, View)`；`support_long_press_mode` 默认 1 |
| 默认长按时长 | `ViewConfiguration.getLongPressTimeout() + 50`，可被 `support_long_press_time_out` 覆盖 |

`Interceptor.d` 对 `ViewGroup` 直接递归；`AndroidComposeView` 和 `WebView` 本身都是
`ViewGroup`，所以原版不会把这两个宿主作为叶子传给 `fetchViewContent`。这是后续
Compose/WebView 增强必须插桩的位置。

`Interceptor.n()` 的 gate：

```text
PageConfig == null
  -> return（不创建 u/a 长按监听）

PageConfig.features 为空 && PageConfig.catchers 为空
  -> return
```

所以 Plan B 在 `Interceptor.n()` 的 `getPageConfig` 调用点注入本地合成的
`PageConfig`，不依赖 AES 服务。

---

## 5. OPPO 全屏 ViewExtract（本期不做，仅记录）

OPPO 确实有一套独立的整窗结构化提取框架：

```text
oplus-framework.jar:
  android.view.viewextract.ViewExtractSDK
  android.view.viewextract.ViewExtractProxy
  android.view.viewextract.OplusViewExtractManager
  android.view.viewextract.AccessibilityHelper
  android.view.viewextract.WebViewExtractHelper
  android.view.viewextract.VisibleImageProcessRule
  android.view.viewextract.BitmapCaptureCanvas

oplus-services.jar:
  com.android.server.wm.OplusViewExtractManagerService
  com.android.server.wm.OplusViewExtractWindowHelper
```

`OplusActivityTaskManagerServiceEnhance.requestViewExtractData()` 强制要求
`com.oplus.permission.safe.WINDOW`。`AccessibilityHelper.buildNodeJson` 输出
`tx/cd/hi/sd/bp/bs/bw/ac/ri/av/vi` 等字段，`reportJsonResult` 限制 JSON 约 900,000 字符；
`WebViewExtractHelper` 提供 `getNodes/getLeafNodes/getDocumentHTML/evaluateJs`。
这是整窗/跨窗口提取，不是 Content Portal 自己的主路径。

Content Portal 17.4.2 的静态证据：

- `ContentPortalService.onBind()` 读取 `ClipData`；
- `d6/x0` 面板 `M(ClipData)` 解析内容，`utils.k.d` 最多处理 99 个 Item；
- `d6/b0` / `d6/z0` 会用 `OplusScreenCapture` / `WindowScreenShotUtils` 截整屏，
  但主要用于面板背景/模糊（`preloadScreenShotBackground`、`setScreenShot`）；
- 没有找到 `ViewExtractSDK` / `IAssistDataReceiver` 的直接引用。

因此：OPPO 的整屏 ViewExtract 是真实存在的独立能力；Content Portal 自身是否间接调用
尚未证实。本期不做整屏，只保留记录。

---

## 6. Plan B：绕过 AES 服务直接投递

### 6.1 架构

```text
目标应用进程
  AES 5.1.7 classes.dex（已插桩）
    -> Interceptor.dispatchTouchEvent -> Bridge.ping（加载证明）
    -> Interceptor.l -> Bridge.recordTouch（触点）
    -> Interceptor.n -> Bridge.getPageConfig（本地合成 PageConfig）
    -> ViewContentFetcher.fetchViewContent -> Bridge.enhanceFetchViewContent
    -> d/c.b / Interceptor.l -> Bridge.deliverContent（直接投递）

我们的新 App
  -> 接收 Bridge 发来的内容
  -> 显示悬浮面板
```

关键点：**不调用 `ContentCatcherManager.onContentCatched(Content)`**。
原版这是 IPC 到 AES 进程的入口；Plan B 改成 `Bridge.deliverContent`，
因此不需要 AES 进程、不需要传送门、不需要 AES 配置 prefs、不需要
`SET_ACTIVITY_WATCHER`。

### 6.2 本期最小实现

`poc/src/dev/dragshare/catcher/Bridge.java` 只做：

| 方法 | 作用 |
|---|---|
| `ping(MotionEvent, View, Activity)` | 每次触摸入口打日志，证明 classes2.dex 在目标进程加载 |
| `recordTouch(PointF, PointF, View)` | 长按入口记录坐标并打日志 |
| `getPageConfig(Object token)` | 先尝试原 AES 服务配置；为空则反射合成 `PageConfig`，加入 `pick_mode` / `image_pick_mode` |
| `enhanceFetchViewContent(View, int, boolean, Bundle)` | 简单 Text/Image 提取：TextView 全文、contentDescription 回退、ImageView BitmapDrawable/Drawable 绘制、512px 缩放；Compose/WebView 暂不接 |
| `deliverContent(Object content)` | 反射读取 `Content.mCatchType` / `mResultMap`，打日志（含文本、bitmap 尺寸）；后续改成发给新 App |

`Bridge` 只依赖 Android SDK 和反射，不直接 import 任何 `miui.*` 类型。

### 6.3 Smali 插桩点

脚本：`poc/scripts/patch_aes_smali.py`

1. `Interceptor.dispatchTouchEvent` 开头：
   `invoke-static {p1,p2,p3}, Bridge->ping(MotionEvent, View, Activity)V`
2. `Interceptor.l` 开头：
   `invoke-static/range {p1..p3}, Bridge->recordTouch(PointF, PointF, View)V`
3. `Interceptor.n()` 的 `getPageConfig` 调用替换为：
   `invoke-static {v3}, Bridge->getPageConfig(Object)Object` + `check-cast` 到 `PageConfig`
4. `Interceptor.l` 的 `onContentCatched(Content)` 调用替换为：
   `invoke-static {v0}, Bridge->deliverContent(Object)V`
5. `d/c.b` 的 `onContentCatched(Content)` 调用替换为：
   `invoke-static {v2}, Bridge->deliverContent(Object)V`
6. `ViewContentFetcher.fetchViewContent(View,int,boolean)` 原方法改名为
   `fetchViewContentImpl`，新增同名包装层调用
   `Bridge->enhanceFetchViewContent(View,int,boolean,Bundle)Bundle`

### 6.4 构建产物与静态验证

产物：

```text
poc/build/apk/ContentCatcher-poc.apk
```

构建步骤：

```bash
# 1. 编译 classes2.dex
javac --release 11 -cp platforms/android-37/android.jar -d build/classes src/.../Bridge.java
jar --create --file build/bridge.jar -C build/classes .
d8 --min-api 29 --lib platforms/android-37/android.jar --output build/dex build/bridge.jar
# 产物 build/dex/classes.dex -> 重命名为 classes2.dex

# 2. apktool 解码 + 插桩 + 回编译
java -jar apktool.jar d -f -o work/decoded "Application Extension Service_5.1.7.apk"
python scripts/patch_aes_smali.py work/decoded
java -jar apktool.jar b work/decoded -o work/rebuilt.apk

# 3. 从 rebuilt.apk 取 classes.dex，与原 APK 的 resources/manifest 组合
#    versionCode 520260911 -> 999999999，加入 classes2.dex
python scripts/repack_apk.py <original.apk> build/apk/classes.dex build/apk/classes2.dex \
    build/apk/unsigned.apk 999999999

# 4. 对齐 + 签名
zipalign -p -f -v 4 build/apk/unsigned.apk build/apk/aligned.apk
apksigner sign --ks signing/poc.jks --ks-key-alias poc \
    --ks-pass pass:pocpass --key-pass pass:pocpass \
    --out build/apk/ContentCatcher-poc.apk build/apk/aligned.apk
```

静态验证（dexsql 对成品 APK）：

```text
classes.dex  : 286,252 bytes
classes2.dex : 6,784 bytes
classes2.dex 中唯一类：Ldev/dragshare/catcher/Bridge;
classes.dex 引用：
  Interceptor.dispatchTouchEvent -> Bridge.ping
  Interceptor.l -> Bridge.recordTouch
  Interceptor.l -> Bridge.deliverContent
  Interceptor.n -> Bridge.getPageConfig
  ViewContentFetcher.fetchViewContent -> Bridge.enhanceFetchViewContent
  d/c.b -> Bridge.deliverContent
AndroidManifest: versionCode=999999999，sharedUserId=android.uid.system 保留
```

### 6.5 风险

1. **安装/签名是第一位风险**：AES 是 `sharedUserId=android.uid.system`，
   重签后需要 CorePatch 保留 system UID。安装失败时看
   `INSTALL_FAILED_SHARED_USER_INCOMPATIBLE` / `INSTALL_FAILED_INVALID_APK`。
2. `classes2.dex` 是否被目标进程 PathClassLoader 加载：静态上成立，需实机日志确认。
3. `Bridge.getPageConfig` 反射合成 `PageConfig` 依赖 `FeatureInfo` / `PageConfig`
   字段名；如果字段被裁剪或版本差异，会回退为 null（不影响 `ping` 加载证明）。
4. `InterceptorProxy.dispatchTouchEvent` 需要 `open_content_extension_clipboard_mode=1`；
   测试前必须设置，否则 `ping` 不会触发。
5. 目标应用进程可能缓存旧 AES 类加载器；安装后必须 force-stop 目标应用再打开。

---

## 7. 实机测试步骤（待执行）

```bash
# 0. 确认设备
adb devices

# 1. 安装 patched AES（CorePatch 已开启）
adb install -r -d poc/build/apk/ContentCatcher-poc.apk

# 2. 打开触摸注入开关（AES 服务不是必须，但 InterceptorProxy 需要这个设置）
adb shell settings put system open_content_extension_clipboard_mode 1
adb shell settings put system content_catcher_network_enabled_content_extension 1

# 3. 清日志，强制重启一个目标应用
adb logcat -c
adb shell am force-stop com.android.settings
adb shell am start -a android.settings.SETTINGS
sleep 1

# 4. 点一下/长按一下设置页面
adb shell input tap 600 900
# 或
adb shell input swipe 600 900 600 900 800

# 5. 看日志
adb logcat -d -s DragSharePoC:D Interceptor_Catcher:D
```

期望看到：

```text
DragSharePoC: Bridge loaded pid=... uid=... (classes2.dex)
DragSharePoC: ping action=0 view=... activity=...
```

如果 `ping` 出现，说明：

- `classes2.dex` 被目标应用进程加载；
- `classes.dex` 的静态转发生效；
- AES DEX 确实运行在目标应用进程，而不是 AES 自己的进程。

如果只有 `Bridge loaded` 没有 `ping`，说明 `mContentExtensionEnabled` 还没生效
（重新 resume 目标应用或检查设置）。

如果两者都没有，说明：

- APK 没有真正覆盖安装（`pm path` 仍指向 `/product/app/...`）；或
- CorePatch 没有放行共享 UID / 签名；或
- `InterceptorProxy.create` 被黑名单/国际版分支拦截。

---

## 8. 当前状态

- [x] AES 5.1.7 静态结构核对
- [x] framework 加载链核对
- [x] Plan B 最小 `Bridge.java` / `classes2.dex`
- [x] 5 处 smali 插桩
- [x] Apktool 回编译 / zipalign / apksigner
- [x] 成品静态验证（classes2.dex、引用、versionCode）
- [x] 实机安装（2026-10-30，`adb install -r -d` 成功）
- [x] 实机日志确认 `Bridge loaded` / `ping` / `longPress` / `enhance` / `deliver`
- [x] 简单 TextView / ImageView 提取接入并实测
- [x] `deliverContent` 改成启动新 App 主页并传文字/图片
- [x] 桥接 App 主页最小实现并实测
- [ ] 后续接 Compose 语义树、WebView `elementFromPoint`、更多图片回退
- [ ] 后续再考虑悬浮窗/多卡片/动作列表

### 8.1 实机结果（2026-10-30）

安装后：

```text
pm path com.miui.contentcatcher
  -> /data/app/.../com.miui.contentcatcher-.../base.apk
dumpsys package
  -> versionCode=999999999
```

Settings 进程：

```text
Bridge loaded pid=20759 uid=1000 (classes2.dex)
PageConfig features=2
ping action=0 view=DecorView activity=com.android.settings.MiuiSettings pid=20759 uid=1000
```

小红书（普通第三方应用，uid=10372）：

```text
Bridge loaded pid=21275 uid=10372 (classes2.dex)
PageConfig features=2
ping action=0/1 view=DecorView activity=com.xingin.xhs.index.v2.IndexActivityV2
longPress first=PointF(610.0, 1200.0) second=PointF(0.0, 0.0) view=DecorView
enhance view=com.xingin.comment.media.browser.image.item.large.CommentBrowserLargeImageView flags=3 origin=null
deliver content catchType=4 mapSize=5/6
deliver item key=com.miui.contentextension#image_pick#image_pick_mode value=Bundle text=null hasBitmap=true
```

知乎（普通第三方应用，uid=10375）：

```text
Bridge loaded pid=28840 uid=10375 (classes2.dex)
PageConfig features=2
ping action=0 view=DecorView activity=com.zhihu.android.app.ui.activity.MainActivity
longPress first=PointF(610.0, 1200.0) second=PointF(0.0, 0.0) view=DecorView
enhance view=com.zhihu.android.ui.shared.sdui.widget.SDUIDividerView flags=3 origin=null
```

**结论：classes2.dex 确实被加载进了普通第三方应用进程；smali 转发、合成 PageConfig、
长按回调、`fetchViewContent` 包装层和 `deliverContent` 都实际执行了。**

### 8.2 简单文字/图片提取实测（2026-10-30）

在 Plan B 骨架上加入 `Bridge.extractSimple`：

- `TextView`：`getText()` 全文；为空时回退 `contentDescription`；
- `ImageView`：`BitmapDrawable.getBitmap()`；否则把 `Drawable` 绘制成 Bitmap；
- 无 Drawable 时回退 `view.draw(Canvas)`；
- Bitmap 最长边缩放到 512px，避免 Binder 过大；
- `EditText` 直接跳过。

知乎正文 `SDUIHtmlTextView`（TextView 子类）实测：

```text
longPress first=PointF(610.0, 699.0)
enhance hit 昨天看到一个人名，问了各种国产模型，没一个能一次性答对。…… catchType=1 bitmap=null
deliver content catchType=4 mapSize=6
deliver item key=com.miui.contentextension#text_pick#pick_mode
  value=Bundle text=昨天看到一个人名，问了各种国产模型，没一个能一次性答对。…… catchType=1 bitmap=null
```

知乎顶部 `ZHDraweeView`（ImageView 子类）实测：

```text
longPress first=PointF(610.0, 345.0)
enhance hit  catchType=2 bitmap=512x55 view=com.zhihu.android.base.widget.ZHDraweeView
deliver content catchType=4 mapSize=6
deliver item key=com.miui.contentextension#image_pick#image_pick_mode
  value=Bundle text=null catchType=2 bitmap=512x55
```

**结论：简单文字和图片都能从目标应用的真实 View 中取到，并沿
`fetchViewContent -> Interceptor.d -> d/c.f -> deliverContent` 到达 Bridge。**
下一步再接 Compose 语义树、WebView DOM 和更完整的图片回退。

### 8.3 桥接 App 最小实现（2026-10-30）

新建普通 App：

```text
包名：com.dragshare.portal
主页：com.dragshare.portal.MainActivity
APK：poc/bridge-app/build/DragShareBridge.apk
```

`Bridge.deliverContent` 不再只打日志，而是：

```text
从 Content.mResultMap 里挑选一个 Bundle
  -> 优先带 viewContent 的 Bundle
  -> 其次带 viewBitmap 的 Bundle
  -> 显式启动 com.dragshare.portal/.MainActivity
  -> extras: viewContent / viewBitmap / catchType / sourcePkg
```

主页只做一件事：显示文字或图片，不做悬浮窗。

实测（知乎正文长按）：

```text
DragSharePoC: enhance hit ... catchType=1 bitmap=null view=SDUIHtmlTextView
DragSharePoC: forward -> portal text=232 chars bitmap=null source=com.zhihu.android
DragShareBridge: render text=232 chars bitmap=null source=com.zhihu.android catchType=1
mCurrentFocus=Window{... com.dragshare.portal/com.dragshare.portal.MainActivity}
```

实测（知乎图片长按）：

```text
DragSharePoC: enhance hit catchType=2 bitmap=195x52 view=ZHDraweeView
DragSharePoC: forward -> portal text=null bitmap=195x52 source=com.zhihu.android
DragShareBridge: render text=null bitmap=195x52 source=com.zhihu.android catchType=2
```

#### 关于启动方式（2026-10-30 修正）

最初看到 `dumpsys window` 的焦点仍是知乎、portal task 显示为 `mode=freeform`，曾误判为
Activity 启动/窗口模式问题，把 `MainActivity` 改成了：

```xml
android:launchMode="singleTask"
android:resizeableActivity="false"
android:screenOrientation="portrait"
```

实际原因是测试时知乎挂在**小窗**里，焦点自然不会变化；不是 Activity 启动问题。
现已回退为：

```xml
android:launchMode="singleTop"
```

不强制全屏、不锁定竖屏、不设置 `resizeableActivity`。主页显示内容的能力不变。

当前设备状态（测试后）：

```text
已安装 patched AES（/data/app，versionCode 999999999）
测试期间 open_content_extension_clipboard_mode = 1
测试完成后已恢复 open_content_extension_clipboard_mode = 0
content_catcher_network_enabled_content_extension = 0
AES 进程可能因 system_server 绑定仍在运行，重启后恢复原状
```

复现时重新执行 `poc/scripts/run_on_device.sh`，或手动把
`open_content_extension_clipboard_mode` 设为 1 后重启目标应用即可。

注意：`ping` 当前会在每个 ACTION_MOVE 打日志，日志很吵；后续应只保留首次加载和
ACTION_DOWN/UP，或在 release 版本关闭日志。

---

## 9. 关键文件索引

```text
poc/
  src/dev/dragshare/catcher/Bridge.java
  scripts/patch_aes_smali.py
  scripts/repack_apk.py
  work/decoded/                          # apktool 解码
  work/rebuilt.apk                       # 回编译产物（含 patched classes.dex）
  build/dex/classes.dex                  # d8 产物（即 classes2.dex）
  build/apk/ContentCatcher-poc.apk       # 最终可安装 APK
  signing/poc.jks                        # PoC 测试签名
```

关键方法：

```text
com.miui.contentcatcher.Interceptor.dispatchTouchEvent(MotionEvent, View, Activity)
com.miui.contentcatcher.Interceptor.l(PointF, PointF, View)
com.miui.contentcatcher.Interceptor.n()
com.miui.catcherpatch.ViewContentFetcher.fetchViewContent(View, int, boolean)
d/c.b(...)
dev.dragshare.catcher.Bridge.ping(...)
dev.dragshare.catcher.Bridge.getPageConfig(Object)
dev.dragshare.catcher.Bridge.enhanceFetchViewContent(...)
dev.dragshare.catcher.Bridge.deliverContent(Object)
```
