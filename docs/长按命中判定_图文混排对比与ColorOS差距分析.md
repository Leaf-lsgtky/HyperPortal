# 长按命中判定对比：HyperPortal vs ColorOS（图文混排场景）

> 现象：上面一段文字、下面一张图的卡片（微博正文+配图、小红书笔记、知乎回答、公众号文章……），
> 长按**图片**时经常抓到**文字**。ColorOS 上同样的操作永远是图片。
>
> 本文对比两侧从"手指按下"到"决定抽哪个内容"的完整判定链，定位差异根因。
> 结论是：**这不是参数没调好，而是判定模型不同** —— ColorOS 是"先定视图、再定模态"的一刀切模型，
> 我们是"多级兜底级联、文字优先"的瀑布模型；一旦图片层因为任何原因失手，瀑布必然落到文本层。
>
> 证据来源：
> - 我方：`aes/src/dev/dragshare/catcher/Bridge.java`（开发版，行号即当前 HEAD）
>   + `aes/smali_project/smali/com/miui/contentcatcher/Interceptor.smali` + `smali/v/c.smali`
> - ColorOS：`oppo/oplus-framework.jar` → `android.view.viewdrag.*`（ColorOS 17 传送门拖拽框架）
> - 静态分析，未在设备上跑（ADB 已断开）。所有结论均标注了可复查的方法/行号。

---

## 0. 摘要（TL;DR）

| # | 差异 | ColorOS | HyperPortal（现状） | 后果 |
|---|---|---|---|---|
| 1 | **谁来当 target** | 先解出**唯一一个**视图，再抽 | 逐层 `preExtract`，谁先吐内容听谁的 | 决定性差异 |
| 2 | **模态从哪来** | 来自**这条视图的规则**（`type=text/image/file`） | 来自**级联顺序**（文本步骤排在图片前） | 决定性差异 |
| 3 | **容器会不会聚合文本** | ❌ 没有这个逻辑（文本只取自命中的那一个 TextView） | ✅ `extractViewGroupText()` 把整格所有 TextView 拼成一段 | 图文卡片必错 |
| 4 | **候选集** | 真实 dispatch 链上的叶子视图 + 最小的可点击容器 | 递归 walk 命中的叶子 → 任意祖先 | 祖先可被选中 |
| 5 | **遮挡/层级校验** | 有（覆盖检测、`Z` 序、`isSkipDraw`、同深度豁免、父裁剪） | 无 | 选到被盖住的节点也无所谓——因为根本不去校验 |
| 6 | **图片合格线** | 只有一条：`ImageView.getDrawable() == null` 才否决 | 60px 门槛 + **类名含 "Layout" 直接否决** + 各家黑名单 | 九宫格/自定义图容器被误杀 |
| 7 | **坐标系容差** | `Rect.contains()` 精确命中 | 自身 ±48px、子视图 ±5px、兄弟 ±30px | 误差扩散 |

一句话：**ColorOS 是"命中即定"（hit → target → type → extract），我们是"命中后猜"（hit → 6 层瀑布 → 先到先得）。**

---

## 1. 两侧管线总览

### 1.1 HyperPortal（AES + Bridge）

```
ACTION_DOWN
  └─ Bridge.ping()                     记录 sDownRawX/Y（raw 屏幕坐标）          Bridge.java:341-370
       └─ 800ms 后：AES Interceptor.u/a 触发 或 我们的 sCustomLongPressRunnable 兜底
            └─ Interceptor.d(root, first, second, results, map)   ← 递归 walk     Interceptor.smali:833
                 │  ① 命中判定：v/c.e(root, (int)x, (int)y)  纯 Rect.contains    v/c.smali:246-311
                 │  ② 是 ViewGroup 且 shouldHandleAsViewGroup()：
                 │       倒序递归子 View（后画先试），results 非空立刻停          Interceptor.smali:911-960
                 │  ③ results 仍空 → 对本层 view 反射调 fetchViewContent()
                 │       └─ 已被 patch 成 Bridge.preExtract()                     patch_aes_smali.py:116-148
                 │            └─ extractContentColorOS()  ← 6 级瀑布              Bridge.java:2284-2487
                 │                 ├ 0   Telegram / WeChat 文件
                 │                 ├ 0.5 原始 Uri/PFD（要 isAcceptableImageView）
                 │                 ├ 1   Compose / 2 WebView
                 │                 ├ 4   ★ 容器文本 → 容器图片 → 自身文本 → 自身图片 → 兄弟(±30px)
                 │                 ├ 5   Accessibility 文本
                 │                 └ 6   contentDescription
                 └─ 首个非空 Bundle 即结果（"深度优先、先到先得"）
```

关键的**两条**性质：

