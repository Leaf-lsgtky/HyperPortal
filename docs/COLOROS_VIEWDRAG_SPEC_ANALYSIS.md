# ColorOS ViewDrag 与传送门拖拽预览规范深度逆向分析报告

> **生成时间**：2026-10-30 21:42 (Local)  
> **逆向工具**：DexSQL (LSPosed)  
> **逆向对象**：
> 1. `oppo/oplus-framework.jar` (ColorOS 系统核心框架库)
> 2. `oppo/内容传送门_17.4.2.apk` (`com.oplus.contentportal` 官方传送门/中转站应用)

---

## 一、 核心问题根因：组件定位混淆

经过 DexSQL 逆向反编译比对，前序版本中“字号过小、图片过小、文字预览字数极低、拖到底部不重排”的根本原因在于：**混淆了 ColorOS 中两个完全不同的系统组件！**

| 属性 | 组件 A：系统级拖拽阴影 (`oplus-framework.jar`) | 组件 B：传送门停靠微缩标 (`内容传送门.apk`) | 前序 AI 的错误实现 |
| :--- | :--- | :--- | :--- |
| **所属模块** | `android.view.viewdrag.OplusViewDragShadowBuilder` | `com.oplus.contentportal.drag.DragViewHelper` | 混为一谈 |
| **界面定位** | 跟随用户手指移动的**半透明浮动预览框** | 屏幕底部流转停靠区内的**微型卡片** | 把组件 B 的写进了组件 A |
| **宽度策略** | `Math.min(textWidth, viewWidth) * 1.05`，上限 **80% 屏幕宽** | 固定 **76dp** (`drag_view_width_height`) | 强行缩成 76dp / 260dp |
| **高度策略** | `StaticLayout` 自适应，上限 **80% 屏幕高** | 固定 **76dp** | 强行写死 76dp |
| **字数限制** | **无限制**（按文本完整测量排版） | **严格截断 40 字符** (`subSequence(0, 40)`) | 强行截断 40 字符 |
| **字体大小** | **12.27dp** (`0x414451ec * density`) | **11dp** (`dragview_text_size`) | 强行写为 11dp |
| **行间距** | **17.43dp** (`(lineHeight - 4) / fontSize` 或 1.5x) | 未单独设置行高 | 强行写死 14dp |
| **内边距** | **12dp** (`mPadding = round(density * 12)`) | **6dp** (`dp_6`) | 错误写为 6dp |
| **圆角** | **12dp** (`density * 12`) | **8dp** (`drag_icon_radius`) | 错误写为 8dp/10dp |

---

## 二、 ColorOS 真实拖拽阴影规范（System Drag Shadow Spec）

### 2.1 文本预览阴影 (`OplusViewDragShadowBuilder$Text`)

#### 1. 样式与度量参数
* **字体大小 (Font Size)**：
  $$\text{fontSize} = 12.27\text{dp} \times \text{density} \quad (\text{IEEE-754: } 0\text{x}414451\text{ec})$$
  *(在 480dpi 下约为 37px / 16sp，远大于 11dp)*
* **行高 (Line Height)**：
  $$\text{lineHeight} = 17.43\text{dp} \times \text{density} \quad (\text{IEEE-754: } 0\text{x}418\text{b}70\text{a}4)$$
* **行间距倍数**：
  若指定了行高与字号：$\text{multiplier} = \frac{\text{lineHeight} - 4\text{px}}{\text{fontSize}}$；默认使用 `1.5f`。
* **背景与圆角**：
  * 背景色：纯白 `#FFFFFFFF` (`0xFFFFFFFF`)
  * 文字颜色：纯黑 `#FF000000` (`0xFF000000`)
  * 圆角半径：**12dp** (`Math.round(density * 12.0f)`)
  * 边距 (Padding)：**12dp** (上下左右四边均为 12dp)

#### 2. 宽度与高度自适应计算
```java
// 宽度限制：不得超过屏幕宽度的 80%
int maxAllowedWidth = (int) (screenWidth * 0.8f);
int measuredWidth = Math.min((int) textPaint.measureText(text), sourceViewWidth);
int candidateWidth = Math.min(measuredWidth, maxAllowedWidth);
int shadowWidth = Math.min((int) (candidateWidth * 1.05f), maxAllowedWidth);

// 高度限制：依据 StaticLayout 自适应，上限为屏幕高度的 80%
StaticLayout layout = StaticLayout.Builder.obtain(text, 0, text.length(), textPaint, shadowWidth)
        .setLineSpacing(0f, 1.5f)
        .setAlignment(Layout.Alignment.ALIGN_NORMAL)
        .build();

int shadowHeight = Math.min(layout.getHeight() + padding * 2, (int) (screenHeight * 0.8f));
```

