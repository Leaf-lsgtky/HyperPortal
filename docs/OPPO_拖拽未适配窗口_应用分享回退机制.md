# OPPO 内容传送门：拖到“未适配原生拖拽”的窗口时，为什么会打开目标 App 的分享面板

> 分析对象：
> - `oppo/内容传送门_17.4.2.apk`（`com.oplus.contentportal`，versionCode `17004002`）
> - `oppo/oplus-services.jar`（OPPO system_server 扩展）
> - `oppo/services.jar`（AOSP WMS 基线）
> - 对照：`xiaomi/services.jar`（HyperOS 4 / Android 17 的 WMS 实现）
>
> 结论先行：**“拖到未适配窗口 → 打开目标 App 分享面板”的核心逻辑不在内容传送门 APK 里，而在 OPPO system_server 的 `com.android.server.wm.OplusGlobalDragAndDropCallback`。** 内容传送门 APK 负责底部中转站面板和 dock target 的点击跳转；窗口级回退由 WMS 的拖拽回调完成。

---

## 0. 结论摘要

OPPO 的完整判定链是：

```text
目标 App 的 ViewRootImpl 收到 ACTION_DROP
  -> View.dispatchDragEvent(event) 返回 false（没有 OnDragListener / 不消费）
  -> Session.reportDropResult(window, false)
  -> DragDropController.reportDropResult(window, false)
  -> IDragDropCallback.preReportDropResult(window, false)
  -> OplusGlobalDragAndDropCallback.preReportDropResult(...)
       -> createSendIntent(targetWindow, consumed=false)
       -> createSendIntentInner(clipData, targetContext)
       -> sendIntent.setPackage(targetWindow.packageName)
       -> resolveActivity(pm) != null
       -> startChooseActivity(Intent.createChooser(sendIntent, "share"))
  -> 系统 Resolver 只匹配目标包
  -> 进入目标 App 的 ACTION_SEND 入口
  -> QQ 的入口就是“发送给好友 / 发送到电脑 / QQ空间”
```

关键点：

1. **触发条件不是“目标 App 没有注册拖拽监听”这种静态判断，而是运行时结果 `consumed == false`。**
2. **目标包名来自最终上报 drop 的 `WindowState.mAttrs.packageName`，不是来自内容传送门 APK。**
3. **构造的是标准 `ACTION_SEND` / `ACTION_SEND_MULTIPLE`，只 `setPackage(targetPkg)`，不指定 `className`。**
4. **因此最终进入的是目标 App 自己声明的“分享入口 Activity”**；QQ 的分享入口 UI 就是“发送给好友 / 发送到电脑”等选项。
5. **总开关开启时默认对所有 App 生效**：`persist.sys.all_drop_to_share_enable` 默认 `true`，只排除 `com.oplus.padconnect` 和 `com.google.android*`；此外还受 `globalDragShareSwitch` / `mGlobalDragShareDisable` 总开关约束。
6. **同一个窗口不触发**：如果目标窗口就是拖拽起始窗口，直接返回 `null`，避免把内容“分享回自己”。

---

## 1. 两条容易混淆的路径

OPPO 的“拖到 App 上打开分享”有两条完全不同的实现路径：

| 路径 | 触发方式 | 代码位置 | 目标包来源 | 是否指定 Activity |
| :--- | :--- | :--- | :--- | :--- |
| **A. 窗口级回退** | 拖到某个 App 的窗口，目标不消费 `ACTION_DROP` | system_server：`OplusGlobalDragAndDropCallback` | 最终上报 drop 的 `WindowState.mAttrs.packageName` | 否，只有 `setPackage` |
| **B. 中转站 dock target** | 拖到内容传送门底部面板里的 App 图标 | APK：`SettlementView` / `JumpUtils` | 云端/本地 `ServiceInfo.pkgName` | 可选 `ServiceInfo.className` |

用户描述的“拖出去的窗口不允许发送图片或文字（没适配原生拖拽）”，对应的是 **路径 A**。
“QQ 会打开发送给好友、发送到电脑”在两条路径下都可能出现，但原因不同：

- **路径 A**：system_server 不知道 QQ 内部页面，只能发通用 `ACTION_SEND`，QQ 自己弹分享入口。
- **路径 B**：如果云端 `ServiceInfo` 没有配 `className`，也会落到 QQ 的通用分享入口；如果配了 `className`，就可以直接进“发送给好友”页面，不再弹 QQ 分享面板。

本文重点分析路径 A，最后补充路径 B 的 APK 侧实现，方便对照移植。

---

## 2. 路径 A：system_server 的全局拖拽分享回退

### 2.1 调用链

```text
┌─────────────────────────────────────────────────────────────────────┐
│ 目标 App 进程                                                        │
│                                                                     │
│  ViewRootImpl.DragEventReceiver                                     │
│    ACTION_DROP                                                      │
│      -> View.dispatchDragEvent(event)                               │
│         -> 没有 OnDragListener / 返回 false                          │
│      -> mWindowSession.reportDropResult(this, false)                │
└─────────────────────────────────────────────────────────────────────┘
                              │ Binder
                              v
┌─────────────────────────────────────────────────────────────────────┐
│ system_server                                                       │
│                                                                     │
│  DragDropController.reportDropResult(IWindow window, boolean result)│
│    -> IDragDropCallback.preReportDropResult(window, result)         │
│       -> OplusGlobalDragAndDropCallback.preReportDropResult(...)    │
│          -> createSendIntent(targetWindow, result=false)            │
│             -> createTempSendIntent(targetWindow)                   │
│                -> createSendIntentInner(mDragData, context)         │
│             -> sendIntent.setPackage(targetPkg)                     │
│          -> resolveActivity(packageManager)                         │
│          -> startChooseActivity(sendIntent, options)                │
│             -> Intent.createChooser(sendIntent, "share")            │
│             -> ActivityStartController.startActivityInPackage(...)  │
└─────────────────────────────────────────────────────────────────────┘
```