1. 递归是**深度优先、首个非空即返回**，所以"更深"层失败后会**上溯到祖先容器**再抽一次；
2. 祖先容器那一层，**文本永远排在图片前面**（`Bridge.java:2382-2395`）。

### 1.2 ColorOS（framework `android.view.viewdrag`）

```
View.dispatchTouchEvent
  └─ ViewExtImpl.dispatchTouchView(view, ev, ...)                       （framework 内联补丁）
       └─ OplusViewDragTouchViewHelper.dispatchTouchView()              OplusViewDragTouchViewHelper:31051
            ACTION_DOWN → 把"这一路上每个 view"塞进 touchViews（WeakReference）
            ACTION_UP   → 清空

ACTION_DOWN → OplusViewDragManager.isFeatureEnabled(view)              :30896
  └─ 按"当前窗口标题(activity)"装载 mWhiteViewList / mBlackViewList
     来自本包配置 PackageDragConfigInfo（云控 XML 切片，见 §2.8）；没有配置 → 拖拽根本不启动

长按触发 → OplusViewDragManager.handleActionMove(ev, ctx, decor)        :30884
  ├ !mIsSupportDrag → return（全局开关 / app 禁用名单）
  ├ tryPluginDrag(...)  ← 第三方 App 自己注册 OplusDragContentPlugin 时优先走这条
  └─ findTouchTargetView(ev, decor)                                    OplusViewDragManager:30859
       ├ 首试 findTouchTargetViewWithEventDispatch(ev)  ← dispatch 链       :30861
       │    遍历 touchViews（外→内）：isInvalidView? → 跳过
       │                            factory.isSupport(view, mViewDragInfo)? → 命中
       │    （isBottomView=true 时继续往后找，取更深的一个）
       └ 空则 findTouchTargetViewWithBfs(ev, decor)     ← 全树 BFS          :30860
            traverseViewTree() 前序展开（父→子）                           :30920
            逆序遍历，命中覆盖赋值；再对选中的 target 做一次"遮挡检测"
  └─ OplusViewDragProcessFactory.processView(target, ev, ...)
       ├ isSupport()：按 mProcessors 顺序逐个问                          :30946
       │   [File, Compose, TextView, ImageView, WebView]                 :30943
       │   每个 processor 的 isSupport 只做一件事：config.type == 自己    :30664 / :30631
       └ 交给对应 processor 抽取（Text 只取目标 TextView 的文本）
```

**这台机器的本质**：配置（云端下发的 `ViewDragInfo` 规则）先回答"这个视图是什么"（`mType`），
再由对应 processor 只抽那一种东西。**规则里写 `type=text` 的视图永远不可能产出位图，
写 `type=image` 的视图永远不可能产出文字** —— 结构上就不存在"长按图片出文字"。

---

## 2. 逐段对比

### 2.1 触点采集：一致

| | ColorOS | 我们 |
|---|---|---|
| 采集时机 | `ACTION_DOWN`（`dispatchTouchView`） | `ACTION_DOWN`（`Bridge.ping`） |
| 坐标 | `MotionEvent.getX/getY` 与 `getLocationOnScreen` 比较 | `getRawX/getRawY` 与 `getLocationOnScreen` 比较 |
| 结论 | 都在屏幕坐标系，**这一段没差异** | |

### 2.2 候选集怎么产生：**中差异**

| | ColorOS | 我们 |
|---|---|---|
| 主路径 | **真实 dispatch 链**（`touchViews`）：只有"事件确实传到了它身上"的 view 才是候选 | **几何 walk**：`Interceptor.d` 从 DecorView 递归，`v/c.e` 判 `Rect.contains(可见矩形)` |
| 叶子过滤 | 非 `ViewGroup` 非 `WebView` 的**叶子**全部入候选 | 无（每个节点都会被问一次 `preExtract`） |
| 容器过滤 | ViewGroup **只有 clickable 或 longClickable** 才入候选，且仅保留"最小"那个（`getSmallView`，按 `width+height` 排序） | 任意祖先都可能被上溯选中，不看 clickable |
| 兜底 | 全树 BFS（仅当 dispatch 链无解，或 `FLAG_TOUCH_VIEW` 才强制 dispatch） | 无第二通道 |
| 裁剪 | `isValidView = 可见 && 命中 && !isEventOutOfBoundsOfParent()` | `v/c.e` 已算可见矩形，但**没有父裁剪判定** |

差异带来的实际效果：

- ColorOS 的候选集**天然排除了"盖在图片上但没接收事件的透明容器"**，因为我们根本没有"它到底收没收到事件"这个事实；
- ColorOS 对容器加了 `clickable/longClickable` 门槛，**"纯布局容器"根本进不了候选**（这正是图文卡片出错的那一层）；
- 我们把任意祖先都当候选，于是容器层成了"兜底文本发射台"。