#### 3. 触摸锚点偏移 (Touch Offset)
ColorOS 不使用粗暴的居中 `(width/2, height/2)` 覆盖手指，而是计算视线避让偏移：
$$\text{offset}(dim) = \min(dim \times 0.55, \, dim \times 0.50 + 48\text{px})$$
手指位于阴影内上方，使得阴影内容清晰露在手指下方，完全不阻挡视线。

---

### 2.2 图片预览阴影 (`OplusViewDragShadowBuilder$Image`)

#### 1. 五级动态面积缩放表 (`getScale(area, density)`)
ColorOS 针对图片采用面积自适应缩放算法，计算公式为：
$$\text{area} = (W \times \text{scale}) \times (H \times \text{scale}) \quad (\text{单位: } \text{px}^2)$$

| 等级 | 面积阈值区间 (dp²) | 换算像素面积 (以 density 为准) | 缩放比率 (Scale) | 典型对应场景 |
| :--- | :--- | :--- | :--- | :--- |
| **Level 0** | $\le 120 \times 120$ | $\le 14,400 \times \text{density}^2$ | **1.10x** (轻微放大) | 小图标、表情包、小头像 |
| **Level 1** | $(120^2, 200^2]$ | $14,400 \sim 40,000 \times \text{density}^2$ | **0.85x** | 列表单张缩略图 |
| **Level 2** | $(200^2, 360^2]$ | $40,000 \sim 129,600 \times \text{density}^2$ | **0.60x** | 社交动态多宫格配图 |
| **Level 3** | $(360^2, 480^2]$ | $129,600 \sim 230,400 \times \text{density}^2$ | **0.40x** | 手机全屏竖图 |
| **Level 4** | $> 480 \times 480$ | $> 230,400 \times \text{density}^2$ | **0.30x** | 超高清原图、长截图 |

#### 2. 图片阴影尺寸与触摸点
* **阴影尺寸**：
  $$\text{outShadowSize} = (\text{width} \times \text{scale}, \; \text{height} \times \text{scale})$$
* **触摸点**：
  $$\text{outShadowTouchPoint} = (\text{outShadowSize.x} / 2, \; \text{outShadowSize.y} / 2)$$
* **圆角裁切**：使用 12dp 圆角 Path 裁切 Canvas (`Path.addRoundRect` with 12dp)，然后平滑绘制 Bitmap。

---

## 三、 “拖动到底部不会触发重排”的真相剖析

### 3.1 为什么前一个 AI 的代码失效？
前一个 AI 在 `Bridge.java` 中写了：
```java
root.setOnDragListener((v, event) -> {
    if (event != null && event.getAction() == DragEvent.ACTION_DRAG_LOCATION) {
        int screenHeight = v.getResources().getDisplayMetrics().heightPixels;
        updateSystemShadow(event.getY() >= screenHeight * 0.82f);
    }
    return true;
});
```
此实现存在三个致命缺陷：
1. **坐标系错误**：`DragEvent.getY()` 是相对于监听 View（此处为 DecorView）的局部坐标，而 `heightPixels` 是屏幕物理分辨率。在具有导航栏/状态栏窗口偏移的情况下，`event.getY()` 永远无法到达屏幕的 `0.82f`！
2. **事件拦截断流**：在 Android 原生拖拽机制中，`DragEvent` 会优先分发给命中层级的子 View。如果中间有任何子 View 消费了拖拽事件或根视图尺寸未填满物理屏幕，`ACTION_DRAG_LOCATION` 将直接丢弃。
3. **触控事件丢失**：一旦 `startDragAndDrop()` 成功，Android WindowManager 会将全局 Touch 独占接管，目标应用的 `dispatchTouchEvent` 停止收到 `ACTION_MOVE`，因此前一个 AI 在 `onTouch` 里的兜底位置探测完全成为死代码。

### 3.2 ColorOS 真正的“底部重排”机制（双视图无缝切换）
通过 DexSQL 逆向 `com.oplus.contentportal` 证实：
1. **底部不是同一个 Window**：
   ColorOS 的底部流转区是一个名为 `ContentPortal-DragEventReceiver` 的全局系统覆盖窗口（`layout_drag_receive` / `res/pU.xml`，通过 `WindowManager.LayoutParams` 挂载，高度约 380dp，热区高度 `drag_event_response_height = 160dp`）。
