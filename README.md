# PinyinSearch

在 Minecraft **1.20.1 Fabric** 的创造模式物品栏中，用**全拼**或**首字母**搜索中文物品名。

- 纯客户端模组，服务端不需要安装，也没有任何服务端逻辑。
- 只注入 `CreativeInventoryScreen`，保留原版英文名 / 物品 ID / `#` 标签搜索。
- 内置轻量拼音字典（约 8 KB），**不依赖 pinyin4j**、Fabric API 或其他第三方库。
- 支持多音字，例如“音乐唱片”可用 `yinyue` 或 `yy` 搜到。

## 两个 Minecraft 版本

| 版本 | 目录 | Git 分支 | Java |
| --- | --- | --- | --- |
| Minecraft 1.20.1 | `D:\PinyinSearch` | `main` | 17 |
| Minecraft 1.21 | `D:\PinyinSearch-1.21` | `mc-1.21` | 21 |

两个目录是同一个仓库的两个 worktree。1.21 分支的 Mixin 目标、`fabric.mod.json`
和 Java 版本都不同，不能直接互相覆盖。

## 版本

> 模组版本写在两处：`gradle.properties` 的 `mod_version` 和
> `src/main/resources/fabric.mod.json` 的 `version`。升级版本时请同时修改，
> 以避免 IDEA 对 `processResources` 动态过滤的误报。

| 项目 | 版本 |
| --- | --- |
| Minecraft | 1.20.1 |
| Fabric Loader | 0.16.10 |
| Yarn | 1.20.1+build.10 |
| Loom | 1.10.5 |
| Gradle | 8.14.6 |
| Java | 17 |

> 这是 1.20.1 分支；不要把这些版本改成 1.21。1.21 版本在 `D:\PinyinSearch-1.21`
> 的 `mc-1.21` 分支中。

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

`fabric.mod.json` 放在 `src/main/resources`，因为它是共同资源；Mixin 配置和拼音字典放在 `src/client/resources`，只在客户端加载。

## 文件放置位置

| 文件 | 位置 |
| --- | --- |
| 客户端入口 | `src/client/java/com/pinyinsearch/client/PinyinSearchClient.java` |
| Mixin | `src/client/java/com/pinyinsearch/client/mixin/CreativeInventoryScreenMixin.java` |
| 拼音字典 | `src/client/resources/assets/pinyinsearch/pinyin_dict.txt` |
| 客户端 Mixin 配置 | `src/client/resources/pinyinsearch.client.mixins.json` |
| 模组元数据 | `src/main/resources/fabric.mod.json` |

## 工作方式

1. 原版 `CreativeInventoryScreen#search()` 在 Yarn 1.20.1 中调用
   `SearchProvider#findAll(String)`（官方名 `search`，intermediary `method_4810`）。
2. `CreativeInventoryScreenMixin` 用 `@Redirect` 只替换这一行调用。
3. `PinyinSearchHelper` 先执行原版搜索，再把拼音命中的物品追加进去。
4. 候选物品来自 `ItemGroups.getGroups()` 中每个标签页的 `getSearchTabStacks()`，
   与原版建立搜索索引的来源一致，不会额外塞入生存模式不可获得的物品。
5. 中文名通过 `PinyinDictionary` 转成无调全拼与首字母；多音字会生成多个组合。
6. 拼音结果按匹配质量、名称长度、创造栏顺序排序；英文查询仍由原版返回。

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

## 构建 / 运行

在 IDEA 中直接用 Minecraft Development 插件打开项目，等待 Gradle 同步完成：

```bash
./gradlew build
./gradlew runClient
```

Windows：

```bat
gradlew.bat build
gradlew.bat runClient
```

产物在 `build/libs/pinyinsearch-1.0.0.jar`。

### 关于 BMCLAPI

`settings.gradle` 和 `build.gradle` 都优先使用：

```groovy
maven {
    name = 'BMCLAPI'
    url = 'https://bmclapi2.bangbang93.com/maven'
}
```

BMCLAPI 没有收录的 Fabric 构件（例如 Loom 插件、Yarn）会自动回退到
`https://maven.fabricmc.net/`。

`gradle.properties` 还通过 Loom 的 extra property 把 Minecraft 依赖库、资源文件
和版本清单指向 BMCLAPI：

```properties
loom_version_manifests=https://bmclapi2.bangbang93.com/mc/game/version_manifest_v2.json
```

这里刻意没有覆盖 `loom_libraries_base` 和 `loom_resources_base`：

- BMCLAPI 对部分 Mojang 库会 302 到带签名的 `download.mocdn.net` 地址，Gradle 的 HEAD 请求可能返回 403；
- BMCLAPI 的 `/assets` 在并发下载时可能 TLS 握手重置，导致 `runClient` 的 `downloadAssets` 失败。

因此 Minecraft 依赖库和资源文件仍从官方地址获取，版本清单走 BMCLAPI。
Loom 读取版本清单后，客户端/服务端 jar 的直链仍可能来自 Mojang；如果所在网络访问
`piston-data.mojang.com` 困难，需要自行配置系统代理，或预先下载到 Loom 缓存。

## 排查顺序

1. **Gradle / Loom 版本**
   - 1.20.1 请使用 Loom 1.10.5 + Gradle 8.14.6 + Java 17。
   - 报 `Unsupported class file major version` 时，先检查 Gradle、IDEA、项目 JDK 是否都是 17。
2. **Mixin 注入失败**
   - 日志里出现 `@Redirect` 找不到目标时，先确认 Minecraft 是 1.20.1、Yarn 是 `1.20.1+build.10`。
   - 1.20.1 的目标是 `CreativeInventoryScreen#search()` 和
     `SearchProvider#findAll(Ljava/lang/String;)Ljava/util/List;`。
   - 如果更换 Yarn 版本，用 `javap` 或 IDEA 反编译确认方法名是否变化。
3. **Yarn 映射名称变化**
   - `ItemGroup#getSearchTabStacks()`、`ItemGroups#getGroups()`、
     `ItemStack#getName()` 都是 1.20.1 Yarn 名称；1.20.4/1.21 中部分 API 不同，不能直接套用。
4. **中文名不显示 / 搜不到**
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

只收录 1.20.1 原版中文物品名中出现的汉字，因此体积很小。多音字的多个读音用逗号分隔，
`PinyinProfile` 会在匹配时组合。字典由公开的 [mozillazg/pinyin-data](https://github.com/mozillazg/pinyin-data)
（MIT License）离线生成，运行时不需要联网，也没有引入 pinyin4j。