`DragDropController.reportDropResult()` 在 OPPO 和 AOSP 中都是先调用 `preReportDropResult()`，再进入 WMS 锁处理 `DragState.endDragLocked()`。OPPO 把回退逻辑放在 `preReportDropResult()`，因此它发生在 WMS 锁之前，可以安全地启动 Activity。

注册链也印证了这一点：

```text
OplusWindowManagerServiceEx.systemReady()
  -> new OplusGlobalDragAndDropCallback(context, wmServiceEx)
  -> OplusGlobalDragAndDropManagerService.setGlobalDragAndDropCallback(callback)
  -> WindowManagerService 的 IDragDropCallback 指向该 callback
```

### 2.2 `preReportDropResult()`：入口

反编译位置：`oplus-services.jar` → `com.android.server.wm.OplusGlobalDragAndDropCallback`

方法签名：

```java
public void preReportDropResult(IWindow window, boolean consumed)
```

核心步骤（按 smali 还原）：

```java
// 1. 记录埋点结果
mDropResult = consumed ? "success_drop" : "fail_01";

// 2. 总开关
if (!OplusGlobalDragAndDropRUSConfigManager.getInstance().isGlobalDragShareEnable()) {
    Slog.d(TAG, "GlobalDragShare disabled!");
    return;
}

// 3. 必须有拖拽起始窗口
if (mStartDragWindow == null) {
    Slog.d(TAG, "preReportDropResult null start drag window!");
    return;
}

// 4. 把上报 drop 的 IWindow 映射成 WindowState
WindowState target = null;
if (mService != null && window != null) {
    IBinder token = window.asBinder();
    target = mService.mWindowMap.get(token);
    if (target != null) {
        mDropWindow = target;
        mDropWindowMode = target.getWindowingMode();
    }
}

// 5. 构造发送 Intent
Intent sendIntent = createSendIntent(target, consumed);

// 6. 只有目标 App 真的能处理 ACTION_SEND 才启动
if (sendIntent != null
        && sendIntent.resolveActivity(mContext.getPackageManager()) != null) {

    ActivityOptions options = ActivityOptions.makeBasic();
    Task task = (mDropWindow != null) ? mDropWindow.getTask() : null;
    if (task != null) {
        options.setLaunchTaskId(task.mTaskId);
    }

    if (ifNeedDisbaleStrictMode(sendIntent)) {
        safelyStartChooseActivity(sendIntent, options); // file:// 专用
    } else {
        startChooseActivity(sendIntent, options);
    }

    mDropResult = "success_send";
    consumed = true; // 只影响本方法内的局部变量
}

// 7. 记录消费结果，供 OPPO 的 DragDropControllerExt 覆盖最终 drag result
mConsumeResult = consumed;
```

注意最后一行：`mConsumeResult` 会被 OPPO 的 `DragDropController` 扩展读取，用来把本次拖拽标记为“已消费”，从而**跳过 cancel fly-back 动画**。这是 OPPO 的定制点，AOSP/HyperOS 没有这个字段。

### 2.3 `createSendIntent()`：判定“目标窗口是否值得回退”

方法签名：

```java
Intent createSendIntent(WindowState targetWindow, boolean consumed)
```

核心逻辑：

```java
if (targetWindow == null) {
    return null;
}

String targetPkg = targetWindow.mAttrs.packageName;
if (targetPkg == null) {
    Slog.d(TAG, "createSendIntent targetPkg name is Null!");
    return null;
}

// 目标用户：优先取目标窗口所属 UID 的 userId，否则当前用户
if (targetWindow.mActivityRecord != null) {
    mCurrentUid = UserHandle.getUserId(targetWindow.getOwningUid());
} else {
    mCurrentUid = ActivityManager.getCurrentUser();
}

String targetComponent = (targetWindow.mActivityRecord != null)
        ? targetWindow.mActivityRecord.shortComponentName
        : null;

// 关键条件 1：有拖拽数据
// 关键条件 2：目标没有消费 drop
// 关键条件 3：不是拖回起始窗口
if (mDragData == null || consumed) {
    return null;
}
if (targetWindow.equals(mStartDragWindow)) {
    Slog.d(TAG, "createSendIntent same window!!");
    return null;
}

// 关键条件 4：目标包在允许列表
if (!OplusGlobalDragAndDropRUSConfigManager.getInstance()
        .isInSupportGlobalDropPkgList(targetPkg)) {
    return null;
}

// 关键条件 5：目标组件不在禁止列表
if (OplusGlobalDragAndDropRUSConfigManager.getInstance()
        .isInUnSupportGlobalDropCpnList(targetComponent)) {
    return null;
}

// 构造 ACTION_SEND / SEND_MULTIPLE
Intent sendIntent = createTempSendIntent(targetWindow);
if (sendIntent == null) {
    return null;
}

// 克隆/多用户：告诉目标以哪个 userId 启动
sendIntent.getIntentExt().setGlobalDragTargetUserId(mCurrentUid);
sendIntent.getIntentExt().addOplusFlags(0x2000);
sendIntent.getIntentExt().addOplusFlags(0x800);

// 如果目标在 flexible 全屏 task 中，带上 taskId
Task task = targetWindow.getTask();
if (task != null && FlexibleWindowUtils.isFlexibleTaskAndHasCaption(mStartDragWindow.getTask())
        && task.getWrapper().getExtImpl().isFullScreenTask()) {
    sendIntent.getIntentExt().setGlobalDragTargetTaskId(task.mTaskId);
}

// 最后才限定包名
sendIntent.setPackage(targetPkg);

return sendIntent;
```

这里有一个很重要的设计：

> **system_server 只知道“目标包”，不知道“目标页面”。**
> 所以它只能发通用 `ACTION_SEND`，让目标 App 自己决定进哪个分享入口。
> 这正是 QQ 会弹出“发送给好友 / 发送到电脑”的根本原因。

