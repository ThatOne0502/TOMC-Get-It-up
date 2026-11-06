# 内部实现：代码怎么组织

看完 [DESIGN.md](DESIGN.md) 再看这份。所有路径都相对于 `neoforge/src/main/java/io/github/thatone0502/getitup/`。

## 注入点一览

Mixin 清单在 `resources/getitup.mixins.json`，`compatibilityLevel` 是 `JAVA_21`。

| Mixin | 目标 | 做什么 |
|---|---|---|
| `StateDefinitionBuilderMixin` | `StateDefinition.Builder#create` 的 HEAD | 给台阶/楼梯的状态表追加 `giu_base`。属性必须在 `create` 之前加进去，之后状态表就冻结了 |
| `BlockStateBaseMixin` | `BlockBehaviour.BlockStateBase#getShape(BlockGetter, BlockPos, CollisionContext)` | 返回值过一遍 `Giu.apply`，即绕方块中心转 90° |
| `SlabBlockMixin` | `SlabBlock#getStateForPlacement` 的 RETURN | 原版先算完（含水、叠双层），再覆盖 `giu_base` / `type` |
| `StairBlockMixin` | `StairBlock#getStateForPlacement` 的 RETURN | 同上，另外还要定 `facing` / `half` / `shape` |
| `StairBlockMixin` | `StairBlock#updateShape` 的 RETURN | 邻居变化时用 `GiuStairShapes` 重算 `shape`；交给原版会用世界视角算错 |
| `BlockStateModelSetMixin` | `BlockStateModelSet#<init>` 的 TAIL | 客户端模型表每次资源重载都会重建，在这里把竖直状态换成转过的那份 |
| `BlockStateModelWrapperMixin` | `BlockStateModelWrapper#update` 里的 `collectParts` 调用 | 渲染真正走的路径：模型片塞进列表后，就地替换成转过的那些 |

形状只需要拦 `getShape` 一个方法：`getCollisionShape` 的默认实现就是交给 `state.getShape(...)`，再拦一次会转两遍。客户端模型是两条路径（模型表 + 就地替换），都做了「已经是转过的就跳过」的护栏。

## 类

| 类 | 职责 |
|---|---|
| `GetItUp` | `@Mod` 入口：注册 payload、登录推送配置、启动报告 |
| `Giu` | 内核：属性常量、`giu_base → OctahedralGroup` 表、`apply` / `rotate` / `unrotate`、`baseOf` |
| `GiuBase` | 属性的枚举值与序列化名 |
| `GiuPlacement` | 放置推导：按点击位置算扇区，改写 `giu_base` / `facing` / `half` |
| `GiuStairShapes` | 楼梯形状合并：坐标系换算、三条探针、左右判定 |
| `GiuMode` | 服务端侧的「谁按住了键」 |
| `GiuReport` | 启动时打一行指纹：注入了几块、状态数从多少变成多少 |

| 网络 / 配置 | 职责 |
|---|---|
| `network/GiuNetwork` | 注册两种 payload；登录时把配置推给玩家 |
| `network/GiuModePayload` | 客户端 → 服务端：按住/松开 |
| `network/GiuConfigPayload` | 服务端 → 客户端：正在用的配置 |
| `config/GiuConfig` | `config/getitup.json`：注入范围、危险开关；服务端权威，不热加载 |

| 客户端 | 职责 |
|---|---|
| `client/GiuClient` | 客户端入口：按键、tick 监听、Cloth 配置页 |
| `client/GiuKeys` | 按键定义（默认左 Shift）与分类 |
| `client/GiuKeySync` | 按键状态变化时发 payload；右键时补发一次以消竞态 |
| `client/GiuClientNet` | 缓存服务端推来的配置，供配置页只读显示 |
| `client/GiuConfigScreen` | Cloth 配置页：四个分类，服务端项只读、渲染项本机可改 |
| `client/GiuRenderConfig` | `config/getitup-client.json`：重贴图开关与取样方式 |
| `client/model/GiuModels` | 包装模型表；就地旋转渲染列表；一次性自检模型与形状是否一致 |
| `client/model/GiuBlockStateModel` | 包一层 `BlockStateModel`，把模型片换成转过的 |
| `client/model/GiuModelPart` | 一个模型片：按剔除面分桶旋转，缓存贴图矩形 |
| `client/model/GiuQuadRotation` | 顶点旋转 + 按世界方向重算 UV + 换贴图 |
| `client/model/GiuFaceUv` | 一张面用到的贴图矩形；判断「整张图」与「半高面板」 |
| `client/model/GiuTexturePlan` | 这一轮怎么取样：裁切 / 缩放 / 自动，双层例外 |

## 数据流

**放置**

```
按住键 → GiuKeySync 发 GiuModePayload → GiuMode 记下 UUID
右键放置 → Slab/StairBlock#getStateForPlacement（原版算完）
        → GiuPlacement 按扇区改写 giu_base（楼梯还要 shape）→ 返回状态
```

**形状**

```
getShape → BlockStateBaseMixin → Giu.apply → Shapes.rotate(原版形状, R)
updateShape（楼梯）→ GiuStairShapes.refresh → 在旋转坐标系里重算 shape
```

**渲染**

```
资源重载 → BlockStateModelSet#<init> → GiuModels.wrap → 竖直状态换成 GiuBlockStateModel
绘制 → BlockStateModelWrapper#update → collectParts → 就地替换成 GiuModelPart
     → GiuQuadRotation：顶点绕中心转 R、UV 按世界方向重算、必要时换贴图
```

## 配置

| 文件 | 键 | 默认 |
|---|---|---|
| `config/getitup.json` | `inject_slabs` / `inject_stairs` | `true` / `true` |
| | `dangerous_inject_every_block` | `false` |
| | `dangerous_max_states_per_block` | `256` |
| `config/getitup-client.json` | `retexture` | `true` |
| | `texture_mode` | `auto` |

`getitup.json` 改完必须重启：它决定方块状态怎么构造。`getitup-client.json` 改完按 `F3+T` 重载资源即可。

## 构建

```
cd neoforge
gradlew.bat build
```

`build.gradle` 里版本号拼成 `<mod_version>.<build-count>-neoforge+mc<minecraft_version>`，`build-count.txt` 每次成功构建自增。`processResources` 会对 `META-INF/neoforge.mods.toml` 做 Groovy 模板展开 —— 那份模板里要写**双反斜杠加 n** 才能得到合法的 TOML 转义换行。

## 排错顺序

1. 启动日志里有没有 `[giu] giu_base injected, first target = ...`（属性注入成功）与 `[giu] giu_base on N blocks ... block states A -> B`（范围与状态数）。
2. 形状不对但模型对：看 `getShape` 那条链；模型不对但形状对：看客户端模型包装。
3. 首次渲染时会打一次自检：`[giu] model matches shape on first check`，或者 `[giu] MODEL/SHAPE MISMATCH` —— 后者说明顶点旋转与形状旋转对不上。