### 2.3 target 的选择规则：**结构性差异**

ColorOS（BFS 通道，`findTouchTargetViewWithBfs:30860`）：

```java
// 逆序遍历（i = size-1 → 0），命中就赋值，不 break
if (!isInvalidView(v, ev) && mViewDragInfo != null && factory.isSupport(v, mViewDragInfo))
    target = v;
// 选完之后：遮挡检测
for (i = targetIndex; i >= 0; i--) {
    if (other != target && other 与 target 矩形相交 && other.isVisibleToUser()
        && other 矩形包含触点 && !other.isSkipDraw()
        && !sameDepth(other, target)          // 同父兄弟不算遮挡
        && !isEmptyView(other) && !isViewSuspicious(other)) {
        target = null;                        // ← 认为"我选中的东西被盖住了"，宁可放弃
    }
}
// 第二轮：同样的条件 + other.getZ() > target.getZ() → 同样放弃
```

三条我们完全没有的判据：

1. **`isInvalidView` 里已经包含"这条视图必须匹配到配置规则"**（`getValidViewInfo` 返回 null 就直接跳过），
   所以"容器"这类没有规则的节点**从候选里就消失了**；
2. **遮挡检测**：选中的节点如果被另一条（不同深度的、可见的、包含触点的、非全屏的）视图盖住，就**直接放弃**，
   而不是随便给你点别的（`isViewSuspicious` = 宽度等于根宽且高度 ≥ 80% 根高 → 视为全屏，不算遮挡，`:30903`）；
3. **`FLAG_BOTTOM_VIEW` / `FLAG_TOUCH_VIEW` 两种取向**：前者取最深命中，后者只认 dispatch 链（最准的"我到底按在哪"）。

我们（`Bridge.java:7216-7297` + `2284-2487`）：

```java
findDeepTouchView():  倒序递归子 view，可见 && 与触点相交(±5px) && !isPassthroughOverlay → 深入
                      一个都没命中 → 返回当前 ViewGroup 本身
extractContentColorOS():  target = leaf（可能是个 ViewGroup）
                      第 4 步先 extractViewGroupText(target)，再 extractViewGroupImage(target)
```

即：**没有"放弃"这个选项，也没有"这条视图配不配"这个门槛**，容器永远有机会说话，而且它是先说话的那个。

### 2.4 模态归属：**决定性差异**

| | ColorOS | 我们 |
|---|---|---|
| 模态由什么决定 | `ViewDragInfo.mType`（`"text"` / `"image"` / file / video），配置预先写死 | `extractContentColorOS` 里步骤的先后顺序 |
| 同一视图内的优先级 | `[File, Compose, Text, Image, WebView]`（`:30943`），但每条规则的 `type` 只有一个，实际永远只命中一个 | Text → Image（`2384` 先于 `2390`；`2404` 先于 `2410`） |
| 文本怎么取 | `getTextViewText(tv)`：**只读这一个 TextView 的 text**（经 framework `ITextViewWrapper`，`:30875`） | `extractTextView` / `extractCustomText` / **`extractViewGroupText`** |
| 会不会跨节点拼文本 | **不会** | **会**（见 2.5） |

### 2.5 容器级文本聚合：**本 bug 的直接原因**

```java
// Bridge.java:2382-2395
if (candidate instanceof ViewGroup && !isComposeView(candidate) && !isWebViewLike(candidate)) {
    if (wantText) {                                    // ← 先文本
        Bundle grpText = extractViewGroupText((ViewGroup) candidate);
        if (grpText != null && isBundleMeaningful(grpText)) return grpText;
    }
    if (wantBitmap) {                                  // ← 后图片
        Bundle grpImg = extractViewGroupImage((ViewGroup) candidate);
        ...
    }
}
```

`extractViewGroupText`（`Bridge.java:2609-2639`）做的事：**把这个容器子树里深度 ≤ 6 的所有 TextView 文本拼成一段**。
也就是说：只要"更深的图片层"交出 null，上溯到卡片容器时，**整格的标题+正文立刻变成结果**，
`extractViewGroupImage` 根本没机会执行。

ColorOS 侧没有对应实现：文本只可能来自 `getTextViewText(命中的那一个 TextView)`（`:30875`），
框架里不存在"把子树文本拼起来"的方法。

### 2.6 图片合格线：**误杀点**

| 判据 | ColorOS | 我们 |
|---|---|---|
| drawable 为空 | 否决（`isInvalidImageView:30897`，除非规则 `allowNullDrawableImage`） | 视情况（`isAcceptableImageView` 允许无 drawable 的"自定义图片 view"，靠反射找 Bitmap） |
| 尺寸 | **无门槛** | `< 60px` 否决（`2810`） |
| 类名 | **无黑名单** | `name.contains("Overlay"/"Background"/"Decor"/"**Layout**")` → 否决（`2801-2804`） |
| 资源 id 名 | 无 | 各家缩略图黑名单（`isInvalidViewId:2840+`） |

