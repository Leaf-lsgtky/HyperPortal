# OhMyAES DragShare PoC: 全局项目现状与交接文档 (Handover Document)

> 本文档为项目完整上下文与技术交接手册，涵盖项目目标、全目录文件作用、已验证进展、关键反混淆映射表、已解决问题根因，以及当前 Jetpack Compose 待攻坚环节与下一步排查指引。后续接手模型/工程师可直接依据本文档无缝继续。

---

## 一、 项目背景与核心目标

### 1.1 项目目标
将 **OPPO ColorOS 传送门 / 全局长按拖拽（ViewDrag）** 的核心内容提取能力（针对 Telegram、Jetpack Compose、WebView、复杂卡片布局等无原生辅助支持的场景），移植并注入到 **小米 MIUI / HyperOS** 的系统级内容抓取服务 **`com.miui.contentcatcher`（Application Extension Service, 简称 AES 5.1.7）** 中。

### 1.2 注入方式与环境
* **免 Xposed / 免系统只读分区修改**：直接重打包 `com.miui.contentcatcher` APK，通过 classes.dex / classes2.dex 双 dex 注入，修改 `versionCode` 实现 `adb install -r -d` 覆盖安装。
* **设备环境**：已连接真机 `adb-67108759-zgOnHt._adb-tls-connect._tcp`，具备 KernelSU root 权限 (`su -c id` -> `uid=0`)。
* **当前构建版本**：`versionCode=1000000037`（构建自 `v37`，`ContentCatcher-poc-v37.apk`，已签名并对齐）。

---

## 二、 仓库目录与核心文件职责表

```
e:\workspace\ohmyAES\
├── xiaomi\
│   └── Application Extension Service_5.1.7.apk  # 小米官方原版 AES 5.1.7 安装包 (底包)
├── oppo\
│   ├── oplus-framework.jar                     # ColorOS 官方 framework 逆向参考库
│   └── oplus-services.jar                      # ColorOS 官方 system_server 逆向参考库
├── poc\
│   ├── src\dev\dragshare\catcher\
│   │   └── Bridge.java                         # 【核心注入代码】编译为 classes2.dex 的 Java 桥接类
│   ├── work\
│   │   ├── decoded\                            # apktool 反编译官方 APK 产生的 smali 目录
│   │   │   └── smali\com\miui\contentcatcher\
│   │   │       └── Interceptor.smali           # 【核心字节码插桩】拦截触摸、长按与 ViewGroup 检查
│   │   ├── zhplus.apk                          # 测试机导出的纯 Compose 测试应用 APK (知乎轻量版)
│   │   └── screen.png                          # 真机实时排查截图
│   ├── scripts\
│   │   ├── build_v22.py                        # 【主构建脚本】一键编译 Bridge -> d8 -> apktool -> 重包 -> 对齐签名
│   │   ├── repack_apk.py                       # 【核心打包脚本】无损注入 dex 并二进制修改 AndroidManifest versionCode
│   │   ├── log_listener.py                     # 【后台监听服务】实时过滤 DragSharePoC/ContentCatcher 日志 (UTF-8)
│   │   └── patch_v*.py                         # 针对 Bridge.java 的自动化差分增量补丁脚本
│   ├── signing\
│   │   └── poc.jks                             # 测试签名证书 (alias: poc, pass: pocpass)
│   └── build\
│       ├── apk\ContentCatcher-poc-v22.apk       # 当前最新已签名的 PoC APK
│       └── live_test.log                       # 后台实时测试日志持久化文件
```

### 详细文件功能说明：

