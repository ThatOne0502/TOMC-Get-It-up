package io.github.thatone0502.getitup.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import io.github.thatone0502.getitup.GiuPlacement;
import io.github.thatone0502.getitup.GiuStairShapes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.StairsShape;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 楼梯的两个改写点：放置时定方向，邻居变化时重算 {@code shape}。
 *
 * <p>放置这一半与 {@link SlabBlockMixin} 同一套路，原版先算完再覆盖方向字段。
 *
 * <p>{@code updateShape} 必须接管：交给原版，它会用<b>世界</b>视角看邻居，给竖直楼梯写出
 * {@code inner}/{@code outer}，几何当场错乱。
 */
@Mixin(StairBlock.class)
public abstract class StairBlockMixin {
	@Inject(method = "getStateForPlacement", at = @At("RETURN"), cancellable = true)
	private void giu$rewritePlacement(BlockPlaceContext context, CallbackInfoReturnable<@Nullable BlockState> cir) {
		BlockState original = cir.getReturnValue();
		BlockState rewritten = GiuPlacement.adjustStair(context, original);

		if (rewritten == null) {
			return;
		}

		// 原版楼梯也要走一遍：它的缺口可能正被一座竖直楼梯顶着，那时它自己该变成 inner。
		StairsShape shape = GiuStairShapes.shapeFor(rewritten, context.getLevel(), context.getClickedPos());
		BlockState shaped = rewritten.setValue(StairBlock.SHAPE, shape);

		if (shaped != original) {
			cir.setReturnValue(shaped);
		}
	}

	@ModifyReturnValue(
		method = "updateShape(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/LevelReader;Lnet/minecraft/world/level/ScheduledTickAccess;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/Direction;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/util/RandomSource;)Lnet/minecraft/world/level/block/state/BlockState;",
		at = @At("RETURN")
	)
	private BlockState giu$refreshShape(
		BlockState original,
		BlockState state,
		LevelReader level,
		ScheduledTickAccess ticks,
		BlockPos pos,
		Direction directionToNeighbour,
		BlockPos neighbourPos,
		BlockState neighbourState,
		RandomSource random
	) {
		return GiuStairShapes.refresh(original, level, pos);
	}
}