`name.contains("Layout")` 这一条杀伤力最大：九宫格/图文格这类**"图片画在 Layout 里"**的自定义视图
（国内 App 常见命名如 `NineGridLayout` / `XxxImageLayout`）会被直接判死，然后上溯 → 容器文本 →
**正好就是"长按图片出文字"**。（具体是哪些类命中这条，需要设备侧日志核实，见 §7。）

### 2.7 容差：误差扩散

| 位置 | 容差 | 说明 |
|---|---|---|
| ColorOS BFS 命中 | **0**（`Rect.contains`，`:30901`） | 精确 |
| ColorOS dispatch 链 | **0**（事件到没到就是事实） | 精确 |
| AES 原生 walk `v/c.e` | 0（精确 `Rect.contains`，`v/c.smali:295-305`） | 精确 |
| 我方 `isTouchInsideView` | **±48px**（`1019-1022`） | 兄弟视图"擦边"也算命中 |
| 我方 `findDeepChildInGroup` | **±5px**（`7289`） | |
| 我方第 4 步兄弟扫描 | **±30px**（`2427-2428`） | 图片正上方 30px 的文字会被当成"触点所在" |

单个容差都不大，但它们串起来就把"图片"和"上一行文字"的边界抹掉了。

### 2.8 ColorOS 的"黑白名单"到底是什么（云端配置实证）

> 常见误解：*"ColorOS 没有黑白名单，是纯通用的。"*
> 实测结论：**它有，而且拖拽完全依赖它** —— 只是它不叫"黑白名单"、也不在设置里给你看。

**配置文件（两处，取版本号大的那个）** —— `OplusViewDragRusHelper.init()`：

```java
File dataFile = new File("data/oplus/os/sys_view_drag_list.xml");            // RUS 云控下发落盘
File sysFile  = new File("system_ext/oplus/sys_view_drag_list.xml");         // ROM 内置
// 各自 getVersion(xml)，谁大用谁
```

- `OplusViewDragRusHelper extends com.android.server.pm.RomUpdateHelper`
  —— **RUS = OPPO 的 ROM Update Service（云控/配置中心）**，基类就是那套"带版本的配置下发"框架；
  `initRusHelper()` 里注册的配置名是 `sys_view_drag_list`。
- 下发链路：
  `system_server: OplusViewDragSystemServer.preBindApplication`
  → Bundle 键 `view_drag_package_raw_xml_gz_bundle_key` / `view_drag_package_raw_xml_bundle_key` / `view_drag_global_config_bundle_key` / `view_drag_bundle_key`
  → 应用进程 `OplusActivityThreadExtImpl.hookHandleBindApplication` 取出
  → `OplusViewDragPackageConfigParser.parse()` → `PackageDragConfigInfo` → `mPackageDragConfig`。
  （即：**每个 App 启动时由 system_server 把"你自己那一份"XML 塞进启动 Bundle**；
  system_server 侧用 `getPackageDragConfig(pkg)` / `extractPackageXmlFromList()` 按包名切片，带 LruCache。）

**XML 结构**（`OplusViewDragPackageConfigParser.handleStartTag` + `createViewDragInfo` 的标签/属性名）：

```xml
<root>
  <whiteList>                                   <!-- 规则表 -->
    <package name="com.xxx">
      <activity name=".MainActivity" supportFileDrag="true">
        <view name="android.widget.ImageView"   <!-- 匹配：name/parent/brother/uncle/cousin/nephews/id/nest/max-depth/source/entryName -->
              type="image"                      <!-- 模态：text | image | file | video -->
              touch-view="true"                 <!-- 只认 dispatch 链命中的那个 -->
              bottom-view="false"               <!-- 取最深命中 -->
              image-path="..." drawable-ref-method-name="..."
              text-field-path="..." text-key-word="..." />
        <view name="android.widget.TextView" type="text" ... />
      </activity>
    </package>
  </whiteList>
  <blackList>                                   <!-- 同一 app 内要排除的视图规则 -->
    <package name="com.xxx"> <activity ...> <view .../> </activity> </package>
  </blackList>
</root>
```

**两组容易混淆的"名单"**：

| 名称 | 实质 | 粒度 | 存在哪 |
|---|---|---|---|
| `whiteList` / `blackList`（XML 里） | **视图规则表**（不是应用名单）：白=可以被拖的视图及其模态，黑=该 app 里要跳过的视图 | package → activity → view 类名/层级 | `sys_view_drag_list.xml`（ROM + RUS 云控） |
| `OPLUS_VIEW_DRAG_DISABLED_APP_LIST` | **真正的应用禁用名单**（字符串列表） | package | `Settings.System["oplus_view_drag_disabled_app_list"]` |
| `OPLUS_VIEW_DRAG_SWITCH` | 传送门总开关 | 全局 | `Settings.System["oplus_view_drag_switch"]` |

