## HyperPortal 0.0.6

> 草稿：下一次发版时使用，条目可能继续追加。

### 动图保存与分享

- 微博大图页的 GIF 可以原样保存了（实测 19.8 MB、完整帧，不再是首帧静图）
- 同一张图经 Glide 渲染时也能取回：从内存里的编码缓冲区直接还原（实测与源文件字节数一致，19,795,318 B）
- 动图探测不再限定应用：只要有动图 drawable（`*GifDrawable` / `*AnimatedDrawable` / `*MovieDrawable` / `*WebpDrawable` / `*ApngDrawable`）就会尝试取回原始文件，微博、Qzone 这类非腾讯系应用也能覆盖
- 取回时优先用真实文件；只有拿不到路径时才落到「重建临时文件」，且临时文件只保留最近几份，不会堆积占用空间

### 已知限制

以下几个位置的动图**仍然只能得到静图**，属于渲染方式限制：

- 微信「表情包」「小程序」整页预览（Flutter 渲染，画面在独立 surface 上）
- QQ 表情包网格缩略图（新版 Libra 图库解码后丢弃编码数据，本地也不缓存）
- 小红书实况图（是 MP4 视频，不属于动图）

同一个表情在 QQ 里点开大图再拖，可以存成动图。

### 安装包

- `HyperPortal.apk`：主应用与 LSPosed 模块
- `AES-patched.apk`：替换系统 ContentCatcher 的 AES Hook

> 本次为正式版（非 pre-release）。安装新的 AES / HyperPortal 后请重启一次，使 system_server 与 SystemUI 加载新的 Hook。

**Full Changelog**: https://github.com/Leaf-lsgtky/HyperPortal/compare/v0.0.5...v0.0.6