### 2.4 `createSendIntentInner()`：ClipData → ACTION_SEND

方法签名：

```java
Intent createSendIntentInner(ClipData clipData, Context context)
```

核心逻辑：

```java
Intent intent = new Intent();
intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
intent.setClipData(clipData);

int count = clipData.getItemCount();
if (count == 0) {
    Slog.e(TAG, "createSendIntentInner create intent fail! item is null");
    return null;
}

if (count == 1) {
    intent.setAction(Intent.ACTION_SEND);

    Intent itemIntent = analyzeClipDataItemMimeType(clipData, 0, context);
    if (itemIntent == null) {
        return null;
    }

    intent.setType(itemIntent.getType());

    String text = itemIntent.getStringExtra(Intent.EXTRA_TEXT);
    intent.putExtra(Intent.EXTRA_TEXT, text);

    Parcelable stream = itemIntent.getParcelableExtra(Intent.EXTRA_STREAM);
    if (stream != null) {
        intent.putExtra(Intent.EXTRA_STREAM, (Uri) stream);
    }

    mDragFileType = itemIntent.getType();
} else {
    intent.setAction(Intent.ACTION_SEND_MULTIPLE);

    ArrayList<Uri> uris = new ArrayList<>();
    ArrayList<String> texts = new ArrayList<>();
    String mimeType = null;

    for (int i = 0; i < count; i++) {
        Intent itemIntent = analyzeClipDataItemMimeType(clipData, i, context);
        if (itemIntent == null) {
            continue;
        }

        String itemType = itemIntent.getType();

        // 混合类型直接放弃
        if (mimeType != null && !mimeType.equals(itemType)) {
            Slog.w(TAG, "createSendIntentInner mix File Drag, Abort!");
            mDragFileType = "mix";
            return null;
        }

        // 多个 text/plain 文件被过滤
        if ("text/plain".equals(mimeType)) {
            Slog.d(TAG, "multiple_send txt file, filter it!");
            continue;
        }

        if (mimeType == null) {
            mimeType = itemType;
        }

        Uri uri = itemIntent.getParcelableExtra(Intent.EXTRA_STREAM);
        String text = itemIntent.getStringExtra(Intent.EXTRA_TEXT);

        if (uri != null) {
            uris.add(uri);
        } else if (text != null) {
            texts.add(text);
        }
    }

    intent.setType(mimeType);

    if (!uris.isEmpty()) {
        intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
        mDragFileType = mimeType;
    } else if (!texts.isEmpty()) {
        intent.putStringArrayListExtra(Intent.EXTRA_TEXT, texts);
        mDragFileType = "text/plain";
    }
}

// 授予目标读/写 URI 权限
intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
        | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);

return intent;
```

几个容易忽略的细节：

- `intent.setClipData(clipData)` 必须设置，否则目标 App 可能拿不到 `content://` 的读权限。
- 单项时 `EXTRA_STREAM` 是 `Uri`；多项时是 `ArrayList<Uri>`。
- 多项混合 MIME 直接放弃，不会“猜一个类型”硬发。
- 多项纯文本会被过滤，这是 OPPO 的现有行为，不是 bug。
- `file://` URI 会走 `safelyStartChooseActivity()`，临时关闭 `StrictMode.disableDeathOnFileUriExposure()`，避免 system_server 因 FileUriExposedException 崩溃。

### 2.5 `analyzeClipDataItemMimeType()`：单项解析

方法签名：

```java
Intent analyzeClipDataItemMimeType(ClipData clipData, int index, Context context)
```

逻辑：

```java
Intent intent = new Intent();
ClipData.Item item = clipData.getItemAt(index);

// 1. 纯文本
CharSequence text = item.getText();
if (text != null) {
    intent.putExtra(Intent.EXTRA_TEXT, text.toString());
    intent.setType("text/plain");
}

// 2. HTML 只记录日志，不写入 EXTRA_HTML_TEXT
String html = item.getHtmlText();
if (html != null) {
    // 仅日志
}

// 3. Item 自带的 Intent 只记录日志，不转发
Intent itemIntent = item.getIntent();
if (itemIntent != null) {
    // 仅日志
}

// 4. 从 ClipDescription 取 MIME
ClipDescription desc = clipData.getDescription();
if (desc.getMimeTypeCount() > 0 && index < desc.getMimeTypeCount()) {
    String mime = desc.getMimeType(index);
    if (mime != null) {
        intent.setType(mime);
    }
}

// 5. URI
Uri uri = item.getUri();
if (uri != null) {
    String mime = getMimeType(uri.toString());
    if (mime == null) {
        mime = getTypeByUid(uri, mCurrentUid);
    }

    intent.setType(mime);
    intent.putExtra(Intent.EXTRA_STREAM, uri);

    // text/plain 的 URI 被视为 txt 文件，直接丢弃
    if ("text/plain".equals(mime)) {
        Slog.d(TAG, "analyzeClipDataItemMimetype txt File!");
        mDragFileType = ".txt";
        return null;
    }
} else {
    // 没有 URI，也不是 text/plain，视为无效
    if (!"text/plain".equals(intent.getType())) {
        Slog.d(TAG, "analyzeClipDataItemMimetype drag uri is null! Ivalid it");
        return null;
    }
}

return intent;
```

这意味着：

- 纯文本 → `text/plain` + `EXTRA_TEXT`。
- 图片/文件 → `image/*` / `application/pdf` 等 + `EXTRA_STREAM`。
- 文本文件（`content://...` 且 MIME 为 `text/plain`）→ **不会**作为流发送。
- 带 `Intent` 的 ClipData Item → 不转发。
- HTML 文本 → 只发纯文本，不发 HTML。

### 2.6 `startChooseActivity()`：真正启动

