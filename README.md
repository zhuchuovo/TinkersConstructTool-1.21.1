# Tinkers' Construct Tool / 匠魂材料编辑器

匠魂（Tinkers' Construct 1.21.1, NeoForge）的游戏内材料编辑器。按 **Shift+Z** 打开。

本模组只是编辑器：材料、词条与强化使用匠魂标准格式的数据包/资源包；新流体通过独立 JAR 或 KubeJS 启动脚本注册。**导出后卸载本模组也能正常使用**。

## 功能

| 页面 | 作用 |
| --- | --- |
| 基础 | ID、等级、排序、部件台制作物品（物品或 `#标签`）、可选熔铸流体、游戏内预览、获取测试工具 |
| 属性 | 头部 / 手柄 / 绑定结 / 弓臂 / 握把 / 各部位镀层 / 锁链甲 / 史莱姆套 / 弹药…… 开关即启用，数值实时生效 |
| 特性 | 强化槽（升级/能力/防御/灵魂槽可加可减，每件工具只算一次）；默认特性、按类别（近战采掘/远程/护甲/弹药）或单个部件覆盖；可使用自定义词条 |
| 纹理 | 拖动调色板改颜色、一键配色、基础样式、发光；特效图层（染色/噪点/条纹/格纹/闪点/渐变/描边/色相/对比度/纹理叠加）可限定暗部/中间调/亮部/边缘；动画（流光/呼吸/彩虹/闪烁/流动） |
| 介绍 | 名称、短句、介绍，编辑当前界面语言的文本（简体中文 / 繁體中文 / English / 日本語） |
| 词条 | 自定义强化属性，例如「每级 +2 攻击伤害、-10% 攻击速度、+1 升级槽、攻击时给目标中毒 5 秒」，作为特性加到材料上。药水效果可选：攻击时→目标、受击时→攻击者、穿戴时攻击→目标、使用时→自己，等级/时长/几率都可随等级增长 |
| 强化 | 把词条做成工匠台/工匠砧配方：指定 5 个槽位各放什么材料（从青金石槽顺时针 1~5，悬停「?」看编号）、占用槽位、最高等级，以及哪些装备可用/禁用 |
| 流体（第四个主分类） | 新建流体，设置 ID、名称、颜色、静止/流动纹理、温度（K）、密度、黏度、发光、桶与可放置开关；可在材料熔铸流体选择器中引用 |

流体的「纹理」页可像材料一样编辑调色板、特效图层和动画，提供水纹、熔融、黏液、油、蜂蜜、云雾、晶体七种基础样式，并可设置纹理尺度、随机种子和不透明度。静止与流动纹理实时预览，生成的 PNG 和动画元数据自动随导出保存。新流体默认启用生成；已有工程保留原来的外部纹理引用，可在纹理页切换。

- **导入**：以任意已加载的材料为模板（数值、特性、制作物品、调色板、多语言文本）。
- **Ctrl+S 保存**：只保存工作区，不重载。
- **应用（Ctrl+Shift+S）**：保存并热重载，背包里的工具会立即刷新；纹理和文本改动会刷新资源。「自动应用」默认关闭，打开后数值改动约 1 秒自动生效。
- **界面语言**：右上角齿轮里可选 跟随游戏 / 简体中文 / 繁體中文 / English / 日本語，只影响编辑器（包括匠魂和原版的名称），关闭编辑器后恢复游戏语言。「介绍」页编辑的就是这个语言的文本。流体脚本的注释名称使用 Minecraft 游戏选项中选择的语言，与实际游戏内名称一致。
- **导出**：在 `<游戏目录>/tcmateditor/exports/` 生成两种形式，任选一种：
  - `tinkers_materials_<命名空间>.jar`（推荐）：放进 `mods` 文件夹（客户端和服务端都放），数据和纹理都在这一个文件里，所有存档通用。有新流体时自带独立注册代码，无需 KubeJS。
  - `*-datapack.zip` 放进存档的 `datapacks`，`*-resourcepack.zip` 放进 `resourcepacks` 并在「选项 > 资源包」中启用。**资源包放进 `datapacks` 不会生效**，会导致纹理和名称丢失。
  - 有新流体时还导出 **一个** `tinkers_materials_<命名空间>-fluids.js`，包含全部流体的 `StartupEvents.registry('fluid', ...)` 注册。配合两种 ZIP 使用时，将脚本放进 `kubejs/startup_scripts/`，需要 Minecraft 1.21.1 对应的 KubeJS。每个注册之间留空行，注释格式为 `// 流体id 实际游戏名称`。
  - 使用生成纹理的流体还会导出 `*-kubejs-assets.zip`。使用 JS 注册方式时，将此 ZIP 解压到 `<游戏目录>/kubejs/`，生成纹理和语言资源位于 `kubejs/assets/`；使用 JAR 时所有纹理已包含在 JAR 中。
  - **流体无法导出为数据包，只能导出到kubejs注册脚本或jar模组**。未安装 KubeJS 时，打开流体页会在右下角显示此黄色提示。安装导出内容后需重启游戏与服务器；流体注册不可热重载。JAR 和 JS 注册方式任选其一，不能同时安装。自定义纹理资源也需提供给目标游戏。