#### 1. `poc/src/dev/dragshare/catcher/Bridge.java`
* **宿主**：由 `Interceptor.smali` 调用，运行在**被抓取的目标应用主进程**（如 Telegram、知乎轻量版、酷安等）中。
* **核心模块**：
  1. **自研独立长按触发器 (`ping`)**：
     * 监听全局 `dispatchTouchEvent`。
     * 采用 **400ms 定时器** + **100px 防微颤滑动手势容差（Touch Slop）**。
     * 采用 `STATE_IDLE` -> `STATE_TRIGGERED` -> `STATE_DRAGGING` / `STATE_PROCESSED` 四态状态机，**彻底防止 120Hz 重入死锁卡顿**。
  2. **Telegram 专有内容提取器 (`extractTelegramContent`)**：
     * 反射探测 `ChatMessageCell` / `BackupImageView`。
     * 提取消息文本 `currentMessageObject.messageText` 与图片 `ImageReceiver.getBitmapSafe()`。
     * **杜绝抓取透明遮罩**：彻底拦截 `BottomSheetTabsOverlay` 等全屏背景，防止整屏截图。
  3. **Jetpack Compose 专有内容提取器 (`extractCompose`)**：
     * 适配 R8/ProGuard 强混淆的 Compose 语义树（Semantics）。
     * 遍历并解析 Compose 1.5+ 的 `ScatterMap`（`b: Object[] keys`, `c: Object[] values`）。
     * 提取文本 `AnnotatedString`，支持本地坐标（Root）与窗口全局坐标双向对齐判定。
  4. **WebView DOM 抓取器 (`webViewBundle`)**：
     * 异步注入 `document.elementFromPoint` 执行 JavaScript 提取 `innerText` 和 `img.src`。
  5. **普通原生控件抓取器 (`extractTextView`, `extractImageBitmap`)**：
     * 包含微信发送者前缀剥离、抖音/小红书/微博/B站/淘宝缩略图黑名单过滤、5层父级-兄弟节点卡片探测。
  6. **传送门 IPC 调度器 (`forwardToPortal`)**：
     * 组装 Intent 将抓取到的数据投递给 MIUI 传送门服务。
     * **Binder 溢出保护**：自动将图片预览下采样至 360x360，彻底根治 Android 2MB Parcel Blob 崩溃。

#### 2. `poc/work/decoded/smali/com/miui/contentcatcher/Interceptor.smali`
* **字节码插桩点**：
  * **触摸事件旁路**：在 `dispatchTouchEvent` 最开始插入 `invoke-static {p1, p0, ...}, Ldev/dragshare/catcher/Bridge;->ping(...)V`。
  * **ViewGroup 检查绕过**：在 `shouldHandleAsViewGroup`（smali 行 864 & 1432）插入对 `Bridge.shouldHandleAsViewGroup(View)` 的调用。让 ComposeView 与 WebView 在系统 AES 逻辑中被视作叶子节点，触发后续高级提取。

#### 3. `poc/scripts/repack_apk.py`
* 避免反编译重打包 resources.arsc 损坏 MIUI 私有资源 ID。
* 以原始官方 APK 为基础，直接替换/增加 `classes.dex` 和 `classes2.dex`。
* 并在二进制字节流层精准修改 `AndroidManifest.xml` 中的 `versionCode`。

---

## 三、 已攻克的技术成果与状态

| 应用场景 | 测试包名 | 当前状态 | 验证详情 |
| :--- | :--- | :--- | :--- |
| **Telegram 文本** | `com.exteraless.app` | **已完成 (100%)** | 正常长按文字直接唤起传送门，提取字符准确无误。 |
| **Telegram 图片** | `com.exteraless.app` | **已完成 (100%)** | 准确提取原图 Bitmap，不再误抓全屏背景遮罩，Binder IPC 无崩溃。 |
| **微博查看大图** | `com.sina.weibo` | **v22 修复，待真机验证** | `SplitDraggableImageViewV4/LargeImageView2` 走 Bitmap 字段 + 原图 Uri/PFD 提取。 |
| **UU 远程** | `com.netease.uuremote` | **已完成 (100%)** | 彻底压制 StreamerSurfaceView 误报的“图片”假标签。 |
| **酷安卡片** | `com.coolapk.market` | **已完成 (90%)** | 5 层兄弟节点/父节点向上探索成功识别卡片文字与配图。 |
| **纯 Compose 应用** | `com.github.zly2006.zhplus.lite` | **核心贯通 (70%)** | **成功突破混淆并在第 4 点提取出 127 字符长文本拖入传送门！** 目前仅剩区域遍历完整性问题。 |
| **长按性能响应** | 全局 | **v22 优化完成，待真机验证** | 去除重复全树扫描、按类缓存反射结果、AES 结果复用；目标恢复到 15~25ms 量级。 |
| **ColorOS 拖拽阴影与底部重排** | 全局 | **v37 完美对齐 ColorOS 规范** | 还原 12.27dp 字号、80% 宽高自适应、无截断、5 级图片面积缩放及底部 160dp 热区重排联动。 |

---

## 四、 关键技术突破与混淆逆向映射字典

