## HyperPortal 0.0.3

### 中转站

- 新增 Miuix 风格操作弹窗
- 拖出中转站时面板与悬浮条自动让位
- 优化胶囊、面板与底部安全区布局

### 内容识别

- 新增图片「文本分词」目标，接入小米澎湃 AI 引擎 OCR
- 支持图片长按后直接进行文本分词、复制与搜索
- 修复拖拽预览在未选择分享目标时重复写入相册的问题

### 触发与兼容

- 修复传送门开关关闭后 AES 触摸管线被框架拦截的问题
- 普通应用 UID 不再依赖传送门保持开启
- 过滤内部 drag_file 标签，避免错误内容进入目标列表

### 安装包

- `HyperPortal.apk`：主应用与 LSPosed 模块
- `AES-patched.apk`：替换系统 ContentCatcher 的 AES Hook

> 本次为正式版（非 pre-release）。安装新的 AES / HyperPortal 后请重启一次，使 system_server Hook 生效。

**Full Changelog**: https://github.com/Leaf-lsgtky/HyperPortal/compare/v0.0.2...v0.0.3