方法签名：

```java
void startChooseActivity(Intent sendIntent, ActivityOptions options)
```

核心逻辑：

```java
Intent chooser = Intent.createChooser(sendIntent, "share");

// 记录拖拽来源 UID，供目标 App / 系统做权限判断
chooser.putExtra("oplus.intent.extra.GLOBAL_DRAG_CREATOR_UID",
        mStartDragWindow.mOwnerUid);

// 让 ATM 记录 creator token，避免后台启动被拦截
mService.mAtmService.mAmInternal.addCreatorToken(
        chooser, mStartDragWindow.getOwningPackage());

// 用 system_server 身份启动
// 注：下面是可读化伪代码，实际是 19 参数的
// ActivityStartController.startActivityInPackage(...) 重载，参数顺序已简化。
mService.mAtmService.getActivityStartController().startActivityInPackage(
        mStartDragWindow.mOwnerUid,          // uid
        Process.myPid(),                     // pid
        Process.myUid(),                     // caller uid
        mStartDragWindow.getOwningPackage(), // calling package
        null,                                // calling feature
        chooser,                             // intent
        null,                                // resolvedType
        null,                                // resultTo
        null,                                // resultWho
        0,                                   // requestCode
        0,                                   // startFlags
        safeOptions,                         // ActivityOptions
        0,                                   // userId
        null,                                // task
        "Drag2Share",                        // reason
        false,                               // validateIncomingUser
        null,                                // pendingIntent
        false                                // allowBackgroundActivityStarts
);
```

几个关键点：

- **用的是 `Intent.createChooser`**，不是直接 `startActivity(sendIntent)`。
  所以系统 Resolver 会先出现；由于 `sendIntent` 已经 `setPackage(targetPkg)`，Resolver 里通常只有目标 App 一个候选。
- 启动者是 system_server，且通过 `ActivityStartController.startActivityInPackage` 走内部启动路径，不受普通后台启动限制。
- `"Drag2Share"` 是 OPPO 的启动 reason，便于系统/埋点识别。
- `addCreatorToken` 和 `GLOBAL_DRAG_CREATOR_UID` 用于让目标 App 获得正确的 URI 权限来源。
- `ifNeedDisbaleStrictMode()` 只在 `EXTRA_STREAM` 是 `file://` 时返回 true；普通 `content://` 不会关闭 StrictMode。

### 2.7 为什么 QQ 会打开“发送给好友 / 发送到电脑”

QQ 的 manifest 中声明了 `ACTION_SEND` 的分享入口 Activity。OPPO 的 `sendIntent` 只做了：

```java
sendIntent.setAction(Intent.ACTION_SEND);
sendIntent.setType("text/plain"); // 或 image/*
sendIntent.putExtra(Intent.EXTRA_TEXT, text);
sendIntent.setPackage("com.tencent.mobileqq");
```

它没有指定 QQ 的具体 Activity，所以 Android 会解析到 QQ 默认的 `ACTION_SEND` 入口。
QQ 的这个入口本身就是分享面板，包含：

```text
发送给好友
发送到电脑
QQ空间
收藏
...
```

所以用户看到的是 **QQ 自己的分享选项**，不是 OPPO 画的 UI，也不是系统分享面板里的 QQ 图标。

如果 OPPO 在 dock target 的 `ServiceInfo` 里配置了 `className`，`JumpUtils` 就会显式启动 QQ 的某个具体 Activity，从而绕过这个面板；但窗口级回退没有 `ServiceInfo`，只能走通用入口。

### 2.8 允许/禁止名单

`OplusGlobalDragAndDropRUSConfigManager` 控制哪些目标包允许回退。

#### 默认模式：全部允许

```java
static final boolean SUPPORT_ALL_DROP_TO_SHARE =
        SystemProperties.getBoolean("persist.sys.all_drop_to_share_enable", true);
```

默认 `true`。此时 `isInSupportGlobalDropPkgList(pkg)`：

```java
if (SUPPORT_ALL_DROP_TO_SHARE) {
    if (!TextUtils.isEmpty(pkg) && pkg.startsWith("com.google.android")) {
        return false;
    }
    return !mUnSupportDropToShareList.contains(pkg);
}
```

`mUnSupportDropToShareList` 在构造函数里初始化为：

```java
Collections.singletonList("com.oplus.padconnect")
```

所以默认模式下：

- 允许：几乎所有第三方 App（QQ、微信、微博、小红书等）。
- 禁止：`com.google.android*`、`com.oplus.padconnect`。
- 空包名不会在前面 `createSendIntent()` 的 `targetPkg == null` 检查中被拦截，但实际 `WindowState.mAttrs.packageName` 几乎不会为空；移植时应自行补一个 `TextUtils.isEmpty()` 判断。

`isInUnSupportGlobalDropCpnList(component)` 在默认模式下直接返回 `false`，即组件级黑名单不生效。

#### 配置模式：按 XML 白名单/黑名单

当 `persist.sys.all_drop_to_share_enable=false` 时，读取：

```text
/data/oplus/os/config/sys_global_drag_share_config.xml
  （不存在则读 /system_ext/oplus/sys_global_drag_share_config.xml）
```

配置对象：`com.oplus.globaldrag.OplusGlobalDragAndDropRUSConfig`

关键字段：

```text
globalDragShareSwitch     // 总开关
supportDropPkgList        // 允许回退的包（OplusGlobalDragAndDropRUSConfigItem）
unSupportCpnList          // 禁止回退的组件
```

`isGlobalDragShareEnable()` 还会检查 `mGlobalDragShareDisable`（运行时禁用标记）。

> 对移植的启示：不要无脑对所有包回退。至少要有“自身包、桌面、SystemUI、输入法、设置、权限控制器”等 denylist；OPPO 的 `com.google.android*` 排除也是同理。

---

## 3. 路径 B：内容传送门 APK 的 dock target