针对 Jetpack Compose 在 Release APK 下经过 R8 深度混淆的结构，已在 `Bridge.java` 中建立完整的反射穿透体系：

### 4.1 Compose 混淆类名与结构对照表（以 Zhplus 1.5+ 为例）

| Compose 原生概念 | 混淆后类名 | 关键字段/方法 | 作用与提取机制 |
| :--- | :--- | :--- | :--- |
| `AndroidComposeView` | `lb` | 实现了 `View`，含 `getSemanticsOwner` | Compose 宿主根 View |
| `SemanticsOwner` | `so5` | `so5.a() -> po5` | 获取非合并根语义节点 `unmergedRootSemanticsNode` |
| `SemanticsNode` | `po5` | `po5.h() -> j95`<br>`po5.g() -> j95`<br>`po5.k() -> jo5`<br>`po5.i(ZZ) -> List<po5>` | 语义节点：<br>`h()`: `boundsInRoot`（根坐标四边形）<br>`g()`: `boundsInWindow`（屏幕窗口坐标）<br>`k()`: 获取配置 `jo5`<br>`i(ZZ)`: 获取子节点列表 |
| `SemanticsConfiguration` | `jo5` | 实现了 `Iterable`<br>字段 `e: c14` (ScatterMap) | 语义配置表。由于不走标准 Java Map，直接迭代或通过 `c14` 提取 |
| `ScatterMap` (AndroidX) | `c14` | 字段 `b: Object[]`<br>字段 `c: Object[]` | 高性能集合：`b` 数组为所有 Keys，`c` 数组为所有 Values |
| `SemanticsPropertyKey` | `yo5` | 字段 `a: String` | 键名对象。混淆后无 `getName()`，但**字段 `a` 保留了真实字符串**（`"Text"`, `"EditableText"`, `"ContentDescription"`, `"Role"`） |
| 四边形坐标盒子 (`Rect`) | `j95` | `a: float` (left)<br>`b: float` (top)<br>`c: float` (right)<br>`d: float` (bottom) | 语义节点包围盒 |

---

## 五、 已解决的历史痛点与根因剖析

### 1. 长按必卡死 / 掉帧（v19 首次修复，v22 继续优化）
* **根因**：原先如果长按没有抓到内容，状态被写回 `STATE_IDLE`。用户手指在屏幕上持续按住时，微小的位移产生 120Hz 高频 `ACTION_MOVE`，每个 MOVE 都认为“已按住满 400ms”，导致 1 秒内执行 50~100 次全树递归反射与语义树遍历，将主线程完全锁死。
* **修复**：增加 `STATE_PROCESSED` 终态，单次手势按下仅允许定时器触发一次；移除了 DOWN 事件的同步全树探测。

### 2. Compose 导致 `Software rendering doesn't support hardware bitmaps` 崩溃
* **根因**：Compose 的底层渲染使用了 Hardware Bitmap 与 RenderNode。此前代码在未提取到文本时，有一段猜测“宽高比在 0.5~2.0 的空布局为图片”的启发式逻辑，命中后调用 `composeView.draw(new Canvas(softwareBitmap))`，直接触发系统硬件位图在软件画布绘制的致命异常。
* **修复**：彻底删除“空布局按宽高比猜图片”的错误逻辑；Compose 位图绘制加入硬件隔离保护，优先提取文本。

### 3. 按出“无障碍语义” `AccessibilityAction(label=null, action=...)`
* **根因**：Compose 的动作属性 `SemanticsActions.SetText` 键名中包含 `"Text"`，旧逻辑用 `keyName.contains("Text")` 误把输入法动作闭包当成文本提取了出来。
* **修复**：引入 `isSemanticsTextKey()` 精准限定为 `Text`、`EditableText`，全面拦截并清洗 `AccessibilityAction`。

---

## 六、 当前仅存核心问题与排查指引（供接手 AI 重点突破）

### 6.1 当前表现
在知乎轻量版（`com.github.zly2006.zhplus.lite`）的文章页面（参见 `poc/work/screen.png`）：
* 长按 **“4. 享界 G9...”** 这段文字：**可以 100% 成功提取出 127 字符并拖出传送门！**
* 长按 **“3. 赛力斯...”**、**“5. 然后大的来了...”**、**“6. 这两天人民日报...”**：**触发判定落空，无法提取。**

