# 宝可梦游戏馆（Android demo）

本项目以 [LibretroDroid](https://github.com/Swordfish90/LibretroDroid) 的 Android 模拟器前端和 [mGBA](https://github.com/mgba-emu/mgba) 的 GBA 核心为基础，**新增**游戏库、横屏触屏按键、存档与金手指界面；另把本账号开发的 [PokéRogue 实时网页版](https://github.com/kungfudaibi/pokerogue-live-android) 和 [Showdown 原生实验版](https://github.com/kungfudaibi/pokemon-showdown-android-native)作为独立应用入口。没有修改或打包原版 PokéRogue、Showdown 服务端，也没有附带商业游戏 ROM。本项目与这些游戏的官方团队无关。

启动器只负责打开另外两个 APK，不能代替安装它们。GBA 核心的预编译文件来自 [LemuroidCores `fee2e824`](https://github.com/Swordfish90/LemuroidCores/tree/fee2e824525daa22bcf318f96127fe43fa8a15ad)，本项目没有改动该二进制文件。

## 试玩

安装 `app/build/outputs/apk/debug/app-debug.apk`。从首页导入自己的 `.gba` 或包含 `.gba` 的 `.zip`，点“开始游戏”。GBA 原生画面是横向 3:2，游玩界面固定横屏；方向键与 A/B、L/R 在画面两侧，START/SELECT 在画面下方。菜单可选 1/2/4 倍速、四种滤镜、音效、隐藏触屏键、重启、五个即时存档槽。退出游戏或切到后台时会保存游戏内普通存档。若已安装本机的 Showdown 和 PokéRogue 安卓客户端，首页也可以直接打开它们。

连接的测试手机游戏库里另放了 [Snek GBA 自制贪吃蛇](https://github.com/LeonarthCG/Snek-GBA) 供试玩（ROM 没有打包在 APK 中）：标题页用左右方向键调整速度，按 START 开始；游戏里用方向键转向，START 暂停。

每个游戏的“存档管理”支持导入和导出 `.sav`。导入前会将已有 `.sav` 备份为 `.sav.bak`，导入后重新进入游戏。请先在原模拟器中完成一次**游戏内保存**，再找普通存档；即时存档或悟饭自己的加密容器不一定能直接互通。ROM 改版的版本及补丁应与存档对应。

## 悟饭存档的查找

无需 root，可以先在悟饭的游戏页面查看“存档管理 / 备份 / 导出 / 分享”是否有导出普通存档。也可把手机接到电脑并允许 USB 调试，再检查手机共享存储里以 `.sav`、`.srm` 为扩展名的文件及悟饭的应用目录。Android 对应用私有目录有访问限制；没有导出功能时，USB 调试通常也读不到私有文件。不要卸载悟饭或清除它的数据。

## 已实现与待做

- 已实现：GBA ROM/ZIP 导入、横屏游玩、触屏和实体手柄按键、普通存档导入导出、五槽即时存档、1/2/4 倍速、滤镜、音效与触屏键开关、按键大小和透明度、游戏重命名、按 ROM 保存的金手指列表、两个在线游戏入口。
- 待做：按键自由拖动、联机/通信线、手柄自定义映射、IPS/UPS/BPS 补丁管理、封面库、云同步。当前 demo 尚未达到 My Boy 的完整功能和成熟度。

金手指菜单接受每行 8+4 或 8+8 位的十六进制代码，可为同一条金手指输入多行。代码按具体 ROM 的哈希分别保存；启停时会重置核心金手指并重新应用已启用的条目。**代码显示为“已启用”只表示已送入核心，并不证明它对当前改版有效。**不同改版可能改动地址；请先备份普通存档，再逐条在游戏内确认效果。[mGBA libretro 核心的实现](https://github.com/mgba-emu/mgba/blob/master/src/platform/libretro/libretro.c)对代码格式有明确限制，不能把任意网上的金手指都视为兼容。

## 构建

需要 JDK 17+ 和 Android SDK 36。此电脑使用以下环境，缓存放在 F 盘：

```powershell
$env:JAVA_HOME='D:\Android_stdio\jbr'
$env:ANDROID_HOME='D:\Android_SDK'
$env:GRADLE_USER_HOME='F:\code\pokemon-launcher\.gradle'
.\gradlew.bat :app:assembleDebug
```

本项目自写代码及 [LibretroDroid 0.9.0](https://github.com/Swordfish90/LibretroDroid/tree/0.9.0) 按 [GPL-3.0](LICENSE) 发布。[mGBA 核心源码](https://github.com/mgba-emu/mgba)使用 [MPL-2.0](THIRD_PARTY.md)；预编译核心原样取自上述 LemuroidCores 提交。第三方代码、二进制、图标及其源码位置详见 [第三方声明](THIRD_PARTY.md)。本仓库的 GPL 声明不覆盖第三方各自的许可，也不授予任天堂或 Pokémon 的商标与素材权利。