2. **“重排”的视觉本质：大卡片与 76x76dp 紧凑卡片的无缝变形切换**：
   * **在普通拖拽期间（屏幕中上方）**：
     系统级拖拽阴影（`OplusViewDragShadowBuilder$Text` / `$Image`）在屏幕上跟随手指显示大卡片（文本宽度高达屏幕 80%，行高 17.43dp，多行自适应；图片按 5 档面积自适应缩放）。此时 `com.oplus.contentportal` 内部的 `dragView` (`DragViewHelper`) 处于完全隐藏状态（`setAlpha(0.0f)`）。
   * **当拖拽进入底部热区（距底 160dp 或进入 Portal 接收区）**：
     - `DragEventHandler` 立即通过 AIDL 向系统发送 `dismissDragShadowView`，隐藏系统拖拽阴影表面（`mDragSurface` 隐藏/透明化）；
     - 与此同时，`DragViewHelper` 的本地 View（`COUITextView` 或 `COUIRoundImageView`）变为可见（`setAlpha(1.0f)`），并播放形变动画，收缩为 **76dp x 76dp 的正方形紧凑卡片**（16dp 圆角，纯白背景，居中 11dp 字号，最多 4 行 40 字；或 76x76 居中裁剪图片），吸附在手指触点正下方；
     - 此时底部 `COUIRecyclerView` 中的应用接收列表（`Lv5/m`）根据手指移动坐标（`ACTION_DRAG_LOCATION`）实时计算距离，触发鱼眼缩放（`1.15f`）与空位重排动画（展开应用插槽供放置）。
   * **当手指拖离底部热区（向上滑回屏幕上方）**：
     - `DragEventHandler` 发送 `showDragShadowView` 重新显示系统大卡片；
     - `DragViewHelper` 的 76x76dp 紧凑卡片渐隐消失（`setAlpha(0.0f)`）。

---

## 四、 先前版本“拖到底部不会触发重排”的 5 大致命缺陷排查

1. **缺陷 1：`PreviewShadow` 缺少 `square` 紧凑模式实现**
   先前代码在 `Bridge.java` 中虽然有 `updateSystemShadow(boolean square)` 方法签名，但 `PreviewShadow` 构造函数和绘制方法根本没有接收 `square` 参数，无论传入什么都绘制 80% 宽度大卡片，阴影从未重排。
2. **缺陷 2：`root.setOnDragListener` 未调用 `updateSystemShadow`**
   在 `ACTION_DRAG_LOCATION` 的热区变化监听逻辑中，只调用了 `notifyPortalDragState`，根本没有触发 `updateSystemShadow(inHotZone)`。
3. **缺陷 3：手势合成 CANCEL 事件提前清空拖拽上下文**
   在 `Bridge.ping` 的 `ACTION_CANCEL` 分支中，`sActiveDragView = null` 等变量在检查 `STATE_DRAGGING` 之前被提前赋值为 `null`，导致系统拖拽启动时注入的合成 CANCEL 直接把活动 View 杀掉，后续所有 `updateSystemShadow` 调用因 `view == null` 判定直接失败。
4. **缺陷 4：拖拽启动时未提前预热底部接收窗口**
   `lastInHotZone` 初始值为 `false`，拖拽开始时处于屏幕中上方，`inHotZone = false`，导致 `inHotZone != lastInHotZone` 为假，`notifyPortalDragState` 从未在拖拽初期发送 `state = 1`，底部接收窗口未能提前由 WindowManager 挂载。
5. **缺陷 5：测试常驻进程未完全重启**
   由于 `classes2.dex` 随各宿主进程初始化加载，安装后未重启的目标应用在内存中继续执行旧版逻辑。

---

## 五、 v38 架构重构与重排修复清单

1. **`PreviewShadow` 完整实现双模态（大卡片 vs 76x76dp 紧凑方块）**：
   - 构造参数增加 `boolean square`；
   - 底层 Surface 尺寸取 `Math.max(contentSize, dp(76))`，确保能容纳两种形态；
   - 当 `square == true` 时：使用 `PorterDuff.Mode.CLEAR` 清屏，并在手指触点 `(touchX, touchY)` 中心绘制 `76dp x 76dp` 的 16dp 圆角紧凑卡片（居中 11dp 字体，最多 40 字符；或居中裁剪 76x76 图片）；
   - 当 `square == false` 时：清屏并绘制完整 12.27dp、多行自适应的 ColorOS 规范大卡片。
2. **打通双向重排联动管道**：
   - `root.setOnDragListener` 在 `ACTION_DRAG_STARTED` 时立即向 `PortalService` 发送 `state = 1`，让底部悬浮窗挂载就绪；
   - 在 `ACTION_DRAG_LOCATION` 进入/离开 160dp 底部热区时，立即调用 `updateSystemShadow(inHotZone)` 触发阴影即时重排，并向 `PortalService` 同步 `state = inHotZone ? 2 : 1`；
   - `PortalService` 的底部容器在收到 `ACTION_DRAG_ENTERED` / `ACTION_DRAG_EXITED` 时，广播 `com.dragshare.bridge.SHADOW_STATE` 作为双重保险，确保跨 Window 也能触发阴影重排；
   - 在 `ACTION_DRAG_ENDED` 时恢复 `square = false` 并清空状态。
3. **修复 `Bridge.ping` 中 `sActiveDragView` 提前置空 Bug**：
   确保合成 `ACTION_CANCEL` 在 `STATE_DRAGGING` 下直接忽略，拖拽 View 引用在整个拖拽生命周期中保持有效。
