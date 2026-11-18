# Oupao Window（欧泡小窗）

ColorOS 16 / 17（Android 16 / 17）上的小窗增强 LSPosed 模块，提供 Flyme 风格的扇形菜单、小窗管理、通知气泡与通知收纳等能力。

> An LSPosed module for ColorOS 16/17 that enhances floating windows with a Flyme-style fan menu, window-count control, notification bubbles and a notification drawer.

## 功能

**扇形菜单**
- 屏幕左下/右下角热区上滑呼出，逐图标从角落淡入、跟手展开
- 固定应用（多套独立配置）、克隆应用角标、长按叠层
- 停留时自动收起：打开多任务 / 通知中心 / 控制中心 / 桌面手势立即收起
- 输入法打开时禁止呼出，避免抢走键盘触摸

**小窗**
- 小窗数量上限（1~10）：上限=1 时串行开窗、自动关闭最早窗口；≥2 时只解除原生上限
- 全屏 → 小窗复用 ROM 过渡动画；平行小窗（全屏二级页 + 小窗主页）
- 双击底部条关闭 / 横屏双击转全屏 / 上滑转迷你窗
- 通知点击、应用间跳转、分享、文本操作等场景可按开关用小窗打开

**多任务与桌面**
- 多任务界面隐藏迷你窗应用（正常小窗保留）
- 重启后静默清空最近任务列表（可选）
- 全局字母索引条（左右侧滑、拼音排序、固定应用分组）
- 「更多应用」悬浮面板 / 小窗页两套呼出方式

**通知**
- 微信 / QQ / 其他应用消息头像气泡，点击直达聊天或小窗
- 通知收纳：长按通知收入收纳箱，支持自动收纳静默通知
- 屏蔽指定/全部应用的悬浮通知

**其他**
- 锁屏数字键盘滑动输入密码
- 长按电源键菜单「软重启」
- 振动完全跟随系统设置；日志上限 8MB，支持一键导出

## 环境要求

- ColorOS 16 / 17（Android 16 / 17，`SDK_INT >= 36`）
- 已安装 **LSPosed**（基于 [libxposed API](https://github.com/libxposed/api) 102+）
- Root（Magisk / KernelSU 等，用于 LSPosed）
- 目标机型/ROM 之外的系统可能不工作，模块内大量逻辑依赖 ColorOS 私有接口

## 安装

1. 安装 `app-release.apk`
2. 在 LSPosed 管理器中启用模块，并勾选作用域：
   - 系统框架（`system`）
   - `com.android.systemui`
   - `com.android.launcher`
   - `com.coloros.smartsidebar`
   - `com.oplus.notificationmanager`（可选，通知收纳设置页入口）
3. **重启手机**（本模块的部分 hook 只会在重启后重新加载）
4. 打开模块 App 按需开启各项功能；新增/修改功能后建议重启

## 构建

```bash
# 需要 JDK 17 与 Android SDK（compileSdk 37）
echo "sdk.dir=/path/to/android-sdk" > local.properties
./gradlew assembleRelease
```

产物：`app/build/outputs/apk/release/app-release.apk`。使用自己的签名配置替换 `app/build.gradle.kts` 中的 release signingConfig。

## 说明与免责

- 本项目为个人学习与研究用途，**不对任何设备损坏、数据丢失或系统不稳定负责**，请自行评估风险。
- 模块通过 LSPosed 在系统进程内 hook 公开/私有 API 实现功能，不修改系统分区。
- 仓库**不包含**任何 Flyme / ColorOS 的专有代码、图标、字体或系统文件；所有实现均为独立编写，仅参考其行为。
- 不收集、不上传任何用户数据；日志仅保存在本机 `/data/system/oupao.log`。
- 本项目与 OPPO / ColorOS / 魅族 / Flyme 官方无任何关联。

## 许可证

[GNU General Public License v3.0](LICENSE)