虽然窗口级回退不在 APK 里，但 APK 的 dock target 也有一条“打开 App 分享面板”的路径，理解它有助于区分两者。

### 3.1 绑定与数据来源

`ContentPortalService.onBind()` 从绑定 Intent 中读取：

```text
clip_data          -> ClipData
drag_pkg_detail    -> 拖拽来源/目标包信息（字符串）
surface_control    -> SurfaceControl（拖拽阴影截图）
```

然后创建 `SettlementView`（混淆名 `d6.x0`），由它展示底部面板和目标列表。

注意：`drag_pkg_detail` 是**拖拽来源 App** 的信息，`d6.x0.A0()` 等 AI 记忆/笔记逻辑用它填 `EXTRA_SOURCE_PACKAGE` / `EXTRA_SOURCE_APP_NAME`，**不是落点窗口的包名**。落点窗口包名只有 system_server 的 `WindowState` 知道，这也是窗口级回退必须在 system_server 做的原因。

`DragInfoModel`（`com.oplus.contentportal.drag.e`）负责缓存：

```text
h: dragPackageInfo
j: ClipData
k: SurfaceControl
```

### 3.2 `JumpUtils.c()`：jumpType

`SettlementView.Q0(position)` 取出 `ServiceInfo`，调用 `JumpUtils.c(serviceInfo)`：

```java
int jumpType(ServiceInfo info) {
    String pkg = safe(info.getPkgName());
    String deepLink = safe(info.getDeepLink());
    String action = safe(info.getAction());

    // 1. 当前文本意图 App
    // 实际代码：pkg.equals(com.oplus.contentportal.aiunit.d.i().getString("appName"))
    // 这里用 OIntentUtils.getCurrentAppName() 代称，便于阅读。
    if (!pkg.isEmpty()
            && pkg.equals(OIntentUtils.getCurrentAppName())) {
        return 1;
    }

    // 2. 有 deeplink
    if (!deepLink.isEmpty()) {
        return 2;
    }

    // 3. 有 action
    if (!action.isEmpty()) {
        return 3;
    }

    return 0;
}
```

`Q0` 根据 jumpType 分发：

```text
1 -> H0(serviceInfo, pkg, flagKey)          // action + 克隆处理
2 -> F0(serviceInfo, deepLink, pkg, flagKey) // deeplink
3 -> E0(serviceInfo, pkg, className, action, textKey, isProcessTextKey, flagKey)
```

### 3.3 `JumpUtils.a()`：构造目标 Intent

`E0` 最终调用 `JumpUtils.a(...)`（混淆名 `v.a`），核心逻辑：

```java
Intent intent = new Intent(action);
intent.addCategory(Intent.CATEGORY_DEFAULT);
intent.setPackage(pkg);

if (className != null && !className.isEmpty()) {
    intent.setComponent(new ComponentName(pkg, className));
}

intent.addFlags(0x10000001); // NEW_TASK | GRANT_READ_URI_PERMISSION

// QQ 额外加 CLEAR_TOP
if ("com.tencent.mobileqq".equals(pkg)) {
    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
}

intent.setType("*/*");

if (clipData.getItemCount() > 1) {
    intent.setAction(Intent.ACTION_SEND_MULTIPLE);
    intent.setType(/* 统一 MIME */);
    intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uriList);
} else {
    ClipData.Item item = clipData.getItemAt(0);
    CharSequence text = item.getText();

    if (text != null) {
        intent.setType("text/*");
        // 优先使用 textKey 指定的 extra key
        intent.putExtra(textKey != null ? textKey : Intent.EXTRA_TEXT, text.toString());

        if (Intent.ACTION_PROCESS_TEXT.equals(action)
                || "1".equals(isProcessTextKey)) {
            intent.putExtra(Intent.EXTRA_PROCESS_TEXT, text.toString());
            intent.setType("text/plain");
        }
    } else {
        Uri uri = item.getUri();
        if (uri != null) {
            intent.setType(/* 从 Uri 解析 MIME */);
            intent.putExtra(Intent.EXTRA_STREAM, uri);
        }
    }
}

// 来源 App ID
intent.putExtra("sourceAppId", extractPackageName(sourceAppId));
```

关键区别：

| 条件 | 结果 |
| :--- | :--- |
| `className` 非空 | 显式 `ComponentName` 启动，直接进具体页面，不弹 App 分享面板 |
| `className` 为空 | 只 `setPackage`，进入 App 默认 `ACTION_SEND` 入口，QQ 弹“发送给好友/发送到电脑” |
| `action = ACTION_SEND` | 目标 App 的分享入口 |
| `action = ACTION_PROCESS_TEXT` | 目标 App 的文本处理入口 |

所以，路径 B 下“是否弹 QQ 分享面板”取决于云端 `ServiceInfo` 有没有配 `className`。
这也解释了为什么 OPPO 的 dock 里可能同时存在：

```text
QQ-发送给好友   （className 指向具体 Activity）
QQ             （className 为空，走通用分享入口）
```

### 3.4 `JumpUtils.i()`：系统分享面板

当目标包是 `com.android.intentresolver` 时，`E0` 调用 `JumpUtils.i()`：

```java
Intent sendIntent = new Intent(action);
sendIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
sendIntent.setType("*/*");

// 根据 ClipData 填 EXTRA_TEXT / EXTRA_STREAM
...

Intent chooser = Intent.createChooser(sendIntent, null);
chooser.addFlags(0x10008001); // NEW_TASK | CLEAR_TASK | GRANT_READ
context.startActivity(chooser);
```

这是内容传送门 dock 里的“分享”目标，打开的是系统分享面板，不是目标 App 的分享入口。
它和路径 A 的区别：

- 路径 A：`setPackage(targetPkg)`，目标 App 自己的分享入口。
- 路径 B-分享：`com.android.intentresolver`，系统 Resolver 列出所有可分享 App。

