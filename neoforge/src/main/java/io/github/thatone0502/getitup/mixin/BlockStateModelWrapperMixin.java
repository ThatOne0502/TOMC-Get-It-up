package io.github.thatone0502.getitup.mixin;

import io.github.thatone0502.getitup.client.model.GiuModels;
import java.util.List;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.block.model.BlockStateModelWrapper;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * 渲染真正经过的那道门。
 *
 * <p>{@link BlockStateModelWrapper#update} 先向渲染状态要一个空列表，再让绑死在 wrapper 里的
 * {@code BlockStateModel} 往里塞模型片 —— 模型换不掉，但「塞列表」这个动作可以接管：塞完就地转 90°。
 * 这样不依赖「状态 → 模型」那张表是否可替换。
 */
@Mixin(BlockStateModelWrapper.class)
public abstract class BlockStateModelWrapperMixin {
	@Redirect(
		method = "update",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/client/renderer/block/dispatch/BlockStateModel;collectParts(Lnet/minecraft/client/renderer/block/BlockAndTintGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/util/RandomSource;Ljava/util/List;)V"
		)
	)
	private void giu$collectRotated(BlockStateModel model, BlockAndTintGetter level, BlockPos pos, BlockState state,
			RandomSource random, List<BlockStateModelPart> collector) {
		int start = collector.size();
		model.collectParts(level, pos, state, random, collector);
		GiuModels.rotateInPlace(state, collector, start);
	}
}
