# 宝可梦游戏馆

这是一个 Android 游戏馆：用 [LibretroDroid](https://github.com/Swordfish90/LibretroDroid) 和 [mGBA](https://github.com/mgba-emu/mgba) 运行自己导入的 GBA 游戏，并把我们做的 [PokéRogue 在线版](https://github.com/kungfudaibi/pokerogue-live-android)和 [Pokémon Showdown 安卓客户端](https://github.com/kungfudaibi/pokemon-showdown-android-native)放进同一个 APK。装好游戏馆后，首页就能直接进入这两款在线游戏，不用再分别安装它们。

PokéRogue 页面仍从 `pokerogue.net` 加载游戏，会随官网更新；Showdown 的大厅、配队和对战操作是本项目的 Android 界面，规则、匹配和官方战斗动画需要连接 Showdown 服务。两款在线游戏都需要网络。与把游戏内容打进 APK 的 [PokéRogue 离线直装版](https://github.com/1596941391qq/pokerogue-android/releases)不同，这里的 PokéRogue 不能离线游玩。

## 怎么玩

从 [Releases](https://github.com/kungfudaibi/pokemon-arcade-android/releases) 安装 APK。首页可以直接打开 PokéRogue 和 Showdown。GBA 游戏需要自行导入 `.gba` 或包含 `.gba` 的 ZIP；APK 不附带商业 ROM。

GBA 游戏库是竖屏，进入游戏后切到横屏。左侧方向键，右侧 A/B、L/R，下方 START/SELECT。画面右上角可以切换 1/2/4/8 倍速或自定义 1–16 整数倍；实际速度受手机和 ROM 性能影响。A 键轻点一次输入一次，长按约 0.3 秒后连续输入，松手即停；B 键连按可在游戏设置中开启。菜单还可调整画面比例、滤镜、音效、触屏键和五个即时存档槽。

横屏默认铺满手机屏幕，会把 GBA 原本的 3:2 画面拉宽；想保持原比例，可在“游戏设置 → 画面比例”选择“原始 3:2”。每个 GBA 游戏可以导入、导出普通 `.sav`，导入前旧存档会备份为 `.sav.bak`。金手指支持每行 8+4 或 8+8 位十六进制代码；改版 ROM 可能修改内存地址，是否生效要在游戏里确认。

这个版本仍是 demo：GBA 部分还没有联机通信线、补丁管理、自由拖动按键和云同步；Showdown 的双打目标选择等规则仍在完善。

## 原有独立版的数据

游戏馆是一个新的 Android 应用包。之前单独安装的 PokéRogue 和 Showdown 不会被卸载，但它们的 WebView 登录状态和 Showdown 本地队伍不会自动转入游戏馆。PokéRogue 可以在游戏馆里重新登录官网账号；Showdown 的本地队伍需要在新应用里重新导入或配置。GBA 游戏馆本身的升级安装会保留它已导入的 ROM 与存档。

## 构建

需要 JDK 17+ 和 Android SDK 36。设置 `JAVA_HOME`、`ANDROID_HOME` 后运行：

```powershell
.\gradlew.bat :app:assembleDebug
```

APK 在 `app/build/outputs/apk/debug/`。PokéRogue 和 Showdown 的 Android 源码及所需资源已经放在 `app/src/main/`，构建时不用另外下载两个客户端。mGBA 预编译核心来自 [LemuroidCores `fee2e824`](https://github.com/Swordfish90/LemuroidCores/tree/fee2e824525daa22bcf318f96127fe43fa8a15ad)。

## 许可

整合后的 Android 应用源码按 [AGPL-3.0](LICENSE) 发布；原本的 GBA 启动器代码来自本仓库的 GPL-3.0 版本，mGBA 核心保留 [MPL-2.0](MGBA-LICENSE)。PokéRogue 和 Showdown 客户端源代码的对应上游、版本与素材说明见[第三方说明](THIRD_PARTY.md)。本项目不是 Pokémon、PokéRogue 或 Showdown 的官方应用。