---

## 4. 与 HyperOS / ohmyAES 的差异

### 4.1 HyperOS 的 WMS 拖拽实现

`xiaomi/services.jar` 中：

- `com.android.server.wm.DragDropController.reportDropResult(IWindow, boolean)` 同样先调用 `IDragDropCallback.preReportDropResult(window, result)`。
- 但 `IDragDropCallback` 是 AOSP 默认实现，没有 OPPO 的“未消费就发 ACTION_SEND”逻辑。
- `DragState` 有：
  - `mData: ClipData`
  - `mDropWindow: WindowState`
  - `mDragWindow: WindowState`
  - `mDragResult: boolean`
  - `mUnhandledDropEvent: DragEvent`
- `DragDropController` 有：
  - `notifyUnhandledDrop(DragEvent, String): boolean`
  - `onUnhandledDropCallback(boolean)`
  - `mGlobalDragListener`

HyperOS 的 `reportDropResult` 在 `consumed == false` 时会调用 `notifyUnhandledDrop(mUnhandledDropEvent, "window-drop")`，让 `IGlobalDragListener` 有机会处理；如果没有 listener，最终 `endDragLocked(false, ...)`，走 cancel fly-back。

### 4.2 可 hook 点

ohmyAES 已经在 `system_server` 有 LSPosed 模块（`PortalSystemModule`），可以新增：

#### 方案 A（推荐）：hook `DragDropController.reportDropResult`

```java
XposedHelpers.findAndHookMethod(
    "com.android.server.wm.DragDropController",
    classLoader,
    "reportDropResult",
    IWindow.class,
    boolean.class,
    new XC_MethodHook() {
        @Override
        protected void beforeHookedMethod(MethodHookParam param) {
            IWindow window = (IWindow) param.args[0];
            boolean consumed = (boolean) param.args[1];
            if (consumed) return;

            // 1. 取 DragState / ClipData
            Object controller = param.thisObject;
            Object dragState = getField(controller, "mDragState");
            if (dragState == null) return;
            ClipData data = (ClipData) getField(dragState, "mData");
            if (data == null) return;

            // 2. IWindow -> WindowState
            Object service = getField(controller, "mService");
            Map<IBinder, Object> windowMap = (Map) getField(service, "mWindowMap");
            Object target = windowMap.get(window.asBinder());
            if (target == null) return;

            String targetPkg = (String) getField(
                    getField(target, "mAttrs"), "packageName");
            String sourcePkg = (String) getField(
                    getField(getField(dragState, "mDragWindow"), "mAttrs"), "packageName");

            // 3. 过滤
            if (targetPkg == null || targetPkg.equals(sourcePkg)) return;
            if (isDenied(targetPkg)) return;

            // 4. 构造并启动
            Intent send = buildSendIntent(data, targetPkg);
            if (send == null) return;
            if (send.resolveActivity(context.getPackageManager()) == null) return;

            Intent chooser = Intent.createChooser(send, "分享");
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivityAsUser(chooser, UserHandle.of(userId));

            // 5. 标记已消费，跳过 fly-back
            param.args[1] = true;
        }
    });
```

注意：

- `beforeHookedMethod` 里 `param.args[1] = true` 会让 `reportDropResult` 走“已消费”分支，跳过 `notifyUnhandledDrop` 和 cancel 动画。
- 如果不想改 `param.args[1]`，可以只启动分享，让阴影飞回；但体验不如 OPPO。
- `context` 可以用 `ActivityThread.currentActivityThread().getSystemContext()`，或 `WindowManagerService.mContext`。
- `userId` 从 `target.getOwningUid()` 取 `UserHandle.getUserId()`，支持分身/多用户。

#### 方案 B：hook `DragState.endDragLocked(boolean, boolean)`

```java
XposedHelpers.findAndHookMethod(
    "com.android.server.wm.DragState",
    classLoader,
    "endDragLocked",
    boolean.class, boolean.class,
    new XC_MethodHook() {
        @Override
        protected void beforeHookedMethod(MethodHookParam param) {
            boolean result = (boolean) param.args[0];
            if (result) return;

            DragState state = (DragState) param.thisObject;
            WindowState target = state.mDropWindow;
            WindowState source = state.mDragWindow;
            if (target == null || target == source) return;
            if (state.mData == null) return;

            // 只在“真的 drop 了”时触发，避免 cancel 也触发
            if (state.mUnhandledDropEvent == null) return;

            // 启动分享
            ...
            // 标记成功，跳过 cancel 动画
            param.args[0] = true;
        }
    });
```

`endDragLocked` 同时用于 drop 和 cancel，必须用 `mUnhandledDropEvent != null` 或 `reportDropResult` 调用栈来区分，否则会在用户取消拖拽时误弹分享面板。**不推荐作为首选。**

#### 方案 C：hook `notifyUnhandledDrop`

```java
XposedHelpers.findAndHookMethod(
    "com.android.server.wm.DragDropController",
    classLoader,
    "notifyUnhandledDrop",
    DragEvent.class, String.class,
    new XC_MethodHook() { ... });
```

`notifyUnhandledDrop` 的 `DragEvent` 带 x/y，可以用 `getTouchableWinAtPointLocked` 找窗口；但它只在 `mGlobalDragListener != null` 且 flags 允许时才会被调用，覆盖不全，不如方案 A 稳定。

### 4.3 最小实现清单

1. 在 `PortalSystemModule.installHooks()` 中注册 `DragDropController.reportDropResult` hook。
2. 只在 `consumed == false` 时处理；`consumed == true` 直接返回。
3. 取 `DragState.mData`；为空则返回。
4. 取目标 `WindowState`；为空则返回。
5. 取 `targetPkg`；为空、等于来源包、命中 denylist 则返回。
6. 构造 `ACTION_SEND` / `ACTION_SEND_MULTIPLE`：
   - `setClipData(data)`
   - `addFlags(NEW_TASK | GRANT_READ_URI_PERMISSION)`
   - 单项：`EXTRA_TEXT` 或 `EXTRA_STREAM`
   - 多项：`EXTRA_STREAM` ArrayList；混合类型放弃
