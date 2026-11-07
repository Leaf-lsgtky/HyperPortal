## HyperPortal 0.1.1

本次主要修三处体感问题：底部光效偏重、右侧触发跑出下侧应用条、边缘自动滚动触发区变矮；另外补上了 ColorOS 风格的拖拽结算动效。

### 拖拽结算动效

- 新增「ColorOS 风格拖拽结算动效」开关（默认开启）：
  - **成功**：阴影在落点原地缩小吸入
  - **未消费**：原地放大消散，不再一路飞回起点
  - 关闭后回到原生动效（成功瞬间消失、未消费飞回起点）
- 修复松手瞬间阴影还在继续形变、甚至胀成大卡片的问题
- 阴影形变（方圆切换、放大）改用 ColorOS 的弹簧参数，手感更接近 OPPO

### 底部光效

- 底部极光按 `glow_effect.coz` 的真实状态机重做，不再用估出来的进度曲线：
  - `process1`：贴底的细边（半径 4.0、间隔 0.18、alpha 1.0）
  - `process2`：300ms 内绽开成 0.76 间隔的大光晕，380ms 时 alpha 淡到 0
- 修正 shader 的旋转方向与纹理环绕模式
- 解决「底部光效比 OPPO 重很多、而且展开后一直很亮」的问题

### 触发位置（右侧 / 底部）

- 修复右侧触发时右侧与下侧同时出现应用条：
  - 拖拽事件广播里的 `position` 不再被渲染端当作配置 —— 这个广播有三个进程在发，其中两个可能带着旧值
  - SystemUI 与 PortalService 只认自己的配置；SystemUI 会把最后一份有效配置缓存到自己的数据目录
  - system_server 在 Hook 装载时就读取配置，并在每次拖拽与设置变更后广播 `ACTION_CONFIG_SNAPSHOT`
- 修复 HyperOS 应用速冻（Greeze）导致配置读取失败、右侧触发退化成底部触发：
  - 第三方应用（酷安、QQ 等）在 HyperPortal 被冻结时解析不到它的 ContentProvider，原来一次失败会锁死 30 秒
  - 现在 Bridge 直接采用 system_server 推送的触发位置，失败重试间隔从 30 秒缩短到 2 秒
- 修复 HyperPortal 进程重建后重复绘制一份悬浮层、与 SystemUI 的那份叠在一起的问题（SystemUI 宿主 pid 持久化，带 boot_id 校验）

### 边缘自动滚动

- 右侧 / 底部的自动滚动触发区不再贴物理屏幕边缘，改为贴 MIUI 手势窗（`GestureStub`）内边界再内缩 8dp
- 最外侧约 37dp 被系统手势窗占用，属于预期行为，不是本应用的判定范围

### 安装包

- `HyperPortal.apk`：主应用与 LSPosed 模块
- `AES-patched.apk`：替换系统 ContentCatcher 的 AES Hook

> 本次为正式版（非 pre-release）。安装新的 AES / HyperPortal 后请重启一次，使 system_server 与 SystemUI 加载新的 Hook。

**Full Changelog**: https://github.com/Leaf-lsgtky/HyperPortal/compare/v0.1.0...v0.1.1