**硬门槛**（`isFeatureEnabled`，`:30896`）：

```java
if (mPackageDragConfig == null) return false;                  // 该包没配置 → 不支持
String activity = getWindowTitle(view);  if (activity == null) return false;
List<ActivityDragInfo> white = mPackageDragConfig.getWhiteList();
if (white == null || white.isEmpty()) { log "whiteActivityDragList is null or size is 0"; return false; }
mWhiteViewList = findViewDragList(white, activity);            // 按当前 activity 过滤
if (mWhiteViewList == null || mWhiteViewList.isEmpty()) return false;
```

也就是说：**XML 里没有这个应用（或没有这个 Activity）→ `findTouchTargetView` 永远返回 null → 长按不拖**，
唯一例外是 App 自己实现 `OplusDragContentPlugin`（`tryPluginDrag` 优先于 ROM 配置路径）。
用户在设置里只看得到"传送门"总开关，看不到这份名单 —— 这就是"感觉没有黑白名单"的来源。

**对我们的三点启示**：

1. ColorOS 长按图片"永远对"，**不是因为它的命中算法更聪明，而是因为这个视图被点名写了 `type=image`**；
   图文混排里文字和图片是两条独立规则、各自绑定自己的视图，天然不可能串。
2. 我们的通用启发式在**未适配 app** 上覆盖面更广（不用逐 app 写规则），代价就是必须自己解决"同格多模态"的判定问题。
3. 因此修复方向不是照搬它的规则系统，而是把它那个结构性优点拿过来：**先定 target，再由 target 定模态**。

---

## 3. 为什么"文字在上、图片在下"我们必错（四条具体链路）

以一张典型的图文卡片（`CellGroup > [ TitleText, DescText, ImageView ]`）为例，手指落在图片中间：

### 链路 A：图片叶子被 `isAcceptableImageView` 否决 —— **最常见**

```
AES walk → 叶 ImageView（或 NineGridLayout）
  → preExtract(leaf)                       Bridge.java:940
    → extractContentColorOS(leaf)          :2284
      → 0.5 extractFile：isAcceptableImageView=false（类名含 Layout / <60px）→ 跳过   :2311
      → 4  candidate=leaf（不是 ViewGroup）→ 自身 TextView? 否
                                            自身 isAcceptableImageView? 否（同上）
      → 返回 null
→ AES results 仍空 → 上溯到 CellGroup → preExtract(CellGroup)
  → extractViewGroupText(CellGroup) → "标题\n正文\n……"  ★ 命中，返回
  → extractViewGroupImage 永不执行
```
结果：**拖出的是整格文字**。`nine-grid` / 自定义图容器 100% 走这条。

### 链路 B：图片是"画在容器上"的（背景/`dispatchDraw`）

叶 ImageView 不存在（图片由 `CellGroup.onDraw` 或 `setBackground` 绘制），
AES 直接以 `CellGroup` 为叶子调用 `preExtract` → 同样落到 `extractViewGroupText`。
`extractImageBitmap` 里那句注释 *"Removed arbitrary view.draw fallback"*（`3079`）意味着我们**不会**去抓容器上的位图。

### 链路 C：自定义长按兜底（`onCustomLongPressTriggered`）

```java
View touchTarget = findDeepTouchView(root, sDownRawX, sDownRawY);   // Bridge.java:475
```
`findDeepTouchView` 会跳过 `isPassthroughOverlay` 的节点（透明背景、不可点击、非文本/图片类 → **视为穿透层**，`7204-7211`）。
自定义图片 view 恰好满足这个画像，于是被跳过，返回它的**父容器** → `preExtract(容器)` → 又是一次 `extractViewGroupText`。

### 链路 D：图片抽到了，但被"兄弟扫描"覆盖（少见但真实）

第 4 步的向上循环里，`candidate` 每上一层，就会扫它的**所有兄弟**，只要兄弟矩形 ±30px 包含触点
（`2427-2428`）就优先试文本（`2429-2440`）。若图片上叠了一个跨边界的 `TextView`（水印、"查看大图"、`contentDescription` 承载节点），
兄弟扫描会让文本先于当前层的图片返回。

> 补充：`contentDescription` 兜底（`2472-2484`）也常常是文本的"最后一扇门"——
> 很多卡片会把摘要塞进根容器的 `contentDescription`，长按图片时就命中它。

---

## 4. 量化对比表