7. `resolveActivity` 为空则返回。
8. `Intent.createChooser(send, "分享")`，`startActivityAsUser` 到目标 userId。
9. `param.args[1] = true`，跳过 fly-back。
10. 加日志：`HyperPortal/DragShareFallback`，记录 `sourcePkg -> targetPkg`、MIME、itemCount、结果。

### 4.4 关键坑

#### 坑 1：`consumed` 必须改，否则阴影飞回

OPPO 通过 `mConsumeResult` + `DragDropControllerExt.getConsumedResult()` 把结果改成 true。
HyperOS 没有这个扩展，最直接的是在 hook 里把 `param.args[1]` 改成 `true`。
如果不改，分享面板会打开，但拖拽阴影会飞回来源位置，观感割裂。

#### 坑 2：URI 权限

`content://` 图片必须：

```java
sendIntent.setClipData(data);
sendIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
```

仅 `putExtra(EXTRA_STREAM, uri)` 不够。
如果目标仍报 `SecurityException`，需要：

- 用 `context.grantUriPermission(targetPkg, uri, FLAG_GRANT_READ_URI_PERMISSION)` 显式授权；
- 或走 `ActivityTaskManagerService` 的内部启动路径，让 ATM 处理 grant；
- 或先把 URI 复制到 HyperPortal 自己的 `FileProvider`（不推荐，增加一次拷贝）。

#### 坑 3：不要分享回自己

必须同时判断：

```java
targetWindow == sourceWindow
targetPkg.equals(sourcePkg)
```

OPPO 只判断了 `targetWindow.equals(mStartDragWindow)`，没有判断同包不同窗口。
在分屏/小窗场景下，同包不同窗口也可能误触发；建议加上包名判断。

#### 坑 4：不要对系统窗口回退

至少排除：

```text
com.android.systemui
com.miui.home
com.android.launcher
com.android.settings
com.android.permissioncontroller
com.github.leaf.hyperportal
com.dragshare.portal
```

否则在桌面、通知栏、设置页松手时可能弹出莫名其妙的分享面板。

#### 坑 5：分身/多用户

目标窗口的 `getOwningUid()` 可能属于分身用户。
启动时要：

```java
int userId = UserHandle.getUserId(target.getOwningUid());
context.startActivityAsUser(chooser, UserHandle.of(userId));
```

不要固定 `UserHandle.CURRENT`。

#### 坑 6：内容传送门面板自己

如果拖拽落点在内容传送门面板上，`mDropWindow` 可能是 `com.oplus.contentportal` 的窗口，而不是底层 App。
移植后要排除自己的包名，避免“把内容分享给内容传送门自己”。

#### 坑 7：混合 MIME / 多图

OPPO 对多项混合 MIME 直接放弃。
如果 ohmyAES 想更宽松，可以按第一项类型发送，但目标 App 可能只识别部分文件；建议先保持 OPPO 行为，后续再优化。

#### 坑 8：`file://` URI

Android 17 对 `file://` 暴露限制更严。
OPPO 用 `StrictMode.disableDeathOnFileUriExposure()` 临时绕过，但这只是避免 system_server 崩溃，不代表目标 App 能读。
建议直接跳过 `file://`，或转换为 `content://`。

---

## 5. 验证方法

### 5.1 OPPO 真机日志

```bash
adb logcat -s OplusGlobalDragAndDropCallback OplusGlobalDragAndDropRUSConfigManager
```

关注：

```text
createSendIntent targetpkg In list!
preReprotDropResult normalStart
preReprotDropResult  safelyStart
createSendIntent same window!!
createSendIntent targetPkg name is Null!
create intent is null!
GlobalDragShare disabled!
```

如果看到 `createSendIntent targetpkg In list!` 且随后 `preReprotDropResult normalStart`，说明回退被触发。

### 5.2 配置检查

```bash
adb shell getprop persist.sys.all_drop_to_share_enable
adb shell ls -l /data/oplus/os/config/sys_global_drag_share_config.xml
adb shell ls -l /system_ext/oplus/sys_global_drag_share_config.xml
```

### 5.3 测试矩阵

| 场景 | 预期 |
| :--- | :--- |
| 文本拖到 QQ 聊天窗口 | 打开 QQ 分享面板（发送给好友/发送到电脑） |
| 图片拖到 QQ 聊天窗口 | 打开 QQ 分享面板，图片作为 `EXTRA_STREAM` |
| 文本拖到微信聊天窗口 | 打开微信分享入口 |
| 图片拖到微信聊天窗口 | 打开微信分享入口 |
| 文本拖到桌面 | 不触发（denylist） |
| 图片拖到系统设置 | 不触发（denylist） |
| 在 QQ 内拖到 QQ 自己的输入框 | 不触发（同窗口/同包） |
| 拖到没有 ACTION_SEND 的 App | 不触发（resolveActivity null） |
| 多张图片拖到 QQ | `ACTION_SEND_MULTIPLE` + `EXTRA_STREAM` 列表 |
| 混合图片+文本拖到 QQ | 不触发（OPPO 放弃混合类型） |
| 拖到内容传送门面板空白处 | 不触发（排除自身包） |

### 5.4 移植后的日志建议

```text
HyperPortal/DragShareFallback: source=com.tencent.mm target=com.tencent.mobileqq
HyperPortal/DragShareFallback: mime=image/* count=1 action=SEND
HyperPortal/DragShareFallback: start chooser user=0
HyperPortal/DragShareFallback: skip same package
HyperPortal/DragShareFallback: skip denied target=com.android.systemui
HyperPortal/DragShareFallback: no ACTION_SEND activity
```

