package io.github.thatone0502.getitup;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.StairsShape;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

/**
 * 楼梯该拿哪个 {@code shape}：沿用原版的三步判断，再加半轴方向这一处。
 *
 * <p>竖直楼梯的几何等于「原版楼梯在旋转 {@code R} 之后的坐标系里」，所以 {@code facing}、{@code half}、
 * {@code shape} 都当坐标系内的相对量用，查邻居时先经 {@code R} 映射。
 *
 * <p>原版只查 {@code facing} 轴两侧，因为原版缺口永远朝天或地；竖直之后缺口可能朝侧面，
 * 所以多查半轴方向那一面。规则细节见 {@code docs/DESIGN.md}。
 */
public final class GiuStairShapes {
	/** 缺口朝向：{@code half=bottom} 的缺口在上，{@code half=top} 的缺口在下（坐标系内的方向）。 */
	private static Direction halfAxis(Half half) {
		return half == Half.BOTTOM ? Direction.UP : Direction.DOWN;
	}

	/** 给定一个楼梯状态，它在当前邻居状况下应有的 {@code shape}。 */
	public static StairsShape shapeFor(BlockState state, BlockGetter level, BlockPos pos) {
		StairsShape stored = state.getValue(StairBlock.SHAPE);

		if (!(state.getBlock() instanceof StairBlock)) {
			return stored;
		}

		GiuBase base = Giu.baseOf(state);

		if (base == null) {
			return stored;
		}

		StairsShape wanted = compute(state, level, pos, base, new Frame(level, pos, base));
		return wanted == null ? stored : wanted;
	}

	/** 拿去覆盖 {@code updateShape} 的返回值；没变化时返回原对象，省掉一次状态查找。 */
	public static BlockState refresh(BlockState state, BlockGetter level, BlockPos pos) {
		StairsShape stored = state.getValue(StairBlock.SHAPE);
		StairsShape wanted = shapeFor(state, level, pos);
		return wanted == stored ? state : state.setValue(StairBlock.SHAPE, wanted);
	}

	/**
	 * 原版 {@code StairBlock#getStairsShape} 的镜像版，多看一处半轴方向。
	 *
	 * @return 该取的 shape；{@code null} 表示「没有意见」（原版楼梯没挨着本模组的方块，保持原版结果）
	 */
	private static @Nullable StairsShape compute(BlockState state, BlockGetter level, BlockPos pos, GiuBase base, Frame frame) {
		Half half = state.getValue(StairBlock.HALF);
		Direction facing = frame.facingOf(state);

		// 自己就是个非法状态（facing 落在坐标系竖轴上，只有调试棒造得出来）：别合并。
		if (facing.getAxis() == Direction.Axis.Y) {
			return base.isNone() ? null : StairsShape.STRAIGHT;
		}

		// 两边都是原版楼梯：一律交回原版结果，本模组不改动纯原版场景。
		if (base.isNone() && !frame.hasGiuPartner(facing, half)) {
			return null;
		}

		// 原版第一步：站在 facing 那一侧、与本块垂直的楼梯 ⇒ outer。
		Direction highSide = frame.partner(facing, state, true);

		if (highSide != null && perpendicular(highSide, facing) && frame.isFree(highSide.getOpposite(), facing, half)) {
			return frame.polarity(state, facing, highSide, facing, StairsShape.OUTER_LEFT, StairsShape.OUTER_RIGHT);
		}

		// 原版第二步：反方向那一侧的楼梯 ⇒ inner。
		Direction lowSide = frame.partner(facing.getOpposite(), state, true);

		if (lowSide != null && perpendicular(lowSide, facing) && frame.isFree(lowSide, facing, half)) {
			return frame.polarity(state, facing.getOpposite(), lowSide, facing, StairsShape.INNER_LEFT, StairsShape.INNER_RIGHT);
		}

		// 本模组新增的第三步：缺口正对的那一面（半轴）上有垂直的楼梯 ⇒ inner。
		Direction notchSide = halfAxis(half);
		Direction under = frame.partner(notchSide, state, false);

		if (under != null && perpendicular(under, facing)) {
			return frame.polarity(state, notchSide, under, facing, StairsShape.INNER_LEFT, StairsShape.INNER_RIGHT);
		}

		return base.isNone() ? null : StairsShape.STRAIGHT;
	}

	private static boolean perpendicular(Direction partnerFacing, Direction facing) {
		return partnerFacing.getAxis() != facing.getAxis();
	}

