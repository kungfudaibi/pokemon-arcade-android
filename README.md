# 宝可梦游戏馆

这个启动器用 [LibretroDroid](https://github.com/Swordfish90/LibretroDroid) 和 [mGBA](https://github.com/mgba-emu/mgba) 玩 GBA 游戏；我们加了游戏库、横屏按键、存档和金手指界面。首页还能打开单独安装的 [PokéRogue 安卓在线版](https://github.com/kungfudaibi/pokerogue-live-android)和 [Showdown 安卓版](https://github.com/kungfudaibi/pokemon-showdown-android-native)。启动器没有打包这两个游戏，也不附带商业 ROM。

## 怎么玩

从 [Releases](https://github.com/kungfudaibi/pokemon-arcade-android/releases) 安装 APK，然后导入自己的 `.gba` 或包含 `.gba` 的 ZIP。游戏库是竖屏，进入游戏会切到横屏：左边方向键，右边 A/B、L/R，下方 START/SELECT。游戏画面右上角可以切换倍速，常用的 1/2/4/8 倍和自定义 1–16 整数倍都能选。菜单里还能调整画面比例、滤镜、音效、触屏键和五个即时存档槽。退出或切到后台时会保存游戏内普通存档。

横屏默认铺满手机屏幕，会把 GBA 原本的 3:2 画面拉宽；若想保持像素比例，在游戏菜单的“游戏设置 → 画面比例”选“原始 3:2”。倍速是目标值，实际能跑到多少受手机和 ROM 性能影响。A 键默认轻点单次、长按约 0.3 秒后连按，松手即停；B 键的长按连按可在游戏设置中开启，频率也可调整。

每个游戏都能导入、导出 `.sav`。如果存档来自悟饭等模拟器，先在原游戏里正常保存，再用它的导出或分享功能找普通存档文件；即时存档和专有格式不一定通用。导入新存档前，启动器会把原 `.sav` 备份为 `.sav.bak`。改版 ROM 的版本最好与存档对应。

金手指可以按游戏保存和开关，支持每行 8+4 或 8+8 位十六进制代码。显示“已启用”只说明代码送进了模拟核心；不同改版可能改过内存地址，效果还得在游戏里确认。建议先备份存档再试。

目前仍是 demo：还没有联机通信线、补丁管理、自由拖动按键和云同步，也没有达到 My Boy 的完整功能。

## 构建

需要 JDK 17+ 和 Android SDK 36。设置 `JAVA_HOME`、`ANDROID_HOME` 后运行：

```powershell
.\gradlew.bat :app:assembleDebug
```

APK 会生成在 `app/build/outputs/apk/debug/`。mGBA 的预编译 Android 核心来自 [LemuroidCores `fee2e824`](https://github.com/Swordfish90/LemuroidCores/tree/fee2e824525daa22bcf318f96127fe43fa8a15ad)，本项目没有修改该文件。

## 许可

启动器源码和 LibretroDroid 按 [GPL-3.0](LICENSE) 发布，mGBA 核心使用 [MPL-2.0](MGBA-LICENSE)。二进制来源和其他素材说明见[第三方说明](THIRD_PARTY.md)。
