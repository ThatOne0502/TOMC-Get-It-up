<p align="center"><img src="art/getitup_logo.png" alt="TOMC-Get It up" width="800"></p>

简体中文（本页）· <a href="docs/README.en.md">English</a>

> [!WARNING]
> **早期开发阶段。** 功能与状态命名仍可能变动，接口未冻结。装进世界后，原版方块的状态 id 会整体位移 —— 卸载本模组会让旧存档里的方块读成别的东西。

> [!WARNING]
> **AI 辅助创作。** 本项目的代码、文档与贴图处理逻辑由作者与 AI 协作完成（DeepSeek Harness / deepseek-flash）。AI 生成的内容经过作者审阅与实机验证，但仍请自行评估风险。
>
> **AI-assisted.** Code, docs and texture handling were produced with AI assistance (DeepSeek Harness / deepseek-flash) under the author's review and in-game testing.

# Get It up

> 发布名（对外）：**`[GIU] 立了 Get It up`**
>
> 无需注册新方块，即可让台阶和楼梯垂直放置。

[设计](docs/DESIGN.md) · [内部实现](docs/INTERNALS.md) · [变更记录](CHANGELOG.md) · [发布文案](PUBLISHING.md)

按住一个键放台阶或楼梯，它会**立起来**：台阶变成半格厚的竖墙，楼梯变成缺一个竖直角柱的拐角 —— 形状变了，方块还是原来那个方块。

## 它做了什么

| 做的事 | 说明 |
|---|---|
| 竖直台阶 | 材质贴着所点的那一面，占半格厚，六个朝向都能放 |
| 竖直楼梯 | 缺一个竖直 1/4 角柱的立方体，`facing` 决定缺口在哪一侧 |
| 不改配方、不新增方块 | 直接给原版台阶/楼梯的方块状态加一个 `giu_base` 属性，活塞、结构方块、存档、网络同步全部免费 |
| 形状自动接合 | 竖直楼梯之间、竖直楼梯与原版楼梯之间都能拼出内角/外角 |
| 贴图不跟着转 | 几何转 90°，贴图仍按世界方向取样：图案方向、明暗关系不变，便于融入建筑 |
| 两层台阶 | 竖直台阶上再放同种台阶仍是双层（原版行为），接缝保留 |

## 安装

| 项 | 值 |
|---|---|
| Minecraft | 26.1.2 |
| 加载器 | NeoForge 26.1.2.124 或更新 |
| Java | 25 |
| 前置（可选） | Cloth Config —— 仅用于游戏内配置页，没有也能玩 |
| 安装侧 | **双端都必须装**，且版本一致（方块状态的 id 变了） |

## 用法

按住 **左 Shift**（可在「按键设置」里改）再放置台阶或楼梯，就会竖直放置。

- 点墙面 → 材质贴在墙面上，朝向按点击位置分左右两档。
- 点地面/天花板 → 把那个面按八扇区切开，材质贴着离点击处最近的那一面；楼梯的缺口落在斜对角。
- 松开键 → 完全恢复原版行为。竖直方块上再放同种台阶、手持非台阶方块时都不受影响。

也可以直接写进命令，`giu_base` 的取值是 `none`（默认，原版形态）、`north`、`south`、`east`、`west`：

```
/setblock ~ ~ ~ minecraft:oak_slab[giu_base=north,type=bottom]
/setblock ~ ~ ~ minecraft:oak_stairs[giu_base=north,facing=east,half=bottom,shape=straight]
/fill ~ ~ ~ ~ ~4 ~ minecraft:smooth_stone_slab[giu_base=east,type=bottom]
```

竖直状态下 `half` 不再决定上下（形状由 `giu_base` 与 `facing` 决定），调试棒仍能摆出各种组合。

## 配置

| 文件 | 管什么 | 谁说了算 |
|---|---|---|
| `config/getitup.json` | 给哪些方块注入 `giu_base`、危险开关 | **服务端权威**，改完必须重启；多人游戏里所有人只能看 |
| `config/getitup-client.json` | 重贴图开关与取样方式 | 每个客户端各自决定，`F3+T` 或重启生效 |

`getitup.json` 不能热加载：它影响的是**方块状态的构造**，世界开着的时候改不了 —— 游戏内的配置页因此只有在主菜单才能编辑，进世界后整页只读。

## 文档

| 你想知道 | 看这份 | 写给谁 |
|---|---|---|
| 为什么这么做、规则从哪来 | [docs/DESIGN.md](docs/DESIGN.md) | 想理解行为的人 |
| 代码怎么组织、注入点在哪 | [docs/INTERNALS.md](docs/INTERNALS.md) | 想改代码的人 |
| 每个版本改了什么 | [CHANGELOG.md](CHANGELOG.md) | 所有人 |

## 从源码构建

```
cd neoforge
gradlew.bat build
```

产物在 `neoforge/build/libs/`，命名规则 `tomc-getitup-<主>.<次>.<修>.<构建号>-neoforge+mc<游戏版本>.jar`；构建号由 `neoforge/build-count.txt` 每次成功构建自增，只在本机有意义。

## 许可与致谢

以 **GPL-3.0-or-later** 发布，见 [LICENSE](LICENSE)。使用、修改、再分发请遵守同一许可。

开发过程由 **DeepSeek Harness**（`deepseek-flash`）辅助完成：设计讨论、API 核对、代码与文档撰写。
