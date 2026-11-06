package io.github.thatone0502.getitup;

import com.mojang.math.OctahedralGroup;
import io.github.thatone0502.getitup.config.GiuConfig;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * 竖直台阶/楼梯的内核：把原版方块当素材，用 {@link #GIU_BASE} 记下它的底面被掰到了哪一面，
 * 再对<b>形状</b>套一个固定的 90° 旋转。状态本身（facing/half/shape/type）原样保留，几何由纯函数反解。
 */
public final class Giu {
	/**
	 * 原方块底面的外法线方向。默认值必须是 {@code NONE}：
	 * {@code StateDefinition.any()} 取每个属性 {@code getPossibleValues()} 的第 0 个，
	 * 而 {@code Block} 构造器里 {@code registerDefaultState(stateDefinition.any())} 紧接着就跑了。
	 */
	public static final EnumProperty<GiuBase> GIU_BASE = EnumProperty.create(
		"giu_base",
		GiuBase.class,
		List.of(GiuBase.NONE, GiuBase.NORTH, GiuBase.SOUTH, GiuBase.EAST, GiuBase.WEST)
	);

	private static final String PROPERTY_NAME = "giu_base";

	/** giu_base -> 对应的八面体旋转群（把原版几何转到目标朝向的那个旋转）。 */
	private static final Map<GiuBase, OctahedralGroup> GROUPS = buildGroups();

	private Giu() {
	}

	private static final AtomicBoolean LOGGED_FIRST_INJECTION = new AtomicBoolean();

	/** 触碰一下，强制类初始化（属性 + 旋转表），让配置错误尽早暴露。 */
	public static void ready() {
		// 空实现，只为触发 static 初始化。
	}

	/** 属性注入成功的回调。第一条打 INFO 当自检，其余降到 DEBUG。 */
	public static void onInjected(Block block) {
		String type = block.getClass().getSimpleName();

		if (LOGGED_FIRST_INJECTION.compareAndSet(false, true)) {
			GetItUp.LOGGER.info("giu_base injected, first target = {}", type);
		} else {
			GetItUp.LOGGER.debug("giu_base injected into {}", type);
		}
	}

	/**
	 * 读一眼配置里「要不要注入」的决定；真正判定在
	 * {@link io.github.thatone0502.getitup.config.GiuConfig#shouldInject}。
	 * 构造期拿不到 registry id，所以只能按类型判（继承原版类的台阶/楼梯顺带覆盖）。
	 */
	public static boolean isTarget(Block block, int currentStates) {
		return GiuConfig.shouldInject(block, currentStates);
	}

	/**
	 * 该状态的 {@code giu_base}；没有属性返回 null。只问状态、不问配置：
	 * 配置决定的是「构造期注不注入」，注入了就必须转，这样余下的状态行为不会随配置漂移。
	 */
	public static @Nullable GiuBase baseOf(BlockBehaviour.BlockStateBase state) {
		return state.hasProperty(GIU_BASE) ? state.getValue(GIU_BASE) : null;
	}

	/** 把原版形状搬到 giu_base 指定的朝向；{@code NONE} 或空形状原样返回。 */
	public static VoxelShape apply(BlockBehaviour.BlockStateBase state, VoxelShape shape) {
		GiuBase base = baseOf(state);
		if (base == null || base.isNone() || shape.isEmpty()) {
			return shape;
		}

		return Shapes.rotate(shape, GROUPS.get(base));
	}

	public static OctahedralGroup group(GiuBase base) {
		return GROUPS.get(base);
	}

	/**
	 * 原版方向 → 旋转后的方向：绕「同时垂直于 DOWN 与 giu_base」的水平轴转 90°，
	 * 即四循环 {@code DOWN → giu_base → UP → 反方向 → DOWN}，另外两个水平方向不动。
	 */
	public static Direction rotate(GiuBase base, Direction direction) {
		Direction d = base.directionOrThrow();
		if (direction == Direction.DOWN) {
			return d;
		}

		if (direction == d) {
			return Direction.UP;
		}

		if (direction == Direction.UP) {
			return d.getOpposite();
		}

		if (direction == d.getOpposite()) {
			return Direction.DOWN;
		}

		// 剩下的两个方向正好垂直于 d，位于旋转轴上，不动。
		return direction;
	}

	/** {@link #rotate} 的反函数：旋转后的方向 -> 原版方向。 */
	public static Direction unrotate(GiuBase base, Direction rotated) {
		for (Direction candidate : Direction.values()) {
			if (rotate(base, candidate) == rotated) {
				return candidate;
			}
		}

		throw new IllegalStateException("no source direction for " + rotated + " under giu_base=" + base.getSerializedName());
	}

	public static boolean hasProperty(BlockBehaviour.BlockStateBase state) {
		return state.hasProperty(GIU_BASE);
	}

	public static String propertyName() {
		return PROPERTY_NAME;
	}

	/**
	 * 在 24 个八面体群里找出与 {@link #rotate} 完全等价的那一个，
	 * 这样「形状旋转」和「方向换算」用的是同一份定义，不会各转各的。
	 */
	private static Map<GiuBase, OctahedralGroup> buildGroups() {
		Map<GiuBase, OctahedralGroup> groups = new EnumMap<>(GiuBase.class);
		groups.put(GiuBase.NONE, OctahedralGroup.IDENTITY);

		for (GiuBase base : GiuBase.values()) {
			if (base.isNone()) {
				continue;
			}

			OctahedralGroup match = null;
			int matches = 0;

			for (OctahedralGroup candidate : OctahedralGroup.values()) {
				if (matchesAll(candidate, base)) {
					if (match == null) {
						match = candidate;
					}

					matches++;
				}
			}

			if (match == null || matches != 1) {
				throw new IllegalStateException(
					"failed to map giu_base=" + base.getSerializedName() + " onto an octahedral group (matches=" + matches + ")"
				);
			}

			groups.put(base, match);
		}

		return groups;
	}

	private static boolean matchesAll(OctahedralGroup group, GiuBase base) {
		for (Direction direction : Direction.values()) {
			if (group.rotate(direction) != rotate(base, direction)) {
				return false;
			}
		}

		return true;
	}
}
