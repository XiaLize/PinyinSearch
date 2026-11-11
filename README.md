# PinyinSearch

在 Minecraft **Fabric** 的创造模式物品栏中，用**全拼**或**首字母**搜索中文物品名。

同一个项目通过 Git 分支维护四个 Minecraft 版本，不再拆成多个文件夹。

- 纯客户端模组，服务端不需要安装，也没有任何服务端逻辑。
- 只注入 `CreativeInventoryScreen`，保留原版英文名 / 物品 ID / `#` 标签搜索。
- 内置轻量拼音字典，**不依赖 pinyin4j**、Fabric API 或其他第三方库。
- 支持多音字，例如“音乐唱片”可用 `yinyue` 或 `yy` 搜到。

## 版本与分支

| Minecraft | Git 分支 | Java | 拼音字典 | 构建产物 |
| --- | --- | --- | --- | --- |
| 1.20.1 | `main` | 17 | 743 字 | `pinyinsearch-1.0.0+mc1.20.1.jar` |
| 1.21 | `mc-1.21` | 21 | 826 字 | `pinyinsearch-1.0.0+mc1.21.jar` |
| 1.21.2 | `mc-1.21.2` | 21 | 826 字 | `pinyinsearch-1.0.0+mc1.21.2.jar` |
| 1.21.4 | `mc-1.21.4` | 21 | 826 字 | `pinyinsearch-1.0.0+mc1.21.4.jar` |

公共构建版本：

| 项目 | 版本 |
| --- | --- |
| Fabric Loader | 0.16.10 |
| Loom | 1.10.5 |
| Gradle | 8.14.6 |

四个版本都已实际启动验证，Fabric Loader 0.16.10 均可正常加载本模组。

切换版本：

```bash
git switch main        # 1.20.1
git switch mc-1.21     # 1.21
git switch mc-1.21.2   # 1.21.2
git switch mc-1.21.4   # 1.21.4
```

一个工作目录同一时间只显示一个分支，切换后让 IDEA 重新同步 Gradle 即可。
各分支的 Java 版本不同：1.20.1 用 17，1.21 及以上用 21。

> 模组版本同时写在 `gradle.properties` 的 `mod_version` 和
> `src/main/resources/fabric.mod.json` 的 `version`，升级时两处都要改。
> 版本号里的 `+mc1.21.4` 会进入 jar 文件名，用来区分不同 Minecraft 版本。

## 构建 / 运行

先切到目标分支，再执行：

```bash
./gradlew clean build
./gradlew runClient
```

Windows：

```bat
gradlew.bat clean build
gradlew.bat runClient
```

产物在 `build/libs/`，文件名带 Minecraft 版本，例如：

```text
pinyinsearch-1.0.0+mc1.21.4.jar
```

IDEA 中打开项目后：

1. 切到目标分支；
2. `File → Reload All Gradle Projects`；
3. `Settings → Build Tools → Gradle → Gradle JVM` 选对应 JDK；
4. 运行 Gradle 任务 `runClient`，不要用旧的 Application 配置。

## 工作方式

1. 原版 `CreativeInventoryScreen#search()` 调用
   `SearchProvider#findAll(String)`（官方名 `search`，intermediary `method_4810`）。
2. `CreativeInventoryScreenMixin` 用 `@Redirect` 只替换这一行调用。
3. `PinyinSearchHelper` 先执行原版搜索，再把拼音命中的物品追加进去。
4. 候选物品来自 `ItemGroups.getGroups()` 中每个标签页的 `getSearchTabStacks()`，
   与原版建立搜索索引的来源一致，不会额外塞入生存模式不可获得的物品。
5. 中文名通过 `PinyinDictionary` 转成无调全拼与首字母；多音字会生成多个组合。
6. 拼音结果按匹配质量、名称长度、创造栏顺序排序；英文查询仍由原版返回。

1.21 及以上还有一个额外处理：

- 普通搜索使用 `SearchManager#getItemTooltipReloadFuture()` 的 provider；
- `#` 标签搜索使用 `getItemTagReloadFuture()`，并且原版会先去掉 `#` 再调用 `findAll`；
- Mixin 通过比较 provider 实例放行标签搜索，只对普通搜索追加拼音结果。

## 搜索示例

| 输入 | 结果示例 |
| --- | --- |
| `zuanshijian` / `zsj` | 钻石剑 |
| `xmtj` | 橡木台阶 |
| `yinghuamumen` | 樱花木门 |
| `fmjpg` | 附魔金苹果 |
| `yinyue` / `yy` | 音乐唱片 |
| `sanchaji` / `scj` | 三叉戟 |
| `pjys` | 喷溅药水 |
| `钻石剑` | 直接输入中文也可以 |

