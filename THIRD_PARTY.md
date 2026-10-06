# 第三方代码与素材

本仓库公开游戏馆源码和构建所用的两个未修改 mGBA 核心文件。整合后的 Android 应用以 AGPL-3.0 发布；已有组件及素材各自的权利与许可仍保留。

| 组件 | 在本项目中的用途 | 许可与对应来源 |
| --- | --- | --- |
| [LibretroDroid 0.9.0](https://github.com/Swordfish90/LibretroDroid/tree/0.9.0) | Android 上运行 Libretro 核心的库，经 Gradle 获取 | [GPL-3.0](https://github.com/Swordfish90/LibretroDroid/blob/0.9.0/LICENSE)；本项目没有修改该库 |
| [mGBA](https://github.com/mgba-emu/mgba) | GBA 模拟核心 | [MPL-2.0](MGBA-LICENSE)，另见上游各文件声明 |
| [LemuroidCores `fee2e824`](https://github.com/Swordfish90/LemuroidCores/tree/fee2e824525daa22bcf318f96127fe43fa8a15ad/lemuroid_core_mgba) | 提供本 APK 中 mGBA 的预编译 Android 核心 | 二进制按原文件复制，没有本地补丁；核心源码项目见 [mGBA](https://github.com/mgba-emu/mgba) |
| [PokéRogue 安卓在线版 `61db37b`](https://github.com/kungfudaibi/pokerogue-live-android/tree/61db37b3b0867b11a759f9ad86eb166c2f1086c3) | 内置 WebView 页面，运行时加载 PokéRogue 官网 | [AGPL-3.0](https://github.com/kungfudaibi/pokerogue-live-android/blob/61db37b3b0867b11a759f9ad86eb166c2f1086c3/LICENSE)；官网游戏及素材仍遵循其自己的许可 |
| [Pokémon Showdown 安卓客户端 `23f40a8`](https://github.com/kungfudaibi/pokemon-showdown-android-native/tree/23f40a8a79243c96a173b07a7fabbfdcd0d40b43) | 内置大厅、配队和战斗 Activity，运行时连接官方服务及加载官网动画 | [AGPL-3.0](https://github.com/kungfudaibi/pokemon-showdown-android-native/blob/23f40a8a79243c96a173b07a7fabbfdcd0d40b43/LICENSE)；其他来源见[该客户端第三方说明](https://github.com/kungfudaibi/pokemon-showdown-android-native/blob/23f40a8a79243c96a173b07a7fabbfdcd0d40b43/THIRD_PARTY.md) |

本仓库的 `app/src/main/jniLibs/arm64-v8a/libmgba_libretro_android.so` SHA-256 为 `FBB03681B7D7EA4FA04BAC0F60A005D9FCDC732ECE3DF1B715FABC19D1F9B2AB`；`x86_64` 文件为 `3ED96EB2299AC97E9400C2DDBD111C4877D2C5F4F1E2B9A22FBA1845F1F16007`。若需自行重建原生核心，请从上游源码和构建环境入手；LemuroidCores 提供的是预编译文件，其仓库未在此提交中标明 mGBA 源码的精确修订号。因此我们不声称当前二进制可由某个已核实的 mGBA 提交逐字节复现。

`app/src/main/assets/` 内的 Showdown 标题图、名称表和网页加载页来自上述 Showdown 客户端；PokéAPI 名称资料的许可文本随包保存在 `app/src/main/assets/POKEAPI_LICENSE.txt`。`app/src/main/res/drawable-nodpi/` 的入口图案仅用于识别对应游戏。Pokémon、PokéRogue、Showdown 的名称、角色与相关美术权利归各自权利人；本项目没有获得官方授权。APK 不包含商业 GBA ROM。
