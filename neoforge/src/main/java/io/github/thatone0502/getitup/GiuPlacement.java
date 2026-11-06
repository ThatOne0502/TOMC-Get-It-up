package io.github.thatone0502.getitup;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * 按住键时把原版算出来的放置状态改写一份。
 *
 * <p>只把点击位置翻译成 {@code giu_base}（以及楼梯要的 {@code facing}）；形状与模型都从
 * {@code giu_base} 走旋转表算出来，这里只管填对字段。
 *
 * <p>以被点击方块中心为原点看水平偏移 {@code dx}/{@code dz}：水平面按「两条中线 + 两条对角线」
 * 切成 8 个 45° 三角，占优轴给 {@code giu_base}、次要轴给拐角在哪个角（楼梯的 {@code facing}）；
 * 竖直面则底面贴墙，{@code giu_base} 取被点那一面的对面，次级方向只有左/右两档。
 */
public final class GiuPlacement {
	/** 一次点击读出来的两个方向。 */
	private record Sector(Direction base, Direction side) {
	}

	/**
	 * 台阶：只改 {@code giu_base}，并强制 {@code type=bottom}。
	 *
	 * <p>{@code type=double} 交给原版原样返回：叠层时方向沿用先放的那一块，不重置。
	 */
	public static @Nullable BlockState adjustSlab(BlockPlaceContext context, @Nullable BlockState vanilla) {
		if (vanilla == null || !shouldRewrite(context, vanilla)) {
			return vanilla;
		}
		if (vanilla.getValue(SlabBlock.TYPE) == SlabType.DOUBLE) {
			return vanilla;
		}
		Sector sector = sector(context);
		GiuBase base = sector == null ? null : GiuBase.byDirection(sector.base());
		if (base == null) {
			return vanilla;
		}
		return vanilla.setValue(SlabBlock.TYPE, SlabType.BOTTOM).setValue(Giu.GIU_BASE, base);
	}

	/**
	 * 楼梯：{@code giu_base} 给底座贴哪面，{@code facing} 给「高的那一侧」（= 拐角块所在的那一侧），
	 * {@code half} 一律 {@code bottom}，{@code shape} 交给 {@link GiuStairShapes} 按邻居算。
	 */
	public static @Nullable BlockState adjustStair(BlockPlaceContext context, @Nullable BlockState vanilla) {
		if (vanilla == null || !shouldRewrite(context, vanilla)) {
			return vanilla;
		}
		Sector sector = sector(context);
		GiuBase base = sector == null ? null : GiuBase.byDirection(sector.base());
		if (base == null) {
			return vanilla;
		}
		BlockState rewritten = vanilla
			.setValue(Giu.GIU_BASE, base)
			.setValue(StairBlock.FACING, sector.side())
			.setValue(StairBlock.HALF, Half.BOTTOM);
		return rewritten.setValue(
			StairBlock.SHAPE,
			GiuStairShapes.shapeFor(rewritten, context.getLevel(), context.getClickedPos())
		);
	}

	/** 键按着、方块带 {@code giu_base}、玩家是真人（不是发射器/命令）。 */
	private static boolean shouldRewrite(BlockPlaceContext context, BlockState vanilla) {
		if (!Giu.hasProperty(vanilla)) {
			return false;
		}
		Player player = context.getPlayer();
		return GiuMode.isActive(player);
	}

	/** 把点击位置读成「底座贴哪面」+「角在哪一侧」；读不出来返回 null。 */
	private static @Nullable Sector sector(BlockPlaceContext context) {
		Direction face = context.getClickedFace();
		BlockPos pos = context.getClickedPos();
		Vec3 click = context.getClickLocation();
		double dx = click.x - (pos.getX() + 0.5);
		double dz = click.z - (pos.getZ() + 0.5);

		if (face.getAxis() == Direction.Axis.Y) {
			if (Math.abs(dx) > Math.abs(dz)) {
				return new Sector(
					dx > 0 ? Direction.EAST : Direction.WEST,
					dz > 0 ? Direction.SOUTH : Direction.NORTH
				);
			}
			return new Sector(
				dz > 0 ? Direction.SOUTH : Direction.NORTH,
				dx > 0 ? Direction.EAST : Direction.WEST
			);
		}

		// 点在墙上：底面贴着墙，也就是贴着「被点那一面的对面」。
		Direction base = face.getOpposite();
		boolean baseAlongX = base.getAxis() == Direction.Axis.X;
		double offset = baseAlongX ? dz : dx;
		Direction side = baseAlongX
			? (offset > 0 ? Direction.SOUTH : Direction.NORTH)
			: (offset > 0 ? Direction.EAST : Direction.WEST);
		return new Sector(base, side);
	}

	private GiuPlacement() {
	}
}