	/**
	 * 「本块的坐标系」视图：所有方向都是坐标系内方向，落到世界才经 {@code R} 映射。
	 *
	 * <p>{@code giu_base=none} 时 {@code R} 是恒等，原版楼梯与竖直楼梯因此共用同一套判断；
	 * 区别在于竖直楼梯会把邻居的 {@code facing} 一起映射进来 —— 原版楼梯那种「缺口朝上、
	 * facing 水平」的邻居搬进来后 facing 落在竖轴上，正好与本块水平的 {@code facing} 垂直。
	 */
	private static final class Frame {
		private final BlockGetter level;
		private final BlockPos pos;
		private final GiuBase base;

		private Frame(BlockGetter level, BlockPos pos, GiuBase base) {
			this.level = level;
			this.pos = pos;
			this.base = base;
		}

		/** 坐标系内的方向 {@code canonical} 在世界里的方向。 */
		private Direction worldOf(Direction canonical) {
			return this.base.isNone() ? canonical : Giu.rotate(this.base, canonical);
		}

		/** 坐标系内的方向 {@code canonical} 在世界里对应的那一格。 */
		private BlockState at(Direction canonical) {
			return this.level.getBlockState(this.pos.relative(worldOf(canonical)));
		}

		/**
		 * 该取 {@code *_left} 还是 {@code *_right}。
		 *
		 * <p>{@code _left} 与 {@code _right} 的差别只有一处：缺口那一格里留空的是靠
		 * {@code CCW(facing)} 的那半还是另一半。原版能用 {@code partnerFacing == facing.getCounterClockWise()}
		 * 直接判，是因为原版伙伴垂直于本块 ⇒ 它的 {@code facing} 必落在 {@code ±CCW(facing)} 上。
		 * 跨坐标系的伙伴 facing 落在竖轴上，这根轴的信息没了，于是改用材料分布（{@link #balance}）；
		 * 那条量在原版情形下与原版判据逐位一致，是它的推广而不是特例补丁。
		 */
		private StairsShape polarity(BlockState self, Direction canonicalDir, Direction partnerFacing, Direction facing, StairsShape leftShape, StairsShape rightShape) {
			// 伙伴的 facing 落在水平面上：与原版同一情形，直接照抄原版那句，保证逐位一致。
			if (partnerFacing.getAxis() != Direction.Axis.Y) {
				return partnerFacing == facing.getCounterClockWise() ? leftShape : rightShape;
			}

			// 否则看材料分布：正的往 left 偏，负的往 right 偏，对称（0）时按原版退化结果取 right。
			return balance(self, canonicalDir, facing) > 0.0 ? leftShape : rightShape;
		}

		/**
		 * 接缝上「缺口那一半」里，伙伴的材料偏 {@code CCW(facing)} 多少：正数 = 偏 left。
		 *
		 * <p>切分轴：伙伴在 {@code ±f} 上取 {@code +h} 那半，在 {@code ±h} 上取 {@code -f} 那半 ——
		 * 两边都是缺口张开的那一半。缺口那半左右对称（左右信息跑到反面那半）时，改用反面并取反号。
		 */
		private double balance(BlockState self, Direction canonicalDir, Direction facing) {
			Direction weft = facing.getCounterClockWise();
			Direction gate = canonicalDir.getAxis() == facing.getAxis()
				? halfAxis(self.getValue(StairBlock.HALF))
				: facing.getOpposite();
			Direction normal = worldOf(canonicalDir);
			Direction worldWeft = worldOf(weft);
			Direction worldGate = worldOf(gate);
			BlockPos otherPos = this.pos.relative(normal);
			BlockState other = this.level.getBlockState(otherPos);

			double primary = layer(other, otherPos, normal, worldGate, worldWeft, 0.0, 0.5);

			if (Math.abs(primary) > 1.0E-6) {
				return primary;
			}

			return -layer(other, otherPos, normal, worldGate, worldWeft, -0.5, 0.0);
		}

