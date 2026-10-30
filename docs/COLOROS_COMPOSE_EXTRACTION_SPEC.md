# ColorOS Compose 视图识别 (`OplusComposeViewDragProcessor`) 与底部重排完整逆向规范

> **更新时间**：2026-10-31 00:46 (Local)  
> **逆向数据源**：
> - `dataset-1`：`oppo/oplus-framework.jar` (`android.view.viewdrag.OplusComposeViewDragProcessor`、`OplusViewDragShadowBuilder`、`ViewExtImpl`)
> - `dataset-5`：`poc/work/deepseek.apk` (`com.deepseek.chat` R8 混淆后的 Compose 语义树与布局树结构)
> - 实机运行日志：`poc/build/live_test.log`

---

## 一、 为什么 v38 拖到底部依旧不会重排（5 个实锤根因）

通过对 `live_test.log`、`Bridge.java` 与 `PortalService.java` 的逐行追踪，确认 `live_test.log` 中从未出现过 `shadow update called square=true`，其完整链路存在以下 **5 个致命阻断点**：

### 1. 致命阻断点 A（最核心）：`startSystemDrag` 未设置 `STATE_DRAGGING`，导致系统 `ACTION_CANCEL` 瞬间清空 `sActiveDragView`
- 在 `Bridge.ping()` 的 `ACTION_DOWN` 中（第 234 行），自定义的 `sCustomLongPressRunnable` 已被移除，长按完全走 AES 原生管道：`deliverContent()` -> `sPendingPayload` -> 手指滑动 `ACTION_MOVE` -> `forwardToPortal()` -> `startSystemDrag()`。
- 然而，全文件中唯一调用 `sGestureState.set(STATE_DRAGGING)` 的地方在已停用的 `onCustomLongPressTriggered()`（第 347 行），**`startSystemDrag()` 内部根本没有设置 `sGestureState.set(STATE_DRAGGING)`！**
- 当 `target.startDragAndDrop(...)` 启动系统级拖拽的瞬间，Android `WindowManagerService` 会向当前窗口派发一个系统级 `MotionEvent.ACTION_CANCEL`（用于终止应用内常规触摸流）。
- 该 `ACTION_CANCEL` 进入 `Bridge.ping()` 第 270 行时：
  ```java
  if (action == MotionEvent.ACTION_CANCEL && sGestureState.get() == STATE_DRAGGING) {
      return;
  }
  sActiveDragView = null;
  ```
  由于 `sGestureState.get()` 仍为 `STATE_IDLE (0)`，条件为假，**`sActiveDragView` 在拖拽启动后 1 毫秒内就被直接置为 `null`！**
- 后续无论任何地方调用 `updateSystemShadow(true)`，都在第一行 `if (view == null) return;` 直接静默退出！

### 2. 致命阻断点 B：`PortalService.mBottomContainer` 全高 380dp 拦截了应用窗口的 `ACTION_DRAG_LOCATION`
- 拖拽一开始（`state = 1`），`PortalService.showBottomPortal()` 挂载了高度为 `dp(380)`（在 2664px 屏幕上占 `1235px`，覆盖 `y = 1429..2664`）的 `TYPE_APPLICATION_OVERLAY` 悬浮窗 `mBottomContainer`，且**未设置 `FLAG_NOT_TOUCHABLE`**。
- Android 的 `DragState.notifyLocationLw(x, y)` 只会将 `ACTION_DRAG_LOCATION` 派发给当前坐标最顶层的可触摸窗口。
- 当手指刚滑过屏幕中线（`y = 1429px`）时，事件就被 `mBottomContainer` 截走，宿主应用的 `root.setOnDragListener` 收到 `ACTION_DRAG_EXITED`，**永远无法在 `y >= 2184px` 处收到 `ACTION_DRAG_LOCATION`**。

### 3. 致命阻断点 C：`PortalService.handleDragStateChanged` 未向 `Bridge` 转发 `SHADOW_STATE`
- `system_server` 中的 `PortalSystemModule` 在手指进入 `y >= screenH * 0.82f` 时向 `PortalService` 发送了 `com.dragshare.portal.DRAG_STATE_CHANGED (state=2)`，`PortalService` 展开了底部应用栏，但 **`handleDragStateChanged` 内部从未向 `Bridge` 发送 `com.dragshare.bridge.SHADOW_STATE`**。

