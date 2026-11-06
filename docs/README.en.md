<p align="center"><img src="../art/getitup_logo.png" alt="TOMC-Get It up" width="800"></p>

<a href="../README.md">简体中文</a> · English (this page)

> [!WARNING]
> **Early development.** Features and state naming can still change. Installing this mod shifts the numeric ids of every vanilla block state — removing it later makes existing worlds read blocks as something else.

> [!WARNING]
> **AI-assisted.** Code, docs and texture handling were produced with AI assistance (DeepSeek Harness / `deepseek-flash`) under the author's review and in-game testing. AI output can be wrong; judge the risk yourself.

# Get It up

> Store name: **`[GIU] Get It up`**
>
> Enables vertical placement for slabs and stairs without adding any new blocks.

[Design](DESIGN.md) · [Internals](INTERNALS.md) · [Changelog](../CHANGELOG.en.md) · [Store copy](../PUBLISHING.md)

Hold a key while placing a slab or stair and it **stands up**: a slab becomes a half-block-thick wall, a stair becomes a corner with one vertical quarter-column missing — the shape changes, the block stays the same block.

## What it does

| | |
|---|---|
| Vertical slabs | Material hugs the face you clicked, half a block thick, six orientations |
| Vertical stairs | A cube missing one vertical quarter-column; `facing` says which side the notch is on |
| No new blocks, no recipes | A `giu_base` property is injected into vanilla slab/stair block states, so pistons, structure blocks, saves and networking all come for free |
| Shape merging | Vertical-to-vertical and vertical-to-vanilla stairs still form inner/outer corners |
| Textures stay upright | Geometry rotates 90°, textures are re-sampled by world direction: patterns and shading keep their orientation |
| Double slabs | Stacking the same slab on a vertical one still gives `type=double`, seam included |

## Install

| | |
|---|---|
| Minecraft | 26.1.2 |
| Loader | NeoForge 26.1.2.124 or newer |
| Java | 25 |
| Optional | Cloth Config — only for the in-game config screen |
| Side | **Both sides must install it**, same version (block state ids change) |

## Usage

Hold **Left Shift** (rebindable) while placing a slab or stair.

- Clicking a wall puts the material on that wall; left/right of the click picks which way it leans.
- Clicking the floor or ceiling splits that face into eight sectors; the material hugs the nearest one, and a stair's notch lands diagonally opposite.
- Release the key and everything behaves like vanilla again.

Or write it into a command — `giu_base` is `none` (default, vanilla), `north`, `south`, `east` or `west`:

```
/setblock ~ ~ ~ minecraft:oak_slab[giu_base=north,type=bottom]
/setblock ~ ~ ~ minecraft:oak_stairs[giu_base=north,facing=east,half=bottom,shape=straight]
```

Once tilted, `half` no longer means up/down — `giu_base` and `facing` decide the shape.

## Configuration

| File | What | Who decides |
|---|---|---|
| `config/getitup.json` | Which blocks get `giu_base`, plus the dangerous switch | **Server**, restart required, read-only for everyone in multiplayer |
| `config/getitup-client.json` | Retexturing on/off and sampling mode | Each client, applies on `F3+T` |

`getitup.json` cannot be hot-reloaded: it affects how block states are **constructed**, so the in-game screen is only editable from the main menu.

## Docs

| You want | Read | For |
|---|---|---|
| Why it works this way | [DESIGN.md](DESIGN.md) | Players curious about the rules |
| How the code is wired | [INTERNALS.md](INTERNALS.md) | People changing the code |

## Build from source

```
cd neoforge
gradlew.bat build
```

Artifacts land in `neoforge/build/libs/`, named `tomc-getitup-<major>.<minor>.<patch>.<build>-neoforge+mc<game version>.jar`; the build counter comes from `neoforge/build-count.txt` and only means something on the machine that built it.

## License

**GPL-3.0-or-later**, see [LICENSE](../LICENSE).

Developed with **DeepSeek Harness** (`deepseek-flash`) assisting with design discussion, API verification, code and docs.
