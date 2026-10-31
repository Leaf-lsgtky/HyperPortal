# ohmyAES (DragShare)

> 将 ColorOS 全局流体云 / 内容传送门（DragShare）完整的交互与动效体验带到小米 HyperOS！
> 包含系统级多模态内容识别、ColorOS 极光光效着色器、底部重排变形动画、以及基于 LSPosed 的底层 3D 空间倾斜动画。

[English](README.md) | [简体中文](README_CN.md)

---

## 🌟 核心特性

- 🎯 **系统级多模态内容提取 (AES Bridge)**
  - 基于小米 HyperOS 特权服务 `com.miui.contentcatcher` 注入，**免去每次触控的繁琐弹窗**。
  - 深度支持 **传统 View 树**、**Jetpack Compose 语义树**、**AndroidViewsHandler 嵌套混合布局**（完美穿透酷安动态正文与图片、微信、微博、小红书、Telegram 等）。
  - 支持复杂图文、Web 网页正文、文件描述符 (`ParcelFileDescriptor`)、多图高清原图快速提取。

- 🌈 **ColorOS 原生质感极光流光 (Aurora GlowView)**
  - 采用 **AGSL (Android Graphics Shading Language)** 自定义 `RuntimeShader` 实现 1:1 动态流动极光。
  - 动态多频扰动与平滑进度过渡：上屏滑动唤起温和光晕，进入热区触发激进汇聚。

- 📱 **底部自适应中转抽屉与重排动画**
  - **动态拖拽阴影重排 (System Shadow Morph)**：当拖拽物滑入底部热区（阈值锁定 76%），系统拖拽阴影实时平滑形变为圆角矩形卡片并微缩，底部应用抽屉同步升起。
  - **回弹与飞回保证 (Native Fly-back Restoration)**：中途松手时严格回退 Android 原生 WMS 取消飞回动画，无缝飞回原文字/图片位置。

- 📐 **LSPosed `system_server` 3D 空间倾斜 (3D Tilt Transform)**
  - 深度 Hook Android 底层窗口管理器服务 `com.android.server.wm.DragState`。
  - 利用 `SurfaceControl.Transaction.setMatrix` 直接在 SurfaceFlinger 层面施加真实 3D 旋转矩阵，随手指拖拽倾斜变幻。

---

## 🏗️ 项目架构

```
ohmyAES/
├── aes/                               # Application Extension Service 挂载组件
│   ├── src/                           # 注入到 AES classes2.dex 的核心 Java 源码
│   │   └── dev/dragshare/catcher/
│   │       └── Bridge.java            # 内容深层穿透提取、触控分发、热区重排动画
│   ├── smali_project/                 # 宿主 Smali 项目（由 apktool 组装 classes.dex）
│   ├── base/                          # 官方基础包 (Application Extension Service_5.1.7.apk)
│   └── patches/                       # 自动化 Smali 注入脚本
│
├── bridge/                            # 桥接伴侣应用 & LSPosed 系统模块
│   ├── AndroidManifest.xml
│   ├── src/
│   │   └── com/dragshare/portal/
│   │       ├── MainActivity.java      # 设置与悬浮窗权限入口
│   │       ├── PortalService.java     # 极光 GlowView、底部目标抽屉、拖拽目标拦截
│   │       ├── PortalReceiver.java    # 跨进程状态广播同步
│   │       ├── PortalFileProvider.java# 跨应用安全文件共享 Provider
│   │       └── system/
│   │           └── PortalSystemModule.java # LSPosed 模块（Hook system_server 实现 3D 倾斜）
│   ├── assets/
│   │   └── oppo_glow.png              # 极光噪波纹理
│   ├── resources/
│   │   └── META-INF/xposed/           # LSPosed 现代模块声明与作用域
│   └── libs/                          # 编译期 Xposed API 依赖与 Stubs
│
├── tools/                             # 统一构建与一键部署基础设施
│   ├── config.py                      # Android SDK、Build-Tools、Apktool 自动探测
│   ├── build.py                       # 极速双目标构建器（纯源码构建，秒级产出）
│   ├── deploy.py                      # 一键安装、LSP 路径刷新、软重启、日志监听
│   └── repack.py                      # 二进制 AXML versionCode 修改与 DEX 合并
│
├── docs/                              # 逆向工程与技术实现文档
│   ├── ARCHITECTURE.md                # 总体设计与通信协议
│   ├── COLOROS_VIEWDRAG_SPEC.md       # ColorOS 拖拽交互与动效规范
│   └── COLOROS_COMPOSE_EXTRACTION_SPEC.md # Compose 语义层深度提取规范
│
├── signing/                           # 签名密钥
└── dist/                              # 构建产物输出目录
```

---

## 🚀 极速构建与开发

项目提供了开箱即用的现代化基础设施，**无需手动编写补丁脚本或配置复杂的 Gradle 环境**：

### 1. 环境准备
- Python 3.8+
- JDK 11+
- Android SDK (包含 `build-tools` 与 `platforms`)

### 2. 统一构建命令

```bash
# 一键构建全部产物 (DragShareBridge.apk + ContentCatcher-patched.apk)
python tools/build.py all

# 仅构建伴侣应用与 LSPosed 模块
python tools/build.py bridge

# 仅构建 AES 特权注入包
python tools/build.py aes

# 清除所有中间缓存
python tools/build.py clean
```

产物将输出至 `dist/` 目录：
- `dist/DragShareBridge.apk`
- `dist/ContentCatcher-patched.apk`

---

## 📲 一键部署与调试

```bash
# 一键安装双 APK 并刷新 LSPosed 模块配置
python tools/deploy.py --install all --refresh-lsp

# 快速软重启 (重启 system_server，耗时约 5 秒，用于使 3D 倾斜模块生效)
python tools/deploy.py --soft-reboot

# 实时监听抓取相关日志
python tools/deploy.py --logs
```

---

## 📄 开源许可证

本项目采用 [Apache License 2.0](LICENSE) 许可协议。