### 4. 致命阻断点 D：`mBottomContainer.setOnDragListener` 广播未指定包名且无热区判断
- `PortalService.java` 第 662 行发送的 `new Intent("com.dragshare.bridge.SHADOW_STATE")` 未调用 `setPackage(mSource)` 和 `FLAG_RECEIVER_FOREGROUND`，在 HyperOS 上跨进程隐式广播极易被限流丢弃；且只要进入 380dp 容器（屏幕中段 `y = 1429px`）就无条件发 `state = 2`。

### 5. 致命阻断点 E：`Bridge.ping()` 中的 `ACTION_MOVE` 在系统拖拽期间不会触发
- 一旦 `startDragAndDrop` 成功，`WindowManagerService` 接管指针通道，应用不再收到 `MotionEvent.ACTION_MOVE`，只会收到 `DragEvent.ACTION_DRAG_LOCATION`。

---

## 二、 `com.deepseek.chat` (`dataset-5`) Compose R8 混淆结构实测还原

通过 DexSQL 对 `poc/work/deepseek.apk` (`dataset-5`) 的字节码反编译，完整还原其 Compose 关键类与混淆映射表：

| Compose 原始类名 / 成员 | `deepseek.apk` (`dataset-5`) 签名 | 作用说明 |
| :--- | :--- | :--- |
| `AndroidComposeView` | `Landroidx/compose/ui/platform/AndroidComposeView;` | Compose 宿主 View（部分壳或组件内为混淆子类） |
| `AndroidComposeView.getSemanticsOwner()` | `getSemanticsOwner()Ldf9;` | 返回 `SemanticsOwner` 实例 |
| `AndroidComposeView.semanticsOwner` | `y: Ldf9;` | `SemanticsOwner` 成员字段 |
| `AndroidComposeView.getRoot()` | `getRoot()Lkb6;` / `o: Lkb6;` | 返回根 `LayoutNode` |
| `SemanticsOwner` | `Ldf9;` | 持有 `a: Lkb6;` (root `LayoutNode`) |
| `SemanticsOwner.getUnmergedRootSemanticsNode()` | `Ldf9;->a()Laf9;` | 创建并返回未合并的根 `SemanticsNode` |
| `SemanticsNode` | `Laf9;` | 语义节点：`b: Z` (`mergingEnabled`), `c: Lkb6;` (`layoutNode`), `d: Lte9;` (`unmergedConfig`) |
| `SemanticsNode.getBoundsInWindow()` | `Laf9;->g()Lrr8;` | 窗口坐标系矩形 `Rect` (`Lrr8;`: `a,b,c,d` float) |
| `SemanticsNode.getBoundsInRoot()` | `Laf9;->h()Lrr8;` | 根 View 坐标系矩形 `Rect` (`Lrr8;`) |
| `SemanticsNode.getTouchBoundsInRoot()` | `Laf9;->m()Lrr8;` | 触摸热区矩形 |
| `SemanticsNode.getConfig()` | `Laf9;->k()Lte9;` | 获取合并后的 `SemanticsConfiguration` |
| `SemanticsNode.unmergedConfig` | `Laf9;->n()Lte9;` / 字段 `d: Lte9;` | 获取未合并的 `SemanticsConfiguration` |
| `SemanticsNode.getChildren(boolean, boolean)` | `Laf9;->i(ZZ)Ljava/util/List;` | `getChildren$ui_release(includeReplacedSemantics, includeFakeNodes)` |
| `SemanticsNode.unmergedChildren(List, boolean)` | `Laf9;->s(Ljava/util/ArrayList;Z)Ljava/util/List;` | 获取未合并子节点列表 |
| `LayoutNode` | `Lkb6;` | Compose 布局树节点 |
| `LayoutNode.getCollapsedSemantics()` | `Lkb6;->x()Lte9;` / 字段 `t: Lte9;` | LayoutNode 附加的折叠语义配置 (`SemanticsConfiguration`) |
| `SemanticsConfiguration` | `Lte9;` (实现 `Iterable`) | `a: Lpd7;` (`MutableScatterMap`), `c: Z` (`isMerging`), `d: Z` (`isClearing`) |
| `MutableScatterMap` | `Lpd7;` (继承 `Lsw8;`) | `b: Ljava/lang/Object;` (`Object[] keys`), `c: Ljava/lang/Object;` (`Object[] values`) |
| `SemanticsPropertyKey` | `Lif9;` | `a: Ljava/lang/String;` 保存**未混淆**的属性名！ |
| `SemanticsProperties` | `Lff9;` | `a="ContentDescription"`, `z="Role"`, `A="TestTag"`, `B="LinkTestMarker"`, `C="Text"`, `D="TextSubstitution"`, `G="EditableText"` |
| `AnnotatedString` | `Lkn;` (实现 `CharSequence`) | `a: Ljava/lang/String;` 保存原始文本字符串 |