拼音规则：

- 全拼允许从中间匹配，`shijian` 可以命中“钻石剑”。
- 首字母从名称开头匹配，避免 `zsj` 把“棕色蜡烛”等名称都带出来。
- `ü` 写作 `v`，同时兼容 `u`（如 `nv` / `nu`）。
- 空格、`'`、`-` 会被忽略，`z s j` 等价于 `zsj`。

## 目录结构

```text
PinyinSearch/
├─ build.gradle
├─ settings.gradle
├─ gradle.properties
├─ gradle/wrapper/gradle-wrapper.properties
└─ src/
   ├─ main/
   │  └─ resources/
   │     ├─ fabric.mod.json
   │     └─ assets/pinyinsearch/icon.png
   └─ client/
      ├─ java/com/pinyinsearch/client/
      │  ├─ PinyinSearchClient.java
      │  ├─ mixin/CreativeInventoryScreenMixin.java
      │  └─ search/
      │     ├─ PinyinDictionary.java
      │     ├─ PinyinProfile.java
      │     └─ PinyinSearchHelper.java
      └─ resources/
         ├─ pinyinsearch.client.mixins.json
         └─ assets/pinyinsearch/pinyin_dict.txt
```

`fabric.mod.json` 放在 `src/main/resources`，因为它是共同资源；Mixin 配置和拼音字典放在
`src/client/resources`，只在客户端加载。

## 关于 BMCLAPI

`settings.gradle` 和 `build.gradle` 都优先使用：

```groovy
maven {
    name = 'BMCLAPI'
    url = 'https://bmclapi2.bangbang93.com/maven'
}
```

BMCLAPI 没有收录的 Fabric 构件（例如 Loom 插件、Yarn）会自动回退到
`https://maven.fabricmc.net/`。

`gradle.properties` 通过 Loom 的 extra property 把 Minecraft 版本清单指向 BMCLAPI：

```properties
loom_version_manifests=https://bmclapi2.bangbang93.com/mc/game/version_manifest_v2.json
```

这里刻意没有覆盖 `loom_libraries_base` 和 `loom_resources_base`：

- BMCLAPI 对部分 Mojang 库会 302 到带签名的 `download.mocdn.com` 地址，Gradle 的 HEAD 请求可能返回 403；
- BMCLAPI 的 `/assets` 在并发下载时可能 TLS 握手重置，导致 `runClient` 的 `downloadAssets` 失败。

因此 Minecraft 依赖库和资源文件仍从官方地址获取，版本清单走 BMCLAPI。

## 排查顺序

1. **Java 版本**
   - 1.20.1：Gradle、IDEA、项目 SDK 都用 Java 17。
   - 1.21 / 1.21.2 / 1.21.4：全部用 Java 21。
   - 报 `Unsupported class file major version` 时先检查这一项。
2. **Mixin 注入失败**
   - 先确认当前分支与 Minecraft 版本一致，Yarn 版本分别为：
     `1.20.1+build.10`、`1.21+build.9`、`1.21.2+build.1`、`1.21.4+build.8`。
   - 目标始终是 `CreativeInventoryScreen#search()` 和
     `SearchProvider#findAll(Ljava/lang/String;)Ljava/util/List;`。
3. **`#` 标签搜索**
   - 1.21+ 的原版会在调用 `findAll` 前去掉 `#`，不能靠 `query.startsWith("#")` 判断。
   - 本模组通过 `SearchManager#getItemTagReloadFuture()` 返回的 provider 实例识别标签搜索。
4. **jar 文件名**
   - 文件名由 `mod_version` 决定；不要为了改文件名去改 `archives_base_name`，
     否则 Loom 生成的 refmap 名称会变化，Mixin 配置将找不到 refmap。
5. **中文名不显示 / 搜不到**
   - 确认游戏语言是简体中文。模组按 `ItemStack#getName()` 的当前语言文本建立拼音索引。
   - 如果物品名包含字典未收录的生僻字，该字会被跳过；可在 `pinyin_dict.txt` 中补一行
     `汉字<TAB>pin,yin`。

## 拼音字典

`pinyin_dict.txt` 每行格式：

```text
汉	han
乐	le,yue
重	zhong,chong,tong
```

1.20.1 分支收录 743 个汉字，1.21+ 分支扩展到 826 个，覆盖新物品名（重锤、旋风、
试炼钥匙、犰狳、树脂、铜傀儡像、眼眸花、苍白橡木等）。

多音字的多个读音用逗号分隔，`PinyinProfile` 会在匹配时组合。字典由公开的
[mozillazg/pinyin-data](https://github.com/mozillazg/pinyin-data)（MIT License）离线生成，
运行时不需要联网，也没有引入 pinyin4j。
