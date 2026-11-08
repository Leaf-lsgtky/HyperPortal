## HyperPortal 0.1.3

0.1.3 集中修复了长按提取的误命中、AES 在 Android 17 上的启动崩溃，以及 WebView/图片目标的原图分辨率问题；首页右上角按钮也改成了可直接重启 SystemUI。

### 长按与拖拽

- 修复微信聊天页长按任意位置都拖出“星标”的问题：微信会保留一个移到屏幕外的浮窗 `MultiTask` 面板，旧逻辑把这个外壳当作命中目标并聚合了面板里的旧文本
- 命中测试、文本/图片递归现在都会跳过 passthrough overlay、完全离屏、零尺寸和透明 View
- 不再对整屏大小的容器做文本/图片聚合，避免把页面里不可见的旧内容当成当前内容
- 长按“按住 说话”时不再触发传送门，避免取消微信录音或把语音直接发出

### AES / Android 17

- `ContentCatcherService` 不再调用隐藏 API `ServiceManager.getServiceOrThrow()`，改用公开的 `ServiceManager.getService()`，修复重签名后的 AES 在 Android 17 / HyperOS 4 上启动即 `NoSuchMethodError` 的问题

### 图片与 WebView

- SystemUI 收到拖拽预览图时仍会在后台下载 `imageUrl` 原图，不再因为已有 360px 预览而跳过原图下载
- 增加 Fresco `SimpleDraweeView -> DraweeHolder -> ImageRequest` 的原始 URL 反射提取，Lofter 等只暴露缩略图 `Drawable` 的页面可以取到原图地址
- 中转站/相册保存继续优先使用原始 staged URI，避免把 360px 预览当成原图

### 首页

- 右上角刷新按钮改为“重启 SystemUI”：优先通过 SystemUI 模块自毁并由系统拉起；模块未响应时回退到 root `kill -9 $(pidof com.android.systemui)`

### 安装包

- `HyperPortal.apk`：主应用与 LSPosed 模块
- `AES-patched.apk`：替换系统 ContentCatcher 的 AES Hook

> 安装新的 AES / HyperPortal 后请重启一次，使 system_server 与 SystemUI 重新加载 Hook。

**Full Changelog**: https://github.com/Leaf-lsgtky/HyperPortal/compare/v0.1.2...v0.1.3