---

## 三、 `Bridge.java` 导致 Compose 应用（DeepSeek / Mishka 等）全面失效的 7 大 Bug

对比 ColorOS 官方 `OplusComposeViewDragProcessor` 与 `Bridge.java` 现有实现，定位出 7 个导致 Compose 识别崩溃或丢弃全部文本节点的致命 Bug：

### Bug 1（致命）：`buildComposeMatch` 将含有 `TestTag` 或 `Link` 的节点标记为 `isLink = true` 并整颗丢弃！
- **位置**：`Bridge.java` 第 2611 行与第 2659-2661 行：
  ```java
  } else if (keyName.contains("TestTag") || keyName.contains("Link")) {
      isLink = true;
  }
  ```
  以及：
  ```java
  if (match != null && !match.isLink) {
      candidates.add(match);
  }
  ```
- **后果**：在 DeepSeek、Bilibili、知乎等几乎所有现代 Compose 应用中，开发者都会在聊天气泡、列表项、文本组件上挂载 `Modifier.testTag(...)`（对应 `SemanticsProperties.TestTag`）或富文本链接标记 `LinkTestMarker`。
- **ColorOS 原版逻辑**：ColorOS 的 `isIgnoredSemanticsPropertyKey(keyName)` 只是在遍历 `SemanticsConfiguration` 的属性键值对时**跳过 `TestTag` / `LinkTestMarker` 这个属性本身**（不把 `TestTag` 的值当成用户文本），**绝不会丢弃该 `SemanticsNode` 节点！**
- `Bridge.java` 把含有 `TestTag` 的节点直接判定为 `isLink = true` 并丢弃，导致 `compose candidates count=14 -> enhance miss`！

### Bug 2（致命）：`getComposeSemanticsOwner(View view)` 反射调用传错对象 `m.invoke(view)`
- **位置**：`Bridge.java` 第 2282-2290 行：
  ```java
  View target = resolveAndroidComposeView(view);
  ...
  Method m = findMethod(target.getClass(), "getSemanticsOwner");
  if (m != null) {
      m.setAccessible(true);
      return m.invoke(view); // <-- 错误！target 是 AndroidComposeView，view 可能是外层 ComposeView！
  }
  ```
- **后果**：当传入的 `view` 是外层包装容器 `androidx.compose.ui.platform.ComposeView` 时，`resolveAndroidComposeView(view)` 找到了内层的 `AndroidComposeView` (`target`)，并在 `target.getClass()` 上找到了 `getSemanticsOwner()` 方法，但随后却对 `view` (`ComposeView`) 调用了 `m.invoke(view)`，直接抛出 `IllegalArgumentException: object is not an instance of declaring class`！
- 随后的 fallback 在混淆应用（如 DeepSeek，`semanticsOwner` 字段名为 `y`，类型为 `df9`）中也全部落空，直接返回 `null`！

### Bug 3（致命）：`getComposeChildren(Object node)` 循环外捕获异常 + 错误的参数 `(false, false)`
- **位置**：`Bridge.java` 第 2563-2581 行：
  1. `try { for (Method m : node.getClass().getDeclaredMethods()) { ... m.invoke(node, args); } } catch (Throwable ignored) {}` 将 `try-catch` 写在了 `for` 循环**外部**！
  2. 在 `Laf9;` (`SemanticsNode`) 中，返回 `List` 的方法有 3 个：
     - `i(ZZ)Ljava/util/List;` (`getChildren$ui_release(boolean, boolean)`)
     - `j(ILaf9;)Ljava/util/List;` (`static synthetic getChildren$default`)
     - `s(Ljava/util/ArrayList;Z)Ljava/util/List;` (`unmergedChildren$ui_release`)
  3. 如果 `getDeclaredMethods()` 先遍历到 `j(int, Laf9)` 或 `s(ArrayList, boolean)`，传入 `(0, null)` 或 `(null, false)` 会立即抛出 `NullPointerException`，直接跳出整个 `for` 循环，`i(ZZ)` 甚至根本没有机会被调用！
  4. 即便调到了 `i(ZZ)`，查看 `Laf9;->i(ZZ)` 的反编译字节码可知：第一个参数是 `includeReplacedSemantics`（在 `getChildren$default` 中默认值为 `!mergingEnabled`，即对于 unmerged 树必须传 `true`！）。若传 `(false, false)`，当节点配置 `isClearingSemantics` 时直接返回 `EmptyList`！

