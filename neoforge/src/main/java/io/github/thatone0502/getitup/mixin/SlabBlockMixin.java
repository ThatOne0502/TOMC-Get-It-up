package io.github.thatone0502.getitup.mixin;

import io.github.thatone0502.getitup.GiuPlacement;

import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 台阶的放置改写。
 *
 * <p>挂在 {@code RETURN} 而不是 {@code HEAD}：先让原版把 {@code waterlogged} 填好、把「点到的位置是上半还是下半」
 * 算完、把「同种台阶已存在 → {@code type=double}」这条也走完，最后只覆盖方向字段。挂在 HEAD 自己重算一遍
 * 会和那套叠层逻辑打架。
 */
@Mixin(SlabBlock.class)
public abstract class SlabBlockMixin {
	@Inject(method = "getStateForPlacement", at = @At("RETURN"), cancellable = true)
	private void giu$rewritePlacement(BlockPlaceContext context, CallbackInfoReturnable<@Nullable BlockState> cir) {
		BlockState original = cir.getReturnValue();
		BlockState rewritten = GiuPlacement.adjustSlab(context, original);
		if (rewritten != original) {
			cir.setReturnValue(rewritten);
		}
	}
}