---

## 6. 对 ohmyAES 的建议

### 6.1 优先级

| 优先级 | 内容 | 理由 |
| :--- | :--- | :--- |
| P1 | 在 `PortalSystemModule` 实现 `reportDropResult` 回退 | 与 OPPO 行为最接近，能拿到窗口包名和 ClipData |
| P1 | denylist + 同包/同窗口过滤 | 避免桌面、SystemUI、自身面板误触发 |
| P2 | URI 权限与分身用户支持 | 图片分享到 QQ/微信的实际可用性 |
| P2 | 设置项“拖到未适配应用时打开其分享面板” | 给用户关闭入口 |
| P3 | 对 `ACTION_SEND_MULTIPLE` 的多图优化 | OPPO 现有行为较保守 |

### 6.2 不建议的方案

- **不要用 AccessibilityService 监听拖拽结束再找窗口。**
  Accessibility 拿不到 system_server 的 `DragState`，也无法可靠拿到最终 drop 的 `WindowState`；时延和误判都高。
- **不要在 HyperPortal App 里用 `UsageStatsManager` 猜前台包。**
  拖拽过程中前台包可能还是来源 App，不是落点 App。
- **不要改内容传送门面板的 `ACTION_DROP` 返回值来“顺便”触发分享。**
  面板只能知道自己是否被 drop，不知道底层窗口包名；而且会破坏现有的 fly-back 逻辑。

### 6.3 推荐架构

```text
system_server (PortalSystemModule)
  └─ hook DragDropController.reportDropResult(window, consumed)
       ├─ consumed == true  -> 什么都不做
       └─ consumed == false
            ├─ target = windowMap[window.asBinder()]
            ├─ data   = dragState.mData
            ├─ targetPkg = target.mAttrs.packageName
            ├─ filter: same window / same pkg / denylist / no ACTION_SEND
            ├─ build ACTION_SEND or ACTION_SEND_MULTIPLE
            ├─ start chooser as target user
            └─ param.args[1] = true  // skip fly-back
```

这样既保留了现有中转站面板的交互，又补上了 OPPO 的“窗口级分享回退”。

---

## 7. 一句话总结

OPPO 并不是“检测到 App 没适配拖拽”后去查一个适配表，而是：

> **目标窗口没有消费 `ACTION_DROP` → system_server 拿到该窗口的包名 → 构造通用 `ACTION_SEND` 并 `setPackage` 给这个包 → 交给目标 App 自己的分享入口。**

QQ 的“发送给好友 / 发送到电脑”不是 OPPO 写的，而是 QQ 的 `ACTION_SEND` 入口本来就长这样。

---

## 附录：关键类 / 方法 / 字符串索引

### OPPO system_server（`oplus-services.jar`）

| 名称 | 作用 |
| :--- | :--- |
| `com.android.server.wm.OplusGlobalDragAndDropCallback` | 全局拖拽分享回调 |
| `preReportDropResult(IWindow, boolean)` | 回退入口；在 WMS 锁之前 |
| `createSendIntent(WindowState, boolean)` | 判断目标窗口是否允许回退 |
| `createTempSendIntent(WindowState)` | 调用 `createSendIntentInner` |
| `createSendIntentInner(ClipData, Context)` | ClipData → ACTION_SEND |
| `analyzeClipDataItemMimeType(ClipData, int, Context)` | 单项 MIME/Extra 解析 |
| `startChooseActivity(Intent, ActivityOptions)` | `createChooser` + ATM 内部启动 |
| `ifNeedDisbaleStrictMode(Intent)` | `file://` 时临时关闭 StrictMode |
| `com.android.server.wm.OplusGlobalDragAndDropRUSConfigManager` | 总开关 / 白名单 / 黑名单 |
| `SUPPORT_ALL_DROP_TO_SHARE` | 默认全量回退开关 |
| `mUnSupportDropToShareList` | 默认只含 `com.oplus.padconnect` |
| `persist.sys.all_drop_to_share_enable` | 控制 `SUPPORT_ALL_DROP_TO_SHARE` 的系统属性 |
| `com.android.server.wm.OplusWindowManagerServiceEx.systemReady` | 构造并注册 callback |

### 内容传送门 APK（`com.oplus.contentportal`）

| 名称 | 作用 |
| :--- | :--- |
| `ContentPortalService.onBind` | 接收 `clip_data` / `drag_pkg_detail` / `surface_control` |
| `com.oplus.contentportal.drag.e`（`DragInfoModel`） | 缓存拖拽数据 |
| `d6.x0`（`SettlementView`） | 底部面板 / 目标列表 / 点击分发 |
| `d6.x0.Q0(int)` | 按 `jumpType` 分发目标点击 |
| `com.oplus.contentportal.utils.v`（`JumpUtils`） | `c()` 判类型，`a()` 构造 Intent，`i()` 系统分享 |
| `com.oplus.contentportal.utils.b0`（`ShareUtils`） | `shareToTransferDock`，存入侧边栏中转站 |
| `com.oplus.contentportal.data.ServiceInfo` | `pkgName` / `className` / `deepLink` / `action` / `extraMap` |
| `drag_pkg_detail` | 来源 App 信息，不是落点窗口包名 |

### 对照（HyperOS）

| 名称 | 作用 |
| :--- | :--- |
| `com.android.server.wm.DragDropController.reportDropResult` | 目标上报 drop 结果 |
| `com.android.server.wm.DragState.mData` | 当前拖拽 ClipData |
| `com.android.server.wm.DragState.mDropWindow` | 落点 WindowState |
| `com.android.server.wm.DragState.mDragWindow` | 起始 WindowState |
| `com.android.server.wm.DragState.mDragResult` | 拖拽结果 |
| `com.android.server.wm.DragDropController.notifyUnhandledDrop` | AOSP 未处理 drop 回调 |