| 维度 | ColorOS | HyperPortal | 影响 |
|---|---|---|---|
| target 数量 | 1（解出来就定死） | 1（但求解过程有 6 层瀑布，任一层都可能改写答案） | 可预测性 |
| 模态判定 | 配置规则 `mType` | 代码步骤顺序 | **正确性** |
| 容器文本聚合 | 不存在 | 存在且优先 | **正确性** |
| 失败语义 | 放弃（宁可不给） | 降级（给点别的） | **正确性** |
| 遮挡校验 | 有（覆盖 + Z 序 + skipDraw + 父裁剪） | 无 | 正确性 |
| 容器候选资格 | 必须 clickable/longClickable | 无 | 正确性 |
| 图片合格线 | 1 条（drawable==null） | 5+ 条（尺寸/类名/资源名/包名…） | 召回 |
| 命中容差 | 0 | 5~48px | 精确性 |
| 逐 app 适配 | **强依赖**：`sys_view_drag_list.xml` 里没有该 app/Activity → 根本不拖（见 §2.8） | 无规则，纯通用启发 | 覆盖面（我们占优） |
| 可解释性 | 每条日志带 `Target View info = …` | 仅 `preExtract hit type=1/2` | 排障 |

---

## 5. 差距清单（按修复收益排序）

### P0 —— 直接造成图文混排抓错

1. **容器层不允许"整格文本"压过"命中点图片"**
   - 现状：`extractViewGroupText` 先于 `extractViewGroupImage`（`2384` vs `2390`）。
   - 目标：容器层先解"触点落在哪个直接子 view 上"（精确 rect），递归只抽那一个；只有在**没有任何子 view 包含触点**时才允许整格聚合。
2. **给图片正名：去掉 `isAcceptableImageView` 的 `"Layout"` 类名黑名单**（`2801-2804`）
   - 改法：类名命中时**不否决**，而是要求"子树里存在可取的 ImageView/位图"（`findFirstImageRecursively` 已有现成能力）。
3. **兜底路径也要"点在哪抽哪"**
   - `findDeepTouchView` 不应把 `isPassthroughOverlay` 直接判为穿透（`7204-7211`）；对"透明但很大"的节点应当**继续向下找**，而不是回退成父容器。

### P1 —— 消掉误差与"给点别的"

4. 把 `extractContentColorOS` 的兄弟扫描容差从 **±30px → 0**，且**仅当当前层完全无内容**时才扫描兄弟；兄弟只允许贡献同模态（图片层失败时不去问兄弟文本）。
5. `isTouchInsideView` 的 **±48px** 收窄到 ~4dp，避免"擦边即命中"。
6. 引入 ColorOS 式**遮挡判据**：若候选 target 被"更晚绘制、包含触点、可见、非全屏、不同深度"的视图覆盖 → 视为无效（不要退回"给个别的"）。

### P2 —— 架构对齐（可选，收益是长期一致性）

7. 引入**单一 target 解析器** `resolveDragTarget(root, x, y)`，输出 `{view, modality}`，再由 `extractByModality(view, modality)` 抽取；
   逐 app 特例（Telegram/WeChat/Weibo/Compose/WebView）降级为"target 解析的 hint"，而不是"内容瀑布的第 0/1/2 层"。
8. 埋一条**决策链路日志**（见 §6），替代现在的 `preExtract hit type=`。

---

## 6. 建议的修复设计（伪代码，未实现）

```java
// ① 点锚定的容器解析：只认"触点真正落在谁身上"
private static View childContainingPoint(ViewGroup group, float x, float y) {
    for (int i = group.getChildCount() - 1; i >= 0; i--) {        // 后画优先，与 dispatch 一致
        View c = group.getChildAt(i);
        if (c.getVisibility() != View.VISIBLE) continue;
        Rect r = visibleRectOnScreen(c);
        if (r == null || !r.contains((int) x, (int) y)) continue;
        if (isPassthroughOverlay(c)) continue;                   // 透明覆盖层
        return c;
    }
    return null;
}

// ② 容器层顺序改写
if (candidate instanceof ViewGroup group) {
    View hit = childContainingPoint(group, rawX, rawY);
    if (hit != null) {
        Bundle deep = extractContentColorOS(hit, flags);         // 只抽命中的那个子树
        if (isBundleMeaningful(deep)) return deep;
    }
    // 只有在"没有任何子 view 包含触点"时，才允许整格聚合（保持旧能力）
    if (hit == null) {
        Bundle grpText = extractViewGroupText(group);
        if (isBundleMeaningful(grpText)) return grpText;
    }
}

// ③ 图片合格线：类名可疑时改判"子树里有没有真图"，而不是否决
if (name.contains("Layout")) {
    return findFirstImageRecursively((ViewGroup) view, 0) != null;
}
```

---

## 7. 验证方案

**日志埋点**（每行长这样，一条日志即可判定走错原因）：