### 6.2 关键排查线索与分析
1. **视觉差异分析（重要线索）**：
   * 观察 `poc/work/screen.png`，只有 **“4. 享界 G9...”** 和 **“6.”** 下方带有**浅色虚线下划线（Dashed Underline）**，而“3.”和“5.”没有！
   * 这表明知乎该文章的段落并非同质控件：带下划线的可能是一个独立的 `SelectionContainer`、富文本链接块或特殊的 `MarkdownBlock`，拥有独立的未合并语义节点；而不带下划线的部分可能被父级合并（Merged）或者挂在不同的节点链上。
2. **`compose candidates count=8` 线索**：
   * 日志显示，不管按在第 3 点还是第 4 点，递归到的候选节点数始终是固定值 `8`！
   * 这说明 `rootNode` 并没有把 LazyColumn 中所有的可见段落都扁平展开，或者我们的子节点递归函数 `getComposeChildren(node)` 只遍历到了部分分支！
3. **`po5.i(ZZ)` 的参数含义**：
   * 在反编译 `po5` 时，获取子节点的方法是：
     ```smali
     name: 'i'
     type: '(ZZ)Ljava/util/List;'
     ```
   * 在 Compose 源码中，`SemanticsNode.getChildren()` 有两个参数：
     `fun getChildren(includeSelfIfMerged: Boolean, mergeDescendants: Boolean): List<SemanticsNode>`
   * 当前 `Bridge.java` 的反射调用可能传入了 `(false, false)`。如果传入 `(false, false)`，对于合并了后代的节点，其子节点可能直接返回空列表！
   * **下一步测试建议**：尝试对比 `po5.i(true, false)`、`po5.i(true, true)` 或遍历 `po5.r(ArrayList, boolean)`，打印输出整棵语义树的所有文本节点分布，定位第 3、5 点究竟挂在哪个子节点下。

### 6.3 v22 已处理的“长按卡顿 + 简单页面坏掉”根因（本轮重点）

#### 6.3.1 长按卡顿的真正来源：同一次长按跑了两遍全树

旧版 `ViewContentFetcher.smali` 的 `fetchViewContent(View,int,boolean)` 实际上**没有调用** `Bridge.preExtract`，而是：

```
fetchViewContentImpl()  // AES 原生全树遍历，约 100~150ms
  -> enhanceFetchViewContent() // 再跑一遍 Bridge 的 5 层父/兄弟 + Accessibility 遍历
```

因此一次长按中出现两条提取链路。日志里 `longPress first` 到 `enhance hit` 约 149ms，之后 `customLongPress` 再花约 137ms，就是这两遍。

v22 修复：
* `ViewContentFetcher.fetchViewContent` 先调用 `Bridge.preExtract`；命中直接返回，不再跑原生 `fetchViewContentImpl`。
* AES 已先生成 `origin` 时，`enhanceFetchViewContent` 直接采用 `origin`，不再二次提取。
* `onCustomLongPressTriggered` 优先复用 `deliverContent` 已放入 `sPendingPayload` 的结果，不再二次全树扫描。

#### 6.3.2 长按卡顿的放大器：每片叶子都做一次 `resolveAndroidComposeView`

旧版 `extractContentColorOS` 第一步对每个传入 View 都执行：
```java
View composeHost = isComposeView(view) ? view : resolveAndroidComposeView(view);
```
`resolveAndroidComposeView` 会递归扫完整棵子树找 `AndroidComposeView`。AES 的 `Interceptor.e()` 会遍历窗口中的每个叶子并调用 `fetchViewContent`，于是每个叶子都触发一次全子树扫描，单个叶子 30~50ms，长按直接卡住。

v22 修复：
* 删除 `extractContentColorOS` 中的全局 `resolveAndroidComposeView` 扫描；只有 `isComposeView(view)` 或 AES 已将 ComposeView 当作叶子传入时才走 Compose 提取。
* 增加 `viewKind()` 按 `Class` 缓存 Compose/WebView/Telegram 判定，避免每个 View 反复 `getDeclaredMethods()`。
* `preExtract` / `enhanceFetchViewContent` 增加 `isPlausibleContentHost()` 与触摸点命中检查，非候选叶子在微秒级返回。
* Accessibility 全树遍历只对 `Text`/`Label`/`Title`/`Caption` 类 View 执行。

#### 6.3.3 微博查看大图页为什么会“坏”

微博 `MediaPreviewActivity` 的图片控件是：

