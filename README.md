# Tinkers' Material Editor / 匠魂材料编辑器

匠魂（Tinkers' Construct 1.21.1, NeoForge）的游戏内材料编辑器。按 **Shift+Z** 打开。

本模组只是编辑器：生成的内容全部是匠魂标准格式的数据包/资源包，**导出后卸载本模组也能正常使用**。

## 功能

| 页面 | 作用 |
| --- | --- |
| 基础 | ID、等级、排序、部件台制作物品（物品或 `#标签`）、可选熔铸流体、游戏内预览、获取测试工具 |
| 属性 | 头部 / 手柄 / 绑定结 / 弓臂 / 握把 / 各部位镀层 / 锁链甲 / 史莱姆套 / 弹药…… 开关即启用，数值实时生效 |
| 特性 | 默认特性、按类别（近战采掘/远程/护甲/弹药）或单个部件覆盖；可使用自定义词条 |
| 纹理 | 拖动调色板改颜色、一键配色、基础样式、发光；特效图层（染色/噪点/条纹/格纹/闪点/渐变/描边/色相/对比度/纹理叠加）可限定暗部/中间调/亮部/边缘；动画（流光/呼吸/彩虹/闪烁/流动） |
| 介绍 | 名称、短句、介绍，支持简体中文 / 繁體中文 / English / 日本語 |
| 词条 | 自定义强化属性，例如「每级 +2 攻击伤害、-10% 攻击速度」，作为特性加到材料上 |
| 强化 | 把词条做成工匠台/工匠砧配方：指定 5 个槽位各放什么材料（从青金石槽顺时针 1~5，悬停「?」看编号）、占用槽位、最高等级，以及哪些装备可用/禁用 |

- **导入**：以任意已加载的材料为模板（数值、特性、制作物品、调色板、多语言文本）。
- **应用（Ctrl+S）**：保存并热重载。数值改动开启「自动应用」后约 1 秒自动生效，背包里的工具会立即刷新；纹理和文本改动会刷新资源。
- **导出**：在 `<游戏目录>/tcmateditor/exports/` 生成 `*-datapack.zip`（放入存档 `datapacks`）和 `*-resourcepack.zip`（放入 `resourcepacks` 并启用）。
- 自己的 PNG 纹理放进 `<游戏目录>/tcmateditor/patterns/`，即可在「纹理」特效中使用。
- 多人游戏：服务器也需安装本模组，编辑者需要管理员权限；服务器没装时仍可编辑并导出。
- **强化的「严格位置」**：开启时生成两个配方——装了本模组时材料必须放在指定槽位（`tcmateditor:positional_modifier`）；没装本模组时自动改用匠魂原生配方（任意顺序），导出的包依然可用。因为注册了这个配方类型，装了本模组的服务器要求客户端也安装本模组。

## 文件位置

```
<游戏目录>/tcmateditor/
  workspace/   编辑器工程（材料、词条的 JSON）
  live_pack/   本模组运行时自动加载的数据包+资源包（热重载用）
  exports/     导出的独立包
  patterns/    自定义纹理图案
```

## 生成的内容（全部为匠魂原生格式）

- `data/<ns>/tinkering/materials/{definition,stats,traits}/<name>.json`
- `data/<ns>/tinkering/modifiers/<entry>.json`（`tconstruct:stat_boost` / `tconstruct:attribute` 模块）
- `data/<ns>/recipe/tcmateditor/<name>/...`（`tconstruct:material`、`material_fluid`、`material_melting`）
- `data/<ns>/recipe/tcmateditor/upgrade/<name>.json`（强化；适用装备用 NeoForge 自带的 `neoforge:difference` 排除禁用装备）
- `assets/<ns>/tinkering/materials/<name>.json`、部件纹理 PNG（按 `generator_part_textures.json` 生成，支持附属模组的部件）、语言文件、`mantle/colors.json`

## 构建须知ovo

需要同级目录下已构建的 `TinkersConstruct-1.21.1` 和 `Mantle-1.21.1`（或用 `-Ptconstruct_jar=` / `-Pmantle_jar=` 指定 jar）。

```
移植的匠魂3：https://github.com/zhuchuovo/TinkersConstruct-1.21.1
移植版的地幔：https://github.com/zhuchuovo/Mantle-1.21.1
```
