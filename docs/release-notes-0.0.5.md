## HyperPortal 0.0.5

### 动图保存与分享

拖动会动的表情，存下来的也是动图了。

- 微信聊天里的动态表情：直接取回原始 GIF（约 700 KB 全保真），不再变成一张 530×530 的静图
- QQ 点开的动图大图：取回原始 GIF（512×512、2.6 MB，帧数与源文件一致）
- 分享给好友、保存到相册、复制到剪贴板、拖进中转站——四条路径都原样交付同一个文件，不再各自重新编码

技术上有三处是必需的：

- 从源应用自己的表情缓存里取回原始文件（微信 `MicroMsg/*/emoji/<md5>`、QQ `.emotionsm/<包>/<md5>`），而不是只拿屏幕上那一帧
- 识别被改写过文件头的动图（有的表情把 `GIF89a` 存成 `GHF99\``），取回时自动修复
- 取不到文件时不再一律降级为 PNG，只在源确实是静图时才那样做

### 已知限制

以下几个位置的动图**仍然只能得到静图**，这是渲染方式的限制，不是遗漏：

- 微信「表情包」「小程序」整页预览（Flutter 渲染，画面在独立 surface 上，视图树里没有内容）
- QQ 表情包网格缩略图（新版 Libra 图库把动图解码成帧后丢弃了编码数据，本地也不缓存）
- 小红书实况图（是 MP4 视频，不属于动图，留待后续单独支持）

同一个表情在 QQ 里点开大图再拖，是可以存成动图的。

### 其他

- 修复图片 MIME 识别：现在按文件头判断，`BitmapFactory` 认不出的格式也能正确定型
- 新增荣耀「GIF 动图拖拽」的机制研究与移植可行性分析（`docs/`）

### 安装包

- `HyperPortal.apk`：主应用与 LSPosed 模块
- `AES-patched.apk`：替换系统 ContentCatcher 的 AES Hook

> 本次为正式版（非 pre-release）。安装新的 AES / HyperPortal 后请重启一次，使 system_server 与 SystemUI 加载新的 Hook。

**Full Changelog**: https://github.com/Leaf-lsgtky/HyperPortal/compare/v0.0.4...v0.0.5
