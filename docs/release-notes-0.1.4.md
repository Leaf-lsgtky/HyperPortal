## HyperPortal 0.1.4

0.1.4 修掉了「拖图片到微信提示"获取资源失败"」这条链路（只出现在 SystemUI 与 system_server 共享
`android.uid.system` 的机型上），顺带收敛了分享授权面、补上了模块热重载，并把这一轮滚动性能优化
带来的长按回归修回去。

### 分享：修复「获取资源失败」

- 根因：HyperOS 的 `UriGrantsManagerService` 拒绝任何由 **core uid 1000** 发起的 uri 授权；出厂
  Android 16 / 部分 HyperOS 4 机型（如 vermeer / OS4.0.29.0）的 SystemUI 就运行在
  `android.uid.system`（uid 1000）里，于是暂存到 `com.android.systemui.fileprovider` 的拖拽文件
  **无法授权给目标 App**，微信报「获取资源失败」，酷安直接 `SecurityException` 崩溃
- 修复：在 system_server 内放行自己两个 provider 的 core-uid 授权
  （`com.android.systemui.fileprovider`、`com.github.leaf.hyperportal.files`），命中条件是
  「authority 属于我们 + 发起方是 core uid」，其它校验（provider 存在、`grantUriPermissions`、
  path pattern、目标 uid）仍由框架执行
- 同时修掉了 `Bridge.copyPfdToPortal()` 的跨进程写入（它依赖 SystemUI 的前缀授权），这类机型上
  不再退化成只能分享缩略图
- SystemUI 自己在 uid 10231（`android.uid.systemui`）的机型行为不变

### 分享：授权面收敛

- 不再在暂存阶段给 31 个「常见分享目标」预授权；只在真正投放时对**唯一目标**授权
  （系统分享面板与中转站的拖出/剪贴板导出仍然保留兜底，因为目标在那时才确定）
- 每一个拖拽文件从 31 条 `UriPermission` 降到 1 条；uid 1000 机型上也不再每次刷新 31 条被拒日志

### 长按与滚动

- WebView/prefetch 的视图树遍历延后到长按阈值（原来的 150ms 提前量仍会拖慢深树页面的滚动起始）
- `AndroidViewsHandler` / Compose interop 判定改为按 class 缓存（含否定结果），命中测试不再每次
  枚举父类方法
- 无参 getter 反射改为定向查找 + 命中/未命中缓存
- 运行时配置 IPC 移出触摸派发路径
- **拖拽内容源改为「长按那一刻的节点 + 实时坐标」**：不再在 ACTION_DOWN 时缓存节点。此前的缓存
  做法在多图场景会把整块九宫格当成一个 View（微博 `FeedBlogItemMixView` 自绘全部格子），因此
  只能取到第一张图；现在按坐标命中具体那一格
- 已知取舍：App 在长按期间自己弹出菜单覆盖内容时，不再有「DOWN 快照」兜底；后续若复现，会改为
  在长按时刻用坐标重新命中，而不是缓存 View

### 开发工具

- libxposed API 102 热重载：`am broadcast -a com.github.leaf.hyperportal.HOT_RELOAD -n
  com.github.leaf.hyperportal/.HotReloadReceiver` 可把改好的模块热重载进运行中的 system_server /
  SystemUI（换 APK 本身不会触发），改代码不再必须重启
- 授权自检：`am broadcast -a com.github.leaf.hyperportal.GRANT_SELFTEST --es target
  com.tencent.mm --es file <暂存文件名>`，由 system_server（uid 1000）实测一次授权并打印
  `landed=true/false`

### CI

- 加固 gradle wrapper 下载流程

### 安装包

- `HyperPortal.apk`：主应用与 LSPosed 模块
- `AES-patched.apk`：替换系统 ContentCatcher 的 AES Hook（构建产物
  `ContentCatcher-patched.apk` 重命名上传，沿用历史发行包名）

> 安装新的 AES / HyperPortal 后请重启一次，使 system_server 与 SystemUI 重新加载 Hook。

**Full Changelog**: https://github.com/Leaf-lsgtky/HyperPortal/compare/v0.1.3...v0.1.4