```
hitPick: xy=(540,1280) level=3 view=NineGridLayout rect=[24,900][1056,1500]
         child=none  decision=groupText(3 TextView)  ← 应改为 decision=image
```

需要覆盖的用例矩阵：

| 场景 | 期望 | 当前 |
|---|---|---|
| 微博正文+单图 | 图片 | ❓ |
| 微博九宫格 | 图片 | ❓（若图片容器类名含 `Layout` → 必错） |
| 小红书笔记（文字+图） | 图片 | ❓ |
| 知乎回答中的图 | 图片 | ❓ |
| 公众号文章配图 | 图片 | ❓ |
| 纯文本段落 | 文本 | ✅ |
| 图片在上、文字在下 | 图片 | ✅（文本在下不干扰） |
| 微信聊天图片 | 图片 | ✅（走 `extractWeChatFile`，不经过容器） |
| 微博大图页 | 图片 | ✅（走 `extractFile`） |

> 注意：微信/微博大图之所以一直正常，是因为它们**在瀑布的第 0/0.5 层就命中**，从来没走到容器层。
> 图文混排卡片则是唯一会落到第 4 层容器聚合的形态 —— 这也解释了这个 bug 为什么"只在信息流里出现"。

---

## 8. 附录：证据索引

### 8.1 我方（可 grep 核对）

| 位置 | 内容 |
|---|---|
| `Bridge.java:341-370` | `ping()` 采集 ACTION_DOWN raw 坐标 |
| `Bridge.java:940-979` | `preExtract()`：`isTouchInsideView` → `extractContentColorOS` |
| `Bridge.java:1007-1026` | `isTouchInsideView`，**±48px** |
| `Bridge.java:2284-2487` | `extractContentColorOS`：6 级瀑布 |
| `Bridge.java:2382-2395` | **容器层：文本先于图片** |
| `Bridge.java:2419-2451` | **兄弟扫描 ±30px** |
| `Bridge.java:2609-2639` | `extractViewGroupText` / `collectTextViewsRecursively`（深度 6，跨子节点拼文本） |
| `Bridge.java:2641-2666` | `extractViewGroupImage` / `findFirstImageRecursively`（子树第一个图，不看触点） |
| `Bridge.java:2796-2828` | `isAcceptableImageView`（60px + `"Layout"` 黑名单） |
| `Bridge.java:7180-7214` | `isPassthroughOverlay`（透明+不可点击 → 判穿透） |
| `Bridge.java:7216-7297` | `findDeepTouchView` / `findDeepChildInGroup`（±5px，无遮挡校验） |
| `Interceptor.smali:833-1204` | AES 递归 walk：`v/c.e` 命中 → 倒序子 → 本层 fetch → 首个非空返回 |
| `v/c.smali:246-311` | `v/c.e()` = `isShown && w>2 && h>2 && visibleRect.contains(x,y)`（**精确**） |
| `patch_aes_smali.py:116-148` | `fetchViewContent` → `Bridge.preExtract` 包装 |

### 8.2 ColorOS（`oppo/oplus-framework.jar`，`android/view/viewdrag/`）

