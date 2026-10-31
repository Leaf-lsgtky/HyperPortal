# 任务指令：基于 `classes2.dex` 静态织入方案改造 `com.miui.contentcatcher.apk`，移植 Oplus 内容提取层

## 1. 背景与核心目标

我们要在 MIUI / HyperOS 上将 Oplus（ColorOS Content Portal）的前台结构化内容提取能力（TextView、Jetpack Compose 语义树、WebView `elementFromPoint`、ImageView 位图提取）移植到小米传送门的上游抓取组件 **`com.miui.contentcatcher.apk`（AES）** 中。

### 为什么采用本方案（严格架构约束，请勿偏离）
1. **AES 的提取代码运行在目标应用进程**：系统 `framework.jar` 在每个应用的 `android.app.ActivityImpl.onCreate()` 中，会主动调用 `miui.contentcatcher.InterceptorProxy.create(activity)` → `InterceptorFactory.initInterceptorClass()` → `createPackageContext("com.miui.contentcatcher", 3)`（`CONTEXT_INCLUDE_CODE | CONTEXT_IGNORE_SECURITY`），将 `com.miui.contentcatcher.apk` 的 DEX 直接加载进**目标应用进程**（如微信、小红书、抖音、银行 App）。
2. **严禁使用目标应用级 Xposed / Zygisk+LSPlant / LSPatch**：
   - 在 LSPosed 中只勾选 `android` 或 `com.miui.contentcatcher` 无法进入目标应用进程；
   - 若勾选全部目标应用，或使用 Zygisk+LSPlant / LSPatch，会在目标应用进程内存中留下 `.so` 映射、`ArtMethod` 篡改等 Native Hook 痕迹，导致银行、支付及加固应用触发风控检测；
   - 此外，`createPackageContext(..., 3)` 只初始化 `PathClassLoader` 并反射加载 `com.miui.contentcatcher.Interceptor`，**不会执行 `AppComponentFactory` 或 `Application.onCreate()`**，因此 LSPatch 在目标应用进程内根本不会被触发。
3. **选定落地方案（纯静态字节码 + `classes2.dex` 塞包 + 核心破解覆盖安装）**：
   - 测试设备已在 `system_server`（`android` 作用域）开启核心破解（CorePatch）。
   - 直接对 `com.miui.contentcatcher.apk` 原有的 `classes.dex` 做**极简 Smali 插桩**（仅插入数行静态方法转发调用），将所有新增的 Oplus 风格提取逻辑放在 Android Studio 独立子模块中编写，编译为纯正 Java/Kotlin 字节码 **`classes2.dex`**，利用 Android `PathClassLoader` 原生 MultiDex 特性直接塞入 APK 根目录。
   - 通过 `adb install -r` 将修改后的 `com.miui.contentcatcher.apk` 覆盖安装到 `/data/app/` 下，系统会自动接管代码路径并生成匹配的 `oat/arm64/base.odex` 与 `.vdex`，无需手动删除系统分区的 `odex/vdex`，且在目标应用进程中 100% 为原生静态字节码（零 Hook 痕迹）。

---

## 2. 工程环境与可用资源

- **本地工作区路径**：`E:\workspace\DragShare`
- **参考分析文档**：请先阅读工程内或上下文中的《长按快捷操作_内容获取机制分析.md》（重点阅读第 4 节与第 14 节）。
- **静态反编译/MCP 核对对象**：
  - `com.miui.contentcatcher.apk`（基准分析版本：`4.1.9`，`versionCode 320260520`）
  - `framework.jar` / `miui-framework.jar`（确认 `InterceptorProxy` 与 `InterceptorFactory` 调用契约）
  - `oplus-framework.jar`（参考 `OplusTextViewDragProcessor`、`OplusComposeViewDragProcessor`、`OplusWebViewDragProcessor`、`OplusImageViewDragProcessor` 的完整实现细节）
  - `com.miui.contentextension.apk`（传送门消费端，确认 `TextContentExtensionService` 读取 `Bundle` 字段契约）

---

## 3. 执行阶段与详细技术要求

请按以下 4 个阶段逐步推进：先用反编译工具核对 `com.miui.contentcatcher.apk` 的精确字节码签名，再搭建子模块与提取逻辑，最后编写一键合成脚本并验证。

### 阶段一：静态核对 `com.miui.contentcatcher.apk` 的精确插桩点

在动手写代码前，先反编译检索当前 `com.miui.contentcatcher.apk`，确认以下三个关键位置的真实类名、方法签名与寄存器结构（注：`m433e`、`m443o`、`f5276q` 等为 `4.1.9` 版本的混淆名，请以方法签名和调用特征为准）：

