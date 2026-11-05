## HyperPortal 0.0.4

### 首页与设置

- 新增「首页」，用状态卡片直接回答「现在能不能用」：已激活 / 等待首次 AES 握手 / 已暂停 / 无法确认
- 明细行分别给出功能总开关、LSPosed 框架版本、system_server 钩子、系统界面钩子、AES 握手（最近握手的应用与时间）
- 原来的设置界面移到「设置」页，底部导航切换，返回键先回首页
- LSPosed 服务未连接时显示「无法确认」，不再误报为「未激活」

### 拖拽触发

- 修复冷启动后第一次拖拽只出现倾斜动画、没有光效和应用条，随后系统界面被 ANR 杀掉并重启的问题
- 配置读取与目标列表构建移出 SystemUI 主线程，避免首次拖拽阻塞输入分发
- 系统界面被重启后传送门不再「隐身」：SystemUI 进程消失后 PortalService 会重新接管

### 未适配窗口

- 拖到不支持原生拖拽的窗口时，回退为目标应用自己的分享入口（与 OPPO 行为一致），例如 QQ 的「发送给好友 / 发送到电脑」
- 覆盖层、系统窗口、同应用窗口、`file://` 与混合 MIME 不触发回退
- 可用 `persist.sys.hyperportal.drop_to_share_enable=0` 关闭

### 倾斜动画

- 修复从中转站拖出时把下方应用整个倾斜的问题（SystemUI 托管的拖拽源没有可倾斜的 task）
- 修复拖拽源窗口解析失败、进而回退到「最顶层全屏任务」的问题

### 图片分享

- 修复图片拖到微信聊天窗口后发送失败的问题（provider 的 `_data` 与 MIME 类型）
- 微博「发送给朋友」可以识别拖入的图片

### 安装包

- `HyperPortal.apk`：主应用与 LSPosed 模块
- `AES-patched.apk`：替换系统 ContentCatcher 的 AES Hook

> 本次为正式版（非 pre-release）。安装新的 AES / HyperPortal 后请重启一次，使 system_server 与 SystemUI 加载新的 Hook。

**Full Changelog**: https://github.com/Leaf-lsgtky/HyperPortal/compare/v0.0.3...v0.0.4