- 生成时会检查物品、流体、词条、工具属性是否存在于当前游戏，不存在的会被跳过并在导出窗口列出（例如把物品 ID 填进流体栏），避免整个配方加载失败。其他模组的物品/流体需要目标整合包里也有。
- 自己的 PNG 纹理放进 `<游戏目录>/tcmateditor/patterns/`，即可在「纹理」特效中使用。
- 多人游戏：服务器也需安装本模组，编辑者需要管理员权限；服务器没装时仍可编辑并导出。
- **强化的「严格位置」**：开启时生成两个配方——装了本模组时材料必须放在指定槽位（`tcmateditor:positional_modifier`）；没装本模组时自动改用匠魂原生配方（任意顺序），导出的包依然可用。因为注册了这个配方类型，装了本模组的服务器要求客户端也安装本模组。

## 文件位置

```
<游戏目录>/tcmateditor/
  workspace/   编辑器工程（材料、词条、强化、流体的 JSON）
  live_pack/   本模组运行时自动加载的数据包+资源包（热重载用）
  exports/     导出的独立包
  patterns/    自定义纹理图案
```

## 生成的内容

- `data/<ns>/tinkering/materials/{definition,stats,traits}/<name>.json`
- `data/<ns>/tinkering/modifiers/<entry>.json`（`tconstruct:stat_boost` / `tconstruct:attribute` / `tconstruct:modifier_slot` / `tconstruct:weapon_mob_effect` 等药水效果模块）
- `data/<ns>/tinkering/modifiers/<材料>_slots.json`：材料的强化槽，作为隐藏特性（只在工匠台显示）加到该材料的所有特性列表
- `data/<ns>/recipe/tcmateditor/<name>/...`（`tconstruct:material`、`material_fluid`、`material_melting`）
- `data/<ns>/recipe/tcmateditor/upgrade/<name>.json`（强化；适用装备用 NeoForge 自带的 `neoforge:difference` 排除禁用装备）
- `assets/<ns>/tinkering/materials/<name>.json`、部件纹理 PNG（按 `generator_part_textures.json` 生成，支持附属模组的部件）、语言文件、`mantle/colors.json`
- 新流体的名称、桶模型、流体方块模型，以及独立 JAR 注册代码 / 单个 KubeJS 启动注册脚本（使用 [KubeJS 1.21.1 流体 API](https://github.com/KubeJS-Mods/KubeJS/blob/2101/src/main/java/dev/latvian/mods/kubejs/fluid/FluidBuilder.java)）

## 构建须知ovo

需要同级目录下已构建的 `TinkersConstruct-1.21.1` 和 `Mantle-1.21.1`（或用 `-Ptconstruct_jar=` / `-Pmantle_jar=` 指定 jar）。

开发测试端默认加载 JEI、KubeJS 和 Rhino（仅开发运行依赖，不打包进编辑器，也不作为正式模组的前置）。KubeJS 与 Rhino 版本在 `gradle.properties` 中固定，可使用 `gradlew runClient` 启动。

```
移植的匠魂3：https://github.com/zhuchuovo/TinkersConstruct-1.21.1
移植版的地幔：https://github.com/zhuchuovo/Mantle-1.21.1
```