### Bug 4（致命）：`isSemanticsTextKey` 误把 `EditableText` 当作只读展示文本，且 `parseComposeTextValue` 对任意对象调用 `.toString()`
- **位置**：`Bridge.java` 第 2628 行与第 2827 行：
  - ColorOS 的 `isIgnoredSemanticsPropertyKey` 明确将 `EditableText`、`TextSubstitution`、`TextLayout`、`TestTag`、`LinkTestMarker` 列为忽略属性（防止误抓取输入框占位符或内部状态）。
  - `parseComposeTextValue` 在第 2827 行对未知类型直接 `return value.toString().trim();`，会把混淆对象引用（如 `kn@4a8b1`）或枚举对象当成文本返回！ColorOS 只接受 `CharSequence`、`List`、或具有 `text`/`a` 字符串字段的 `AnnotatedString`。

### Bug 5（重要缺失）：缺少 ColorOS 的 `extractTextFromLayoutNodeAttached`（从 `LayoutNode` 提取折叠语义）
- 在 ColorOS `OplusComposeViewDragProcessor` 中，不仅从 `SemanticsNode.unmergedConfig` (`Laf9;->d` / `k()`) 提取文本，还会读取 `SemanticsNode.layoutNode` (`Laf9;->c` -> `Lkb6;`) 上的 `getCollapsedSemantics()` (`Lkb6;->x()Lte9;` 或字段 `t: Lte9;`)，并用 `pickLongerText` 取更完整的一方。

### Bug 6（重要缺失）：缺少 ColorOS 的行级子树聚合兜底 (`findPaddingTouchTextRow` / `collectTextInRowBounds`)
- 在 DeepSeek 聊天界面或 Compose 列表卡片中，用户长按的位置往往落在外层气泡/Row 容器的留白处，或者外层容器是一个 `isMergingSemanticsOfDescendants = true` 的卡片节点（其文本分散在内部的多个未合并子 `SemanticsNode` 中，例如 Markdown 分段、代码块、标题+正文）。
- ColorOS 的做法是：
  1. 若直接命中的节点没有文本，或者命中的是容器节点，定位包含触摸点的最小/最深容器节点 `rowNode`（及其 `rowBounds`）；
  2. 调用 `collectTextInRowBounds(rowNode, rowBounds, touchX, touchY)`，遍历该容器子树（最多 128 个节点），收集所有与 `rowBounds` 相交的子节点的文本并拼接/择优！

### Bug 7：`viewKind` 与 `findDeepTouchView` 在遇到外层 `ComposeView` 时过早停止
- `androidx.compose.ui.platform.ComposeView` 继承自 `AbstractComposeView`（是一个 `ViewGroup`），它真正的渲染与语义宿主是它的子 View `AndroidComposeView`（甚至内部还可能通过 `AndroidViewsHandler` 嵌套原生 `TextView` / `WebView`）。
- `findDeepTouchView` 在遇到 `ComposeView` 时直接返回外层 `ComposeView`，且对于被完全混淆类名与方法名的自定义 Compose 宿主缺少基于 `SemanticsOwner` / `LayoutNode` 字段特征的识别。

---