1. **触摸坐标捕获与 View 命中遍历点（`com.miui.contentcatcher.Interceptor`）**：
   - **问题**：`com.miui.catcherpatch.ViewContentFetcher.fetchViewContent(View, int, boolean)` 的参数中**没有传入触点坐标 `(x, y)`**！但 Jetpack Compose 语义树命中（`boundsInRoot.contains(x, y)`）和 WebView DOM 提取（`document.elementFromPoint(x, y)`）**必须依赖用户长按时的屏幕/窗口坐标**。
   - **同时需要检查**：原版 `Interceptor.m433e(View, PointF, PointF, ArrayList, HashMap)` 采用子 View 逆序递归，取**第一个**非 `ViewGroup` 命中即返回。而 Jetpack Compose 的宿主容器 `androidx.compose.ui.platform.AndroidComposeView` 以及 `android.webkit.WebView` **本身都继承自 `ViewGroup`**！
   - **核对任务**：
     - 检查 `Interceptor.dispatchTouchEvent(...)` 和 `m433e(...)` 是如何处理 `AndroidComposeView` 和 `WebView` 的，确认当内部没有普通子 View 时，`AndroidComposeView` 或 `WebView` 自身能否被作为目标 View 传给 `fetchViewContent(...)`。如果原版逻辑因为它们是 `ViewGroup` 而跳过了它们，需在 `m433e`（或 `Interceptor` 的命中入口）处增加钩子：当遇到 `AndroidComposeView` 或 `WebView` 时，允许将其作为命中 View 候选。
     - 确认在 `Interceptor` 中记录当前长按触点坐标 `PointF(rawX, rawY)` / `(windowX, windowY)` 的最佳插桩位置，以便通过一行 `invoke-static` 将触点坐标存入 `classes2.dex` 的上下文管理类（如 `TouchCoordinateHolder.record(MotionEvent / PointF)`）。

2. **页面配置与 `catchType` 放行点（`Interceptor.m443o()` 或 `ViewContentFetcher` 调用点）**：
   - 原版 AES 会向 `ContentCatcherService.getPageConfig(Token)` 查询页面配置，决定当前 Activity 是否启用抓取及 `catchType` flags（`1` = 文字，`2` = 图片）。
   - **核对任务**：确认当目标页面不在官方白名单或 `catchType` 未开启时，`Interceptor` 是否会直接跳过长按触发。如果是，请在 `Interceptor` 读取配置处插桩，确保默认开启文字与图片抓取标志（`catchType |= 3`），除非命中明确的系统级排除包。

3. **内容提取收口点（`com.miui.catcherpatch.ViewContentFetcher`）**：
   - 核对 `fetchViewContent(Landroid/view/View;IZ)Landroid/os/Bundle;` 的完整实现，以及它返回的 `Bundle` 键值契约（如 `"viewContent"`、`"viewBitmap"`、`"catchType"` 等）是如何被打包进 `Content` 并传给 `ContentCatcherManager.onContentCatched(...)` 的。
   - 确认 WebView 异步提取的回传路径：如果 WebView `evaluateJavascript` 是异步回调，能否在长按按下/判定阶段提前预取缓存，或者在异步回调拿到结果后直接复用 `Interceptor` 持有的引用触发 `ContentCatcherManager.onContentCatched(...)`。

---

### 阶段二：在 `DragShare` 工程中新建 `:catcher-patch` 纯代码子模块

在 `E:\workspace\DragShare` 中创建一个独立的 Android Library 或 Java/Kotlin 模块 `:catcher-patch`（**严禁引入 `de.robv.android.xposed`、LSPlant 或任何 Native `.so` 依赖**，只使用 Android SDK 标准 API 与反射，确保编译出的 `classes2.dex` 100% 纯净）。

模块内需实现核心入口类（例如 `dev.dragshare.catcher.OplusStyleExtractor`）及以下四大提取器（严格对标 `oplus-framework.jar` 的策略）：

#### 1. 输出契约与兜底原则
- 入口方法示例：
  ```java
  public static Bundle enhanceFetchViewContent(View view, int flags, boolean isText, Bundle origResult)
  ```
- **安全兜底**：所有提取逻辑必须包裹在 `try-catch (Throwable)` 中，任何反射失败或异常均安静回退到 `origResult`，绝不能引起宿主 App 崩溃。
- **契约映射**：
  - 提取到有效文本时：构建或更新 `Bundle`，写入 `bundle.putString("viewContent", text)`，并设置对应的 `catchType`（文字=`1`）。
  - 提取到有效图片 `Bitmap` 时：写入 `bundle.putParcelable("viewBitmap", bitmap)`，并设置对应的 `catchType`（图片=`2`）。
