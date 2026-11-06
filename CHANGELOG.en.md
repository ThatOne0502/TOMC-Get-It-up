<a href="CHANGELOG.md">简体中文</a> · English (this page)

# Changelog: Get It up

Versions are `<major>.<minor>.<patch>.<build>`: the first three are decided by a human, the fourth is "how many times this machine has built successfully" and only means something on the machine that produced the file. The file name ends with the target game version. Format follows [Keep a Changelog](https://keepachangelog.com/), without dates.

## 0.1.0

### Added

- `giu_base` property injection: every block extending `SlabBlock` / `StairBlock` (including modded ones) gets `giu_base` at construction time. Default is `none`, i.e. the vanilla form.
- Shape rotation: when `giu_base` is not `none`, the shape is rotated 90° around the block centre — collision, outline, occlusion and support faces all follow. `/setblock`, `/fill`, `/execute if block`, the debug stick, pistons and structure blocks work out of the box.
- Vertical placement: hold the key (Left Shift by default, rebindable) while placing and a slab becomes a half-block-thick wall hugging the clicked face, while a stair tips over into a corner missing one vertical quarter-column.
- Eight-sector orientation: clicking the floor or ceiling splits that face into 45° sectors that decide which face the material hugs and which corner a stair's notch falls into; clicking a wall picks left or right from the click position.
- Vertical slabs: six slots (top/bottom plus four walls); `type=double` means a rotated double slab.
- Vertical stairs: `facing` perpendicular to `giu_base`, 32 poses in total. Degenerate combinations where `facing` lies on the `giu_base` axis only come from commands or the debug stick.
- Shape merging: vertical-to-vertical and vertical-to-vanilla stairs form inner and outer corners; the rule is "the side whose notch is blocked changes shape".
- Retexturing: geometry rotates, textures are re-sampled by world direction, so pattern orientation and shading stay put. Sampling is cut by world position (identical to vanilla, never distorted); dedicated half-height panel textures (`*_slab_side`) fall back to the block's full texture, and double slabs keep their seam.
- Config: `config/getitup.json` decides which blocks are injected (server-authoritative, restart required); `config/getitup-client.json` decides retexturing (per client, applies on `F3+T`).
- Cloth Config screen (optional dependency): four categories — injection targets, dangerous options, authority, client rendering. While a world is open the server options are read-only and always show the server's live values.
- Dangerous switch: inject `giu_base` into every block, with a per-block state-count ceiling as a brake. Off by default.
- Startup report: the log shows the injection range and the state count change (e.g. `block states 29873 -> 49921`).

### Known limitations

- The treads of vertical stairs are vertical: **not walkable**, purely decorative.
- Block state ids shift globally: old saves created without the mod, or worlds opened after removing it, can read blocks as something else. Both sides need the same version and config.
- In the vertical form, `half` no longer means up/down; `giu_base` and `facing` decide the shape.
- Vanilla stairs adjacent to a vertical stair have their `shape` rewritten by this mod (part of shape merging).