### 补充实锤：`Laf9;->s(Ljava/util/ArrayList;Z)Ljava/util/List;` 反编译验证
通过 DexSQL `decode` `dataset-5` (`deepseek.apk`) 的 `Laf9;->s(Ljava/util/ArrayList;Z)Ljava/util/List;`：
```smali
.method public final s(Ljava/util/ArrayList;Z)Ljava/util/List;
    invoke-virtual {v5}, Laf9;->o()Z
    move-result v0
    if-eqz v0, :label_0009
    sget-object v5, Lz54;->a:Lz54;
    return-object v5
    :label_0009
    iget-object v0, v5, Laf9;->c:Lkb6;
    invoke-virtual {v5, v0, v6}, Laf9;->c(Lkb6;Ljava/util/ArrayList;)V
    ...
    return-object v6
.end method
```
- 第一个参数 `v6` (`java.util.ArrayList`) 是用于收集子节点的**输出容器**（由调用方 `new ArrayList<>()` 传入，`Laf9;->c(Lkb6;Ljava/util/ArrayList;)V` 会往 `v6` 中 `add` 子节点并最终返回 `v6`）。
- 旧版 `Bridge.java` 在反射调用时对非基本类型参数一律传 `null`，导致调用 `s(null, false)` 时在 `c(Lkb6;Ljava/util/ArrayList;)` 内部直接触发 `NullPointerException`；再加上 `try-catch` 写在 `for (Method m : ...)` 循环外部，一旦先遍历到 `s(ArrayList, Z)` 或静态合成方法 `j(ILaf9;)`，整个子节点获取函数直接崩溃返回 `null`！
- **修复方案**：
  1. `try-catch` 移入 `for` 循环内部，且跳过 `Modifier.isStatic` 方法；
  2. 对 `(boolean, boolean)` 签名（`i(ZZ)`）传入 `(Boolean.TRUE, Boolean.FALSE)`；
  3. 对 `(ArrayList/List, boolean)` 签名（`s(ArrayList, Z)`）传入 `(new ArrayList<>(), Boolean.FALSE)`。

---

## 四、 全混淆 Compose 宿主识别与 `SemanticsOwner` 结构特征匹配（零反射误判）

### 1. 为什么部分纯 Compose 应用（如 `top.yukonga.mishka`）连 `longPress` / `enhance` 都不触发？
- 在 `ComponentActivity.setContent { ... }` 创建的纯 Compose 页面中，视图树直接将 `AndroidComposeView` 挂载在 `android.R.id.content` 下，外层没有 `ComposeView`。
- 当开启 R8 强混淆时，`AndroidComposeView` 类名被混淆（如 `lb`、`h8`），且若 `getSemanticsOwner()` 方法名也被一并混淆，旧版 `Bridge.viewKind()` 的三个条件：
  `name.contains("AndroidComposeView") || name.contains("compose.ui.platform") || findMethod(clazz, "getSemanticsOwner") != null`
  会**全部返回 `false`**！
- 一旦 `viewKind()` 返回 `VIEW_KIND_NORMAL`：
  `Bridge.shouldHandleAsViewGroup(view)` 对 `AndroidComposeView` 返回 `true`（因为它继承自 `ViewGroup`）。
  随后小米 AES 的 `Interceptor.e()` 将其当作普通 `ViewGroup` 遍历子 View，而 `AndroidComposeView.getChildCount() == 0`，于是直接跳过，**连 `preExtract` 和 `enhanceFetchViewContent` 都不会被调用一次**！

### 2. 终极识别方案 A：利用 Android Framework 接口三元组识别被混淆的 `AndroidComposeView`
`androidx.compose.ui.platform.AndroidComposeView` 继承自 `android.view.ViewGroup`，且在源码级同时实现了三个系统级 `ViewTreeObserver` 接口（向系统注册全局布局、滚动与触摸模式监听）：
- `android.view.ViewTreeObserver.OnGlobalLayoutListener`
- `android.view.ViewTreeObserver.OnScrollChangedListener`
- `android.view.ViewTreeObserver.OnTouchModeChangeListener`
由于这三个接口属于 Android SDK (`framework.jar`)，**R8 永远无法混淆或移除这三个接口实现声明**。因此只需通过：
```java
view instanceof ViewGroup
    && view instanceof ViewTreeObserver.OnGlobalLayoutListener
    && view instanceof ViewTreeObserver.OnScrollChangedListener
    && view instanceof ViewTreeObserver.OnTouchModeChangeListener
```
即可在纳秒级 100% 识别出任何被 R8 重命名的 `AndroidComposeView`！

### 3. 终极识别方案 B：通过类型拓扑特征定位被混淆的 `semanticsOwner` 字段 (`y: Ldf9;`)
当 `getSemanticsOwner()` 方法名与 `semanticsOwner` 字段名均被混淆时：
- 遍历 `AndroidComposeView` 的非基本类型字段 `f`，其类型 `OwnerClass`（如 `Ldf9;`、`Lso5;`）必然满足：
  1. 包含无参方法返回 `NodeClass`（如 `Laf9;`、`Lpo5;`）；
  2. `NodeClass` 包含无参方法返回具有 4 个 `float` 字段的 `RectClass`（如 `Lrr8;`、`Lj95;`），且包含返回 `java.util.List` 的方法。