- **提取优先级**：按 `Compose -> WebView -> TextView -> ImageView`（或根据实际命中的 View 树层级）执行。

#### 2. Jetpack Compose 提取器（参考 `OplusComposeViewDragProcessor`）
当命中 View 或其祖先节点为 `androidx.compose.ui.platform.AndroidComposeView` 时，必须在 **UI 线程** 执行以下多级降级提取：
1. **坐标换算**：将记录的屏幕/窗口触点坐标换算为 `AndroidComposeView` 内部本地坐标 `(touchX, touchY)`。
2. **第一级：`SemanticsOwner` 语义树精准命中**：
   - 通过反射读取 `AndroidComposeView` 的 `getSemanticsOwner()` → `getUnmergedRootSemanticsNode()`（或 `getRootSemanticsNode()`）。
   - 递归遍历子树，筛选 `boundsInRoot.contains(touchX, touchY)` 的节点（跳过明确标记为 link 且无独立语义的装饰节点）。
   - **候选节点择优排序**（严格对齐 Oplus 规则）：
     1. 含有文本（`SemanticsProperties.Text` / `EditableText` / `ContentDescription`）的节点优先于无文本节点；
     2. 图像角色（`Role.Image` 或含图像语义）节点优先于普通非文本节点；
     3. 非容器节点且层级深度 `depth > 0` 优先；
     4. 深度 `depth` 更大的节点优先；
     5. 深度相同时，包围盒面积 `(width * height)` 更小的节点优先。
   - 若最优节点含文本，拼接其文本列表并以 `"viewContent"` 返回；若为图像节点且边界有效，对 `AndroidComposeView` 执行 `view.draw(canvas)` 生成位图，并按该节点的 `boundsInRoot` 裁剪出局部 `Bitmap`，以 `"viewBitmap"` 返回。
3. **第二级：同语义行补救与 `AccessibilityNodeProvider` 虚拟节点探测**：
   - 若最优节点无直接内容，检查同一语义行（Y 轴范围重叠）内的嵌入 `AndroidView`（如内嵌 `TextView`）或同行 Semantics 子树文本。
   - 调用 `composeView.getAccessibilityNodeProvider()`：先尝试通过反射读取 provider 内部缓存虚拟节点的 `Map` / `SparseArray`；若字段混淆或读取失败，通过 `createAccessibilityNodeInfo(virtualViewId)` 线性探测或从根节点的 `getChildId` 递归遍历虚拟节点树（若做线性探测，范围控制在 `0..512` 并在连续空结果达到阈值时提前终止）。
4. **第三级：无障碍节点命中与小区域裁图回退（`FALLBACK_CROP`）**：
   - 对收集到的 `AccessibilityNodeInfo` 按触点坐标过滤，并按“有文本 -> 深度更大 -> 面积更小”排序；文本优先取 `node.getText()`，非图像节点才回退 `node.getContentDescription()`。
   - 若命中图像类节点（类名含 `Image`、`ImageView`、`AsyncImage`）或非根节点的普通无文本节点（边界有效且面积 `< 85%` Compose View 总面积），按节点边界从 `view.draw(canvas)` 位图中裁剪返回 `viewBitmap`。
   - 若触点位于非图像的文本行内但未提取到文本，直接放弃截图回退（避免把整页背景误当成图片）。

#### 3. TextView 与原生视图提取器（参考 `OplusTextViewDragProcessor`）
- 基础文本优先取 `((TextView) view).getText()` 的完整 `CharSequence.toString()`（不受 `maxLines` 或省略号截断影响，直接拿内存中持有的完整字符串）。
- 当 `getText()` 为空时，回退读取 `view.getContentDescription()`。
- 针对微信（`com.tencent.mm`）做清理：剥离形如 `wxid_xxx:\n`、`gh_xxx:\n` 等群聊发送者前缀。
- 若原版 `origResult` 已通过 `mViewClassReflectionMap` 反射拿到了更长或更完整的文本，保留原版有效结果。

#### 4. WebView DOM 提取器（参考 `OplusWebViewDragProcessor`）
- 支持标准 `android.webkit.WebView` 以及常见定制 WebView（如腾讯 X5 等，通过反射查找名为 `evaluateJavascript` 的双参数方法 `(String, ValueCallback)`）。
- 计算 CSS 视口坐标：
  ```java
  float density = view.getResources().getDisplayMetrics().density;
  int[] loc = new int[2];
  view.getLocationInWindow(loc);
  float cssX = (windowTouchX - loc[0]) / density;
  float cssY = (windowTouchY - loc[1]) / density;
  ```
