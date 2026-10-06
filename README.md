# 宝可梦游戏馆

这个 Android 游戏馆用 [LibretroDroid](https://github.com/Swordfish90/LibretroDroid) 和 [mGBA](https://github.com/mgba-emu/mgba) 运行自己导入的 GBA 游戏，也能下载并启动我们做的 [PokéRogue 在线版](https://github.com/kungfudaibi/pokerogue-live-android)和 [Pokémon Showdown 安卓客户端](https://github.com/kungfudaibi/pokemon-showdown-android-native)。GBA 模拟核心随游戏馆安装；另外两款游戏从各自的 GitHub Release 按需下载、分别更新。

## 开始玩

从[游戏馆 Releases](https://github.com/kungfudaibi/pokemon-arcade-android/releases)安装 APK。首页导入自己的 `.gba` 或包含 `.gba` 的 ZIP，就能玩 GBA 游戏；APK 不附带商业 ROM。

首页的 PokéRogue 和 Showdown 卡片会显示安装状态。第一次点“下载并安装”，游戏馆会从对应项目的 GitHub Release 下载 APK，校验 SHA-256 和应用包名，然后打开 Android 的安装确认页。手机可能先要求允许游戏馆“安装未知应用”；这需要你在系统页面手动允许。装好后回到游戏馆点“启动”。以后两款游戏有新版本时，卡片会显示“更新”，不用重装游戏馆。已安装的独立版也可以直接启动。

PokéRogue 版从 `pokerogue.net` 加载游戏，官网网页更新通常无需更新模块；它需要联网。若想离线玩，可看[另一作者的离线直装版](https://github.com/1596941391qq/pokerogue-android/releases)。Showdown 的 Android 界面由我们制作，规则、匹配和部分战斗资源依赖官方服务，也需要联网。两个模块分别由其原项目维护，数据保存在各自的 Android 应用中；更新游戏馆不会清除它们的数据。

GBA 游戏库是竖屏，进入游戏后切到横屏。左侧方向键，右侧 A/B、L/R，下方 START/SELECT。画面右上角可以切换 1/2/4/8 倍速或自定义 1–16 整数倍；实际速度受手机和 ROM 性能影响。A 键轻点一次输入一次，长按约 0.3 秒后连续输入，松手即停；B 键连按可在游戏设置中开启。菜单还能调整画面比例、滤镜、音效、触屏键和五个即时存档槽。

横屏默认铺满手机屏幕，会把 GBA 原本的 3:2 画面拉宽；想保持原比例，可在“游戏设置 → 画面比例”选择“原始 3:2”。每个 GBA 游戏可以导入、导出普通 `.sav`，导入前旧存档会备份为 `.sav.bak`。金手指支持每行 8+4 或 8+8 位十六进制代码；改版 ROM 可能修改内存地址，是否生效要在游戏里确认。

这个版本仍是 demo：GBA 部分还没有联机通信线、补丁管理、自由拖动按键和云同步；Showdown 的双打目标选择等规则仍在完善。

## 从三合一版升级

v0.3.0 曾把两款客户端直接编进游戏馆。v0.4.0 改为分别安装模块，因此上一版游戏馆里的 PokéRogue 登录状态和 Showdown 队伍不会自动迁到独立模块；已经单独安装过这两款客户端的手机则继续使用它们原有的数据。覆盖安装游戏馆会保留它的 GBA ROM 与存档。

## 构建与许可

需要 JDK 17+ 和 Android SDK 36。设置 `JAVA_HOME`、`ANDROID_HOME` 后运行：

```powershell
.\gradlew.bat :app:assembleDebug
```

APK 在 `app/build/outputs/apk/debug/`。游戏馆源码与 LibretroDroid 按 [GPL-3.0](LICENSE) 发布，mGBA 核心保留 [MPL-2.0](MGBA-LICENSE)。PokéRogue 和 Showdown 模块各自按 AGPL-3.0 发布；来源及其他素材说明见[第三方说明](THIRD_PARTY.md)。本项目不是 Pokémon、PokéRogue 或 Showdown 的官方应用。

发布模块新版时，需要在对应项目里提高 Android `versionCode`／`versionName`，沿用同一签名密钥，并把 APK 发到该项目的 GitHub Release。游戏馆读取 Release 中的 APK 与 SHA-256 摘要后，就能显示新版本。