- 当 `OwnerClass` 存在多个返回 `NodeClass` 的无参方法时（如 `getRootSemanticsNode` 与 `getUnmergedRootSemanticsNode`），优先选择返回节点的 `boolean` 字段（`mergingEnabled`）为 `false` 的未合并根节点。

---

## 9. `v39` 实机测试四大深度发现与 ColorOS 对齐修正 (`v40`)

### 9.1 发现一：Compose 识别坐标偏移与整屏脏文本聚合 (`Laf9;` 逆向对比)
通过对 `dataset-5` (`com.deepseek.chat`) 中 `Laf9;` (`SemanticsNode`) 全部 20 个方法的逐条反编译，明确了 Compose 语义树坐标与配置的精确映射：
- `Laf9;->g()Lrr8;`：`getBoundsInRoot()`（调用 `Lzj3;->S(Lva6;)Lva6;` 获取根布局坐标并计算局部包围盒 `localBoundingBoxOf`，坐标系为相对于 `AndroidComposeView` 左上角的 `(localX, localY)`）。
- `Laf9;->h()Lrr8;`：`getBoundsInWindow()`（调用 `Lzj3;->E(Lva6;Z)Lrr8;` 计算窗口坐标，当 `AndroidComposeView` 在窗口中有偏移或状态栏偏移时与 `localY` 不同）。
- `Laf9;->m()Lrr8;`：`getTouchBoundsInRoot()`。
- `Laf9;->k()Lte9;`：`getConfig()`（当 `p()` 即 `isMergingSemanticsOfDescendants` 为真时，调用 `r(ArrayList, Lte9;)` 将子节点的文本合并进 `Lte9;` 副本中返回）。
- `Laf9;->n()Lte9;`：`getUnmergedConfig()`（直接返回 `Laf9;->d: Lte9;` 字段，未合并子节点语义）。

**导致 `v39` 偏移与整屏文本污染的 3 个根因**：
1. **双坐标系混用导致偏移 (`boundsContainTouch`)**：
   - `v39` 中 `getComposeNodeBounds` 把 `{"getBoundsInRoot", "getBoundsInWindow", "h", "g", "m"}` 混在一起返回，且 `boundsContainTouch` 对同一个 `Rect` **同时判定** `bounds.contains(localX, localY)` **或** `bounds.contains(rawX, rawY)`（外加 `36px` 容差）！
   - 当 `AndroidComposeView` 的屏幕位置 `loc[1] > 0`（或列表气泡在屏幕纵向上刚好相差 `loc[1]`）时，拿 `rawY` 去匹配 `BoundsInRoot`（或拿 `localY` 去匹配 `BoundsInWindow`）会误命中上方/下方偏移 `±loc[1]` 的节点！
   - **修复 (`v40`)**：将节点包围盒统一归一化到 `AndroidComposeView` 的局部坐标系 `(localX, localY)`（优先调用 `getBoundsInRoot` / `g()`，若回退到窗口坐标则减去根节点的 `(rootBounds.left, rootBounds.top)` 偏移），且 `boundsContainTouch` **只与 `(localX, localY)` 比对**（容差收敛为 `12px`）。
2. **`getComposeConfig` 字段优先于方法导致 `mergeDescendants` 气泡漏失直接文本**：
   - `v39` 先反射读取 `Laf9;->d`（未合并的 `unmergedConfig`），后调用 `Laf9;->k()`（合并后的 `getConfig()`），导致设置了 `semantics(mergeDescendants = true)` 的卡片/气泡容器拿不到合并后的文本，被迫回退到子树遍历。
   - **修复 (`v40`)**：在 `buildComposeMatch` 中同时提取 `unmergedConfig`（字段 `d`）与 `getConfig()`（方法 `k()`），择优保留有效用户文本。
3. **`collectTextFromCandidateSubtrees` 中 `i > 0` 漏洞导致回退到全屏根节点 (`i == 0`)**：
   - `v39` 第 2997 行写成了 `if (cand.area <= 0 || (maxArea > 0 && cand.area > maxArea && i > 0)) continue;`。当遍历到 `i == 0`（面积等于全屏的根 `SemanticsNode`）时，`i > 0` 为 `false`，跳过了面积过滤，直接把整屏所有可见文字（包括顶部栏、底栏按钮 `"转至底部 打开侧边栏 自动朗读 开启新对话..."`）全部拼在一起！
   - **修复 (`v40`)**：移除 `&& i > 0`，严格要求容器候选满足 `cand.depth >= 2` 且 `cand.area <= viewWidth * viewHeight * 0.40f`，并在子树收集时按触摸点 `localY` 邻域过滤，彻底杜绝全屏/Scaffold 级聚合。