- 注入 JS 脚本执行 `document.elementFromPoint(cssX, cssY)`，提取命中元素的 `(el.innerText || el.textContent || '').trim()` 以及图片元素的 `el.src`（或从 `el.outerHTML` / `getComputedStyle(el).backgroundImage` 中正则提取 `url(...)`）。
- **同步/异步衔接设计**：
  - 方案 1（预取缓存）：在 `Interceptor.dispatchTouchEvent` 收到 `ACTION_DOWN`（或长按判定触发前）检测到触点下有 WebView 时，立即在 UI 线程发起 `evaluateJavascript`，将返回的 JSON 结果缓存在内存中（带时间戳和坐标校验）；几百毫秒后长按触发 `fetchViewContent` 时即可**同步直接读取**缓存的文本！
  - 方案 2（异步补发）：若长按触发时 JS 尚未返回，在 `ValueCallback` 回调拿到文本后，直接调用原版 `Interceptor` 的上报方法将 `Content` 发送给 `ContentCatcherManager.onContentCatched(...)`。
  - **推荐采用“方案 1（`ACTION_DOWN` 预取缓存）为主 + 方案 2 为辅”**，这样完全不破坏原版 `fetchViewContent` 的同步返回模型！

#### 5. ImageView / Drawable 提取器（参考 `OplusImageViewDragProcessor`）
- 若 `view instanceof ImageView` 且 `getDrawable() instanceof BitmapDrawable`，直接取其 `getBitmap()`。
- 否则若存在有效 `Drawable`，或属于自定义图片控件（宽高 `>= 48dp`），通过 `Bitmap.createBitmap` + `drawable.draw(canvas)` 或 `view.draw(canvas)` 渲染位图写入 `viewBitmap`。

---

### 阶段三：一次性 Smali 插桩与自动化打包脚本

请编写一套可重复执行的构建脚本（Gradle Task、Python 或 PowerShell 脚本，放在 `scripts/` 目录下），完成以下自动化流水线：

1. **原版 `ContentCatcher.apk` 基底准备**：
   - 解包原版 `com.miui.contentcatcher.apk` 的 `classes.dex`。
   - 对 `Interceptor` 和 `ViewContentFetcher` 进行最小化 Smali 插桩（只添加静态转发调用，不塞复杂逻辑），重新编译为基底 `classes.dex`。
   - （可选但推荐）修改 `AndroidManifest.xml` 将 `versionCode` 设为 `999999999`（或高于当前系统版本的数值），防止日后系统 OTA 升级 `/system_ext` 分区时覆盖掉 `/data/app/` 下的修改版。
2. **`classes2.dex` 编译与秒级塞包**：
   - 运行 `:catcher-patch` 模块的编译任务，获取生成的 DEX 文件并重命名为 `classes2.dex`（若原 APK 已有 `classes2.dex`，则顺延命名为 `classes3.dex`）。
   - 直接将 `classes2.dex` 更新进基底 APK 根目录（无需每次用 `apktool` 全量解包回编译资源）。
   - 执行 `zipalign -p -f -v 4` 对齐，并使用 `apksigner` 签名（配合 `system_server` 侧 CorePatch 核心破解，可直接覆盖安装）。
3. **输出产物与调试命令**：
   - 脚本最终输出可直接安装的 `ContentCatcher-patched-signed.apk`。
   - 提供一键安装与验证命令：
     ```bash
     adb install -r -d ContentCatcher-patched-signed.apk
     adb shell am force-stop <测试目标应用包名，如 com.xingin.xhs>
     adb logcat -s DragShareCatcher:D Interceptor_Catcher:D
     ```

---

### 阶段四：自检清单（交付前必须逐项核实）

- [ ] `:catcher-patch` 生成的 `classes2.dex` 中**不包含**任何 Xposed、LSPlant 或 Native SO 调用，所有反射均有 `try-catch (Throwable)` 保护。
- [ ] `Interceptor` 的触点坐标已准确传递到 `classes2.dex`，且 `AndroidComposeView` 与 `WebView` 不会因为继承自 `ViewGroup` 而被原版 `m433e()` 递归逻辑丢弃。
- [ ] WebView 的 `elementFromPoint(x, y)` 采用了 `ACTION_DOWN` 预取缓存（或异步回调上报），不会在同步方法里死锁 UI 线程。
- [ ] 保持 `com.miui.contentcatcher` 的包名、`sharedUserId`、Service 声明及对外 AIDL / Binder 接口 100% 与原版一致。
- [ ] 同步更新《长按快捷操作_内容获取机制分析.md》第 14.7 / 14.8 节的状态记录，如实记录本次 `ContentCatcher` 静态织入方案的实现细节与实际构建产物路径。