		/**
		 * 接缝上、切分轴落在 {@code [from, to]} 那一带里，伙伴材料偏 {@code worldWeft} 多少。
		 *
		 * <p>只统计贴着接缝的盒子（相对伙伴自己中心，朝向本块那一面在 {@code -0.5}）。
		 */
		private double layer(BlockState other, BlockPos otherPos, Direction normal, Direction worldGate, Direction worldWeft, double from, double to) {
			if (!(other.getBlock() instanceof StairBlock)) {
				return 0.0;
			}

			double sum = 0.0;

			for (AABB box : other.getShape(this.level, otherPos).toAabbs()) {
				if (Math.abs(lo(box, normal) + 0.5) > 1.0E-4) {
					continue;
				}

				double gateLength = overlap(lo(box, worldGate), hi(box, worldGate), from, to);
				double plus = overlap(lo(box, worldWeft), hi(box, worldWeft), 0.0, 0.5);
				double minus = overlap(lo(box, worldWeft), hi(box, worldWeft), -0.5, 0.0);

				sum += gateLength * (plus - minus);
			}

			return sum;
		}

		/** 把任何一个楼梯的 {@code facing} 当作世界方向搬进本块的坐标系。 */
		private @Nullable Direction facingOf(BlockState stair) {
			if (!(stair.getBlock() instanceof StairBlock)) {
				return null;
			}

			Direction world = stair.getValue(StairBlock.FACING);
			return this.base.isNone() ? world : Giu.unrotate(this.base, world);
		}

		/** 邻居与本块是否同一坐标系（同 {@code giu_base}，含「都是 none」）。 */
		private boolean sameFrame(BlockState stair) {
			return Giu.baseOf(stair) == this.base;
		}

		/**
		 * 该方向上若是楼梯，返回它在本坐标系里的 {@code facing}；否则 {@code null}。
		 *
		 * @param gateHalf 是否要求 {@code half} 相同。同一坐标系内沿用原版的门槛；
		 *                 跨坐标系时 {@code half} 指的不是同一个半格，比了反而错，所以放行。
		 */
		private @Nullable Direction partner(Direction canonical, BlockState self, boolean gateHalf) {
			BlockState neighbour = at(canonical);
			Direction facing = facingOf(neighbour);

			if (facing == null) {
				return null;
			}

			if (gateHalf && sameFrame(neighbour) && neighbour.getValue(StairBlock.HALF) != self.getValue(StairBlock.HALF)) {
				return null;
			}

			return facing;
		}

		/** 三个查看方向上是否有竖直（{@code giu_base != none}）的邻居。 */
		private boolean hasGiuPartner(Direction facing, Half half) {
			return isGiu(facing) || isGiu(facing.getOpposite()) || isGiu(halfAxis(half));
		}

		private boolean isGiu(Direction canonical) {
			GiuBase neighbourBase = Giu.baseOf(at(canonical));
			return neighbourBase != null && !neighbourBase.isNone();
		}

		/** 原版 {@code canTakeShape}：那个斜对角的格子里若是「同 facing 且同 half」的楼梯，就不许取这个形状。 */
		private boolean isFree(Direction canonical, Direction facing, Half selfHalf) {
			BlockState neighbour = at(canonical);
			Direction neighbourFacing = facingOf(neighbour);

			if (neighbourFacing == null || neighbourFacing != facing) {
				return true;
			}

			return Giu.baseOf(neighbour) != this.base || neighbour.getValue(StairBlock.HALF) != selfHalf;
		}
	}

	/** 盒子在方向 {@code dir} 上、相对方块中心的低端（方块中心记作 0，方块的边就是 ±0.5）。 */
	private static double lo(AABB box, Direction dir) {
		return switch (dir) {
			case EAST -> box.minX - 0.5;
			case WEST -> 0.5 - box.maxX;
			case UP -> box.minY - 0.5;
			case DOWN -> 0.5 - box.maxY;
			case SOUTH -> box.minZ - 0.5;
			case NORTH -> 0.5 - box.maxZ;
		};
	}

	/** 盒子在方向 {@code dir} 上、相对方块中心的高端。 */
	private static double hi(AABB box, Direction dir) {
		return switch (dir) {
			case EAST -> box.maxX - 0.5;
			case WEST -> 0.5 - box.minX;
			case UP -> box.maxY - 0.5;
			case DOWN -> 0.5 - box.minY;
			case SOUTH -> box.maxZ - 0.5;
			case NORTH -> 0.5 - box.minZ;
		};
	}

	/** 两段区间的重叠长度，没有重叠就是 0。 */
	private static double overlap(double lo, double hi, double from, double to) {
		return Math.max(0.0, Math.min(hi, to) - Math.max(lo, from));
	}

	private GiuStairShapes() {
	}
}