### 9.2 发现二：普通应用（如酷安 `com.coolapk.market`）混合 Compose 顶层视图抢占导致只识别底栏标签
在 `live_test.log` (lines 11361–11372) 中，长按酷安帖子正文 `(861, 1100)` 时：
- 酷安 `MainActivity` 在整个窗口顶层覆盖了一个全屏的 `androidx.compose.ui.platform.ComposeView`（仅用于绘制底栏 `"首页 数码 发现 我的"` 或底部评论栏 `"写评论"`），而中間的信息流列表则是传统的原生 `RecyclerView` + `TextView`。
- 由于 `findDeepTouchView` 遇到 `isComposeView(root)` 直接返回该全屏 `ComposeView`，且 `collectTextFromCandidateSubtrees` 因 `i == 0` 漏洞遍历了全屏根语义树，导致每次长按屏幕中央的普通帖子都只识别到底栏的 `"首页 数码 发现 我的"`！
- **修复 (`v40`)**：
  1. 修复 `collectTextFromCandidateSubtrees`（禁止遍历 `depth < 2` 或面积超过屏幕 40% 的根节点子树），使酷安顶层全屏 `ComposeView` 在触摸点 `(861, 1100)` 无 Compose 气泡时准确返回 `null`。
  2. 在 `shouldHandleAsViewGroup`、`findDeepTouchView` 与 `extractContentColorOS` 中：若 `ComposeView` 的 `extractCompose` 返回 `null`（即手指触摸处并非 Compose 语义节点，而是穿透到底层原生视图或内嵌 `AndroidViewHolder`），继续向下/向兄弟节点搜索原生 `TextView` / `ImageView`！

### 9.3 发现三：拖到底部触发应用栏后约 40ms 自行消失、重排瞬间还原的根因
在 `live_test.log` 中捕获到精确的 41ms 状态跳变：
- `08:48:40.587`: `handleDragStateChanged state=2 y=2181.0` -> `shadow update called square=true`（成功触发底部应用栏与重排）。
- `08:48:40.628`: `handleDragStateChanged state=1 y=1421.0` -> `shadow update called square=false`（41ms 后被强制退回状态 1）。
- **根因**：
  1. 当 `state == 2` 调用 `setBottomPortalTouchable(true)` 移除 `mBottomContainer`（高度 `380dp = 1235px`，屏幕顶部坐标 `location[1] = 2656 - 1235 = 1421.0`）的 `FLAG_NOT_TOUCHABLE` 时，Android 系统的 `DragDropController` 立即向 `mBottomContainer` 派发 `DragEvent.ACTION_DRAG_ENTERED`，同时向宿主 `DecorView` 派发 `DragEvent.ACTION_DRAG_EXITED`。
  2. **Android 规范中，`DragEvent.ACTION_DRAG_ENTERED` 和 `ACTION_DRAG_EXITED` 事件的 `event.getX()` 与 `event.getY()` 恒为 `0.0f`**！
  3. `PortalService.java` 中 `case DragEvent.ACTION_DRAG_LOCATION:` 与 `case DragEvent.ACTION_DRAG_ENTERED:` 共用分支，计算出 `rawY = location[1] + 0.0f = 1421.0f < screenH * 0.77f`，当场调用 `handleDragStateChanged(1, 1421.0)` 把刚刚展开的应用栏关掉！同时 `ACTION_DRAG_EXITED` 分支也无条件发送了 `notifyBridgeShadowState(1, rawY)`。
- **修复 (`v40`)**：
  - 在 `PortalService.java` 的 `container.setOnDragListener` 中，将 `ACTION_DRAG_ENTERED` 和 `ACTION_DRAG_EXITED` 与 `ACTION_DRAG_LOCATION` 拆开：`ACTION_DRAG_ENTERED` 仅返回 `true`（绝不用其 `(0, 0)` 坐标计算 `rawY`），且在 `ACTION_DRAG_LOCATION` 中增加 `if (event.getX() == 0f && event.getY() == 0f) return true;` 防护；同时对 `SHADOW_STATE` 广播做状态去重（仅当 `state` 变化时发送）。