```
com.sina.weibo.photoalbum.imageviewer.view.SplitDraggableImageViewV4
  -> com.sina.weibo.view.SplitTouchTagImageViewV4
  -> com.sina.weibo.view.largeimageview.LargeImageView2
  -> androidx.appcompat.widget.AppCompatImageView
```

`LargeImageView2` 虽然最终是 `ImageView`，但它**不使用 `getDrawable()`**，而是把图片放在私有字段 `E:Bitmap` / `F:Bitmap`，并提供 `q()` / `z()` 返回 Bitmap；原图 Uri 在 `z:Uri`，PFD 在 `y:ParcelFileDescriptor`。旧版 `extractImageBitmap()` 看到 `getDrawable()==null` 就直接 `return null`，所以这类“自绘 ImageView”全部 miss。旧版 Weibo 黑名单还包含 `preview_pic_iv`，会把大图页进一步排除。

v22 修复：
* `extractCustomBitmap()` 按类型扫描自定义 View 的 `Bitmap`、`Bitmap[]`、`Drawable` 字段与零参 getter，适配 `LargeImageView2` 与 `MblogItemPicView`。
* `extractCustomText()` 支持 `LayoutTextView` 这类“自己画文字”的 View（读取 `CharSequence` 字段 / `h()` 之类的 getter），不再依赖 Accessibility 兜底。
* `findTypedFileValue()` 支持混淆字段名下的 `Uri` / `File` / `ParcelFileDescriptor`；微博大图页优先返回原始文件 PFD（保留原图），同时附带 720px 预览图供拖拽阴影使用。
* 全屏大图不再被 `preview_pic_iv` 等缩略图黑名单拦截（`isLargeImageOnScreen`）。

#### 6.3.4 仍未解决 / 需要真机验证的点

* **Compose 语义树区域遍历**：按用户要求本轮先放一放，`extractCompose` 逻辑未改动。
* **AES 服务进程**：日志中持续出现
  `E/ContentCatcherManager: failed to get ContentCatcherService.`
  说明 `com.miui.contentcatcher` 的 `StartContentCatcherService` 没有注册到 `ServiceManager`。这会让 `Bridge.getPageConfig()` 走合成配置（只有 `pick_mode` / `image_pick_mode`）。如果安装 v22 后仍有个别页面异常，先重启一次手机让系统进程重新注册服务，再复测。
* **`preview_pic_iv` 黑名单**：已从 Weibo 规则移除；若发现信息流缩略图被误抓，可在 `isInvalidViewId()` 中按尺寸/资源名重新收紧，但不要影响全屏大图。
* **ColorOS 拖拽阴影与底部重排规范分析**：已完成 DexSQL 逆向反编译，详见专题文档 [COLOROS_VIEWDRAG_SPEC_ANALYSIS.md](file:///e:/workspace/ohmyAES/docs/COLOROS_VIEWDRAG_SPEC_ANALYSIS.md)。前序版本把底部流转卡片（76x76、40字、11dp）混淆为系统拖拽阴影，已查清真实 12.27dp、80% 宽高自适应及 5 级图片缩放公式。

---

## 七、 常用维护与构建命令速查

```powershell
# 1. 编译并部署最新 APK (以 v22 为例，版本号在 build_v22.py 中)
python poc/scripts/build_v22.py
adb install -r -d poc/build/apk/ContentCatcher-poc-v22.apk

# 2. 杀死测试应用使其重新加载 Bridge (classes2.dex)
adb shell "am force-stop com.github.zly2006.zhplus.lite"
adb shell "am force-stop com.exteraless.app"
adb shell "am force-stop com.sina.weibo"

# 3. 实时查看日志 (推荐直接读取后台生成的 live_test.log)
Get-Content "poc/build/live_test.log" -Tail 30 -Wait

# 4. 手动截屏排查界面布局
adb shell "screencap -p /data/local/tmp/screen.png"
adb pull /data/local/tmp/screen.png poc/work/screen.png
```

---

## 八、 严格约束与行为准则（Commitments）

1. **绝对禁止全量兜底截屏**（用户明确约束：`"不要全量兜底截屏"`）。
2. **测试时必须挂载后台日志监听**（用户要求：`"以后遇到要操作的你就挂一个日志监听让我操作"`）。
3. 所有文件写入与编码统一使用 **UTF-8**。
