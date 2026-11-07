## HyperPortal 0.1.2

0.1.2 把传送门运行时完整收进 SystemUI：App 侧不再常驻悬浮窗、前台服务或保活广播，拖拽链路只保留 SystemUI 的 Glow / 应用条 / 文件暂存。与此同时修复了迁移过程中暴露的长按失效、配置默认值覆盖、首页握手状态和暂存文件被清理等问题。

### SystemUI-only 运行时

- 删除 App 侧的 `PortalService`、`PortalReceiver` 和旧 `incoming/` 文件服务，`PortalFileProvider` 只保留配置引导端点
- 拖拽内容、Staging FileProvider、运行时配置与 AES 握手全部由 `PortalSystemUiModule` 托管
- 去掉 App 自启动、前台服务和保活 Hook，减少 HyperOS 速冻/后台限制对拖拽的影响
- `system_server` 只负责 3D 倾斜与配置快照，不再需要为 App 侧悬浮层做唤醒

### 拖拽与配置

- 修复长按门禁把 AES 的 `Content.mCatchType=4` 当成非长按、导致完全起不了拖拽影子的问题
- `config_ready` 现在被 `Bridge`、`SystemUI` 和 `system_server` 一致遵守；配置缓存未就绪时不再用 `bottom/76%/master=true` 覆盖用户设置
- 修复首次启动或 HyperPortal 被冻结时右侧触发退化成底部触发、总开关失效、长按时长/黑名单不生效的问题
- `syncToSystemUi()` 改为定向广播，不再每次打开首页都唤醒所有 AES App
- 首页 `__status__` 在 SystemUI 启动时显式授权给 HyperPortal，AES 握手状态可以正常合并回首页

### 文件与中转站

- 拖拽文件只通过 SystemUI 的 FileProvider 暂存；来源 App 首次写入前会自动申请 prefix 授权
- 中转站优先保存 staged 原始文件，不再把 360px 拖拽预览图当作原图；GIF、视频和 4K 图片保持原始字节与分辨率
- 中转站文件复制到自己的 `mediaDir` 并生成缩略图，不再依赖会被 LRU 清理的 SystemUI cache URI
- 暂存缓存清理增加 3 分钟保护，避免刚分享/刚拖出的文件被下一次 staging 删掉
- 保存相册、存中转站和图片 OCR 解码移到后台线程，避免阻塞 SystemUI 输入分发

### 安全与稳定性

- `ACTION_REQUEST_GRANT` 校验发送方 UID 与包名，不能替任意包授权
- `__status__` 只允许 HyperPortal / 系统读取
- AES 握手记录增加 60 秒节流，减少 SystemUI SharedPreferences 写入
- 清理已删除组件的 ProGuard 规则

### 安装包

- `HyperPortal.apk`：主应用与 LSPosed 模块
- `AES-patched.apk`：替换系统 ContentCatcher 的 AES Hook

> 安装新的 AES / HyperPortal 后请重启一次，使 system_server 与 SystemUI 重新加载 Hook。

**Full Changelog**: https://github.com/Leaf-lsgtky/HyperPortal/compare/v0.1.1...v0.1.2