### 9.4 发现四：DeepSeek (`dataset-5`) 段落与列表 `Modifier.Node` 链 (`Ldha;` / `Lqla;`) 及 `Laf9;->r` 合并机制
通过 `dexsql` 逆向 `deepseek.apk` (`dataset-5`) 的 `Laf9;` (`SemanticsNode`)、`Lkb6;->x()Lte9;` (`LayoutNode.getCollapsedSemantics`)、`Lvg7;->a(Lkb6;Z)Laf9;`、`Ldha;` (`TextAnnotatedStringNode`) 与 `Lqla;` (`TextStringSimpleNode`)，定位到：
1. **`Laf9;` 字段与方法映射**：
   - `Laf9;->a:Lw97;`：`Modifier.Node`
   - `Laf9;->b:Z`：`mergingEnabled`（`Ldf9;->a()` 返回 `unmergedRootSemanticsNode` 时 `b = false`）
   - `Laf9;->c:Lkb6;`：`LayoutNode`（其 `Lkb6;->E:Lbi;` 为 `NodeChain`，`Lbi;->f` / `Lbi;->g` 为 `Lw97;` 链表头尾，通过 `Lw97;->f` `child`、`Lw97;->e` `parent` 及 `Lup3;->p` `delegate` 连接）
   - `Laf9;->d:Lte9;`：`unmergedConfig`（在 `Lvg7;->a(Lkb6;Z)Laf9;` 中直接赋值为 `Lkb6;->x()`，即 `Lkb6;->t:Lte9;`）
2. **为何 `v39` 中部分列表项/段落未能直接命中**：
   - 在 `Lvy0;->O` (`MarkdownOrderedList`) 和 `Lvy0;->P` (`MarkdownBulletList`) 中，列表项 `Row` 设置了 `Modifier.semantics(mergeDescendants = true) {}`（`Lte9;->c = true`）。但因为从 `unmergedRoot` 遍历时 `Laf9;->b == false`，`Laf9;->p()` (`this.b && this.d.c`) 恒为 `false`，导致 `Laf9;->k()` 直接返回空的 `Laf9;->d`，而没有调用 `Laf9;->r(ArrayList, Lte9;)` 合并子节点的 `SemanticsConfiguration`！
   - 同时，`Ldha;` (`TextAnnotatedStringNode`) 在字段 `Ldha;->o:Lkn;` (`AnnotatedString`，实现 `CharSequence`) 中直接持有完整富文本，而 `Lqla;` (`TextStringSimpleNode`) 在字段 `Lqla;->o:Ljava/lang/String;` 中直接持有字符串。
3. **修复 (`v40`)**：
   - 在 `extractTextFromLayoutNodeAttached(Object node, Object nodeConfig)` 中：
     1. 若 `nodeConfig` 的 `isMergingSemanticsOfDescendants` (`Lte9;->c`) 为 `true`，反射调用 `Laf9;->r(ArrayList, Lte9;)` 提取合并后的子节点文本。
     2. 直接遍历 `Laf9;->a` (`Lw97;`) 以及 `Laf9;->c` (`Lkb6;`) -> `Lbi;` (`NodeChain`) 上的 `Lw97;` (`Modifier.Node`) 链表（含 `f` / `e` / `p`），直接提取 `Ldha;->o` (`Lkn;`) 或 `Lqla;->o` (`String`) 中的 `CharSequence` 文本。

### 9.5 发现五：ColorOS 预览框长方形 -> 方形 (`76x76dp`) 实时平滑重排动画
- ColorOS 在进入/离开底部热区时，并不是生硬地在两帧之间切换长方形与 `76x76dp` 方形，而是通过属性动画实时插值卡片宽高 (`width`, `height`)、圆角 (`12dp -> 16dp`)、内边距 (`12dp -> 6dp`) 与字号 (`12.27dp -> 11dp`)，并在每一帧根据当前插值宽度重新构建 `StaticLayout`，使文字随着卡片宽度收缩实时流式换行重排（并在向上拖出热区时平滑展开还原）。
- **实现 (`v40`)**：在 `Bridge.java` 中引入 `ValueAnimator sShadowMorphAnimator`（`200ms`，插值进度 `sShadowMorphProgress` 从 `0f` 到 `1f`），每帧调用 `view.updateDragShadow(new PreviewShadow(..., sShadowMorphProgress))` 实现丝滑形变与文字实时重排。


