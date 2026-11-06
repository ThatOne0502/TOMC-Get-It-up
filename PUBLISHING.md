# 发布页文案 / Store Page Copy

对外发布名：**`[GIU] 立了 Get It up`**。这只是发布平台上的展示名，游戏内的模组名仍是 `neoforge.mods.toml` 里的 `TOMC-Get It up`，两者不冲突，也不需要同步。

**一句话总结**

- 中文：**无需注册新方块，即可让台阶和楼梯垂直放置。**
- 英文：**Enables vertical placement for slabs and stairs without adding any new blocks.**

下面两篇同源：第一篇纯中文（MC 百科这类中文平台），第二篇纯英文（CurseForge / Modrinth）。按需整段复制。

## 元数据（照抄）

| 字段 | 值 |
|---|---|
| 发布名 | `[GIU] 立了 Get It up` |
| 英文名 | `Get It up` |
| 中文名 | `立了` |
| Mod ID | `getitup` |
| 平台 / 版本 | NeoForge · Minecraft 26.1.2 |
| 运行环境 | 客户端与服务端都必须装，且版本一致 |
| 依赖 | 无强制依赖；Cloth Config 可选（仅游戏内配置页需要） |
| 许可 | GPL-3.0-or-later（开源） |
| 作者 | ThatOne |
| 仓库 | https://github.com/ThatOne0502/TOMC-Get-It-up |
| 分类 | MC 百科：装饰 / 建筑；CurseForge：Decoration；Modrinth：Decoration、Utility |

---

## 一、纯中文（MC 百科，直接贴）

立了 Get It up

无需注册新方块，即可让台阶和楼梯垂直放置。

台阶和楼梯一直是建筑玩家的遗憾：想要半格厚的墙、想要竖着靠墙的栏板，只能拿台阶的碰撞箱硬凑，或者干脆换个方块。

本模组不新增任何方块，而是给原版（以及其它模组）的台阶、楼梯加了一个方块状态 `giu_base`，让它们能**立起来**。

怎么放

按住左 Shift（可在按键设置里改）再放置台阶或楼梯。

台阶会变成半格厚的竖墙：材质贴着你点的那一面，可以贴地面、天花板，也可以贴四面墙。

楼梯会侧倒成一个缺了竖直角柱的拐角：缺口落在你点击的那一侧，剩下的两个完整竖面正好是你点的墙面和踏步方向。

松开键，一切恢复原版。你手里的普通台阶依然只是普通台阶 —— 竖直放置只在你按住键的那一刻生效。

形状会自己接合

竖直楼梯之间能拼出内角和外角；竖直楼梯旁边放普通楼梯，也能接上，形状会自动判断该补哪半格。

原版楼梯靠近竖直楼梯时，也会被本模组改写形状 —— 这是接合的一部分，不是 bug。

贴图不跟着转

方块转 90°，贴图仍按世界方向取样：图案方向不变，明暗关系不变。你的墙仍然是横着砌的墙，木纹不会突然竖起来。

台阶那种专门画的半高面板贴图（例如平滑石台阶的侧面）会改用完整方块的贴图再裁切，于是竖直单层台阶得到的是一整块带描边的面板；双层台阶保留原来的接缝。

可以直接写进命令

`giu_base` 的取值是 `none`（默认，原版形态）、`north`、`south`、`east`、`west`：

```
/setblock ~ ~ ~ minecraft:oak_slab[giu_base=north,type=bottom]
/setblock ~ ~ ~ minecraft:oak_stairs[giu_base=north,facing=east,half=bottom,shape=straight]
```

因此 `/fill`、`/execute if block`、活塞、结构方块、调试棒全部可用。

配置

`config/getitup.json` 决定给哪些方块注入（台阶 / 楼梯 / 危险的「所有方块」开关），服务端权威，改完必须重启。

`config/getitup-client.json` 决定重贴图开关与取样方式，每个客户端各自决定，改完按 F3+T 生效。

装 Cloth Config 的话，这两份配置都能在游戏内的配置页里改。

请注意

竖直楼梯的踏步是竖直的，**不能走**，它是纯装饰方块。

本模组会改变方块状态的 id：装过之后再卸载，或者拿旧存档进新世界，方块可能读成别的东西。客户端与服务端必须装同一个版本、用同一份配置。

---

## 二、纯英文（CurseForge / Modrinth）

Get It up

Enables vertical placement for slabs and stairs without adding any new blocks.

Slabs and stairs have always been a compromise for builders: you want a half-block-thick wall, or a panel standing against a wall, and you end up faking it with collision boxes.

This mod registers no blocks at all. It adds one block state, `giu_base`, to vanilla (and modded) slabs and stairs so they can **stand up**.

How to place

Hold Left Shift (rebindable) while placing a slab or a stair.

A slab becomes a half-block-thick wall hugging the face you clicked — floor, ceiling or any of the four walls.

A stair tips over into a corner missing one vertical quarter-column; the notch lands on the side you clicked, and the two remaining full faces are exactly the wall you clicked and the direction you walk up.

Release the key and everything is vanilla again. The slab in your hand is still an ordinary slab — vertical placement only happens while the key is held.

Shapes connect on their own

Vertical stairs form inner and outer corners with each other, and also with ordinary stairs next to them. Vanilla stairs adjacent to a vertical one get their shape rewritten too — that is part of connecting them, not a bug.

Textures stay upright

The geometry rotates 90°, but textures are re-sampled by world direction: pattern orientation and shading stay put. Your wall still looks like a wall built the normal way; wood grain does not suddenly turn sideways.

Slabs whose side uses a dedicated half-height panel texture (smooth stone, for example) fall back to the block's full texture and are cut from that, so a vertical single slab becomes one clean panel with a border. Double slabs keep their original seam.

Commands work too

`giu_base` is `none` (default, vanilla), `north`, `south`, `east` or `west`:

```
/setblock ~ ~ ~ minecraft:oak_slab[giu_base=north,type=bottom]
/setblock ~ ~ ~ minecraft:oak_stairs[giu_base=north,facing=east,half=bottom,shape=straight]
```

So `/fill`, `/execute if block`, pistons, structure blocks and the debug stick all work.

Configuration

`config/getitup.json` decides which blocks are injected (slabs / stairs / a dangerous "every block" switch). It is server-authoritative and requires a restart.

`config/getitup-client.json` decides retexturing and sampling mode per client; applies on F3+T.

With Cloth Config installed, both are editable from the in-game config screen.

Please note

The treads of vertical stairs are vertical — **not walkable**. They are decorative.

This mod shifts block state ids: removing it later, or bringing an old save, can make blocks read as something else. Client and server must use the same version and the same config.