| 方法（method_idx） | 作用 |
|---|---|
| `OplusViewDragManager.findTouchTargetView(ev, root)` :30859 | 主入口：先 dispatch 链，后退化到 BFS；`FLAG_TOUCH_VIEW` 时只认 dispatch |
| `…findTouchTargetViewWithEventDispatch(ev)` :30861 | 遍历 dispatch 链，`isInvalidView` + `factory.isSupport` 首个命中；`isBottomView` 时取最深 |
| `…findTouchTargetViewWithBfs(ev, root)` :30860 | 前序展开全树 → 逆序命中 → **遮挡检测**（相交/可见/含点/非 skipDraw/非同父/非空/非全屏/Z 序） |
| `…isValidView(view,x,y)` :30901 | 可见 && `Rect.contains` && 非父裁剪外 |
| `…isInvalidView(view,ev)` :30898 | 反之；其中 `getValidViewInfo == null`（无配置规则）即判无效 |
| `…getValidViewInfo(view)` :30878 | 在 `mWhiteViewList` 里找匹配的 `ViewDragInfo`（写回 `mViewDragInfo`） |
| `…getTextViewText(tv)` :30875 | **只读该 TextView 自身文本**（`ITextViewWrapper`） |
| `…isInvalidImageView(view)` :30897 | **唯一**图片否决条件：`ImageView && getDrawable()==null` |
| `…isViewSuspicious(view)` :30903 | 宽==根宽 且 高/根高 ≥ 0.8 → 视为全屏，不算遮挡 |
| `…sameDepth(a,b)` :30911 | 同父 → 兄弟间不算遮挡 |
| `…traverseViewTree(list, v)` :30920 | 前序 DFS（父→子） |
| `OplusViewDragTouchViewHelper.dispatchTouchView(v,ev)` :31051 | `ACTION_DOWN` 收集整条 dispatch 链（跳过 DecorView） |
| `…getTouchView()` :31053 | 叶子全收 + 容器只留"最小的 clickable/longClickable"（`getSmallView`，按 `w+h`） |
| `…getViewSize(v)` :31054 | `width + height` |
| `OplusViewDragProcessFactory.<init>` :30943 | processor 顺序 `[File, Compose, Text, Image, WebView]` |
| `…isSupport(v,info)` :30946 | 顺序试 processor，首个支持者即处理器 |
| `OplusTextViewDragProcessor.isSupport` :30664 | `info.getType() == "text"` 且缓存 `textFieldPath/textKeyWord/newFilePath` |
| `OplusImageViewDragProcessor.isSupport` :30631 | `info.getType() == "image"` |
| `bean/ViewDragInfo` 字段 | `mType`、`mTextFieldPath`、`mTextKeyWord`、`mImagePath`、`mFilePath`、`mViewName`、`mParent`、`mBrother`、`mUncle`、`mCousin`、`mNephews`、`mId`、`mEntryName`、`mSemanticsKeys`、`mEvaluateJs`、`mMaxDepth`、`mNest`；flags：`FLAG_BOTTOM_VIEW` / `FLAG_TOUCH_VIEW` / `FLAG_DRAW_COVER` / `FLAG_UNCLE_VISIBILITY` |
| `…isFeatureEnabled(view)` :30896 | **配置硬门槛**：无 `mPackageDragConfig` / 无该 activity 的 whiteList → false，拖拽不启动 |
| `…isSupportDrag(ctx)` :30900 | 全局开关 `oplus_view_drag_switch` + app 禁用名单 |
| `…getViewDragDisabledAppList(ctx)` :30880 | `Settings.System["oplus_view_drag_disabled_app_list"]`（真正的应用黑名单） |
| `OplusViewDragPackageConfigParser.handleStartTag` :30941 / `createViewDragInfo` | 云控 XML 的解析骨架：`whiteList/blackList → package → activity(supportFileDrag) → view(type, touch-view, bottom-view, text-key-word, image-path, parent, brother, uncle, cousin, nephews, id, nest, max-depth, semantics-keys, evaluate-js …)` |

### 8.3 ColorOS 云控配置链路（`oppo/oplus-services.jar`，system_server 侧）

| 位置 | 内容 |
|---|---|
| `com.android.server.viewdrag.OplusViewDragRusHelper extends com.android.server.pm.RomUpdateHelper` | RUS（ROM Update Service）配置助手，配置名 `sys_view_drag_list` |
| `OplusViewDragRusHelper.init()` | 读 `data/oplus/os/sys_view_drag_list.xml`（云控落盘）与 `system_ext/oplus/sys_view_drag_list.xml`（ROM 内置），比对 `<version>` 取新 |
| `…getPackageDragConfig(pkg)` / `extractPackageXmlFromList(...)` | 按包名切片出该 App 的 XML（LruCache 缓存），无该包 → null |
| `OplusViewDragSystemServer.preBindApplication` | 把 `view_drag_package_raw_xml_gz_bundle_key` / `view_drag_global_config_bundle_key` / `view_drag_bundle_key` 塞进 bind-application Bundle |
| `android.app.OplusActivityThreadExtImpl.hookHandleBindApplication` | App 进程侧取上述 Bundle 键 → `setPendingGzDragConfig` / `setRawDragConfigXml` |
| `OplusViewDragManager.loadPendingConfigOnBackground` :30907 | gz 解压 → `parse()` → `mPackageDragConfig`（同时把 `mRawConfigXml` 存下，调试用） |
| `OplusDragSystemServerSupplement.getSwitchClosed` | 读 `oplus_view_drag_switch` 全局开关 |

## 9. 一个值得记住的差别（产品视角）

ColorOS 的高命中率**建立在逐 app 云控规则上**（详见 §2.8：
`sys_view_drag_list.xml`（ROM 内置 + RUS 云控）→ `preBindApplication` 切片下发 → 每 App 一份 `PackageDragConfigInfo`），
规则里明确写死"哪个类的哪个子视图是 text、哪个是 image"。它没有"通用图文混排正确性"，
只有"已适配列表内的正确性"；**未适配的应用连拖拽都不会起来**（`isFeatureEnabled` 直接 false），
除非 App 自己实现 `OplusDragContentPlugin`。

我们的优势是**零规则、通用启发**（不挑 app，装机即用）；代价就是这套瀑布会在"图文同格"的形态下按顺序排错。
所以修复方向不是照抄它的规则系统，而是**把它唯一正确的那个结构性决定抄过来：先定 target，再由 target 定模态。**
