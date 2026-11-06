package io.github.thatone0502.getitup.mixin;

import io.github.thatone0502.getitup.client.model.GiuModels;
import java.util.Map;
import net.minecraft.client.renderer.block.BlockStateModelSet;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 客户端模型表的入口：{@link BlockStateModelSet} 一造出来，就把 {@code giu_base != none} 的状态
 * 换成转过的那份模型。
 * <p>
 * 选这里是因为它<b>每次资源重载都会重新构造</b>（F3+T、换资源包都算），
 * 所以不需要额外监听重载事件，也不会重复包两层 —— 表每次都从烘焙结果重建。
 */
@Mixin(BlockStateModelSet.class)
public abstract class BlockStateModelSetMixin {
	@Mutable
	@Shadow
	@Final
	private Map<BlockState, BlockStateModel> modelByState;

	@Inject(method = "<init>", at = @At("TAIL"))
	private void giu$rotateModels(CallbackInfo callback) {
		Map<BlockState, BlockStateModel> wrapped = GiuModels.wrap(this.modelByState);

		if (wrapped != this.modelByState) {
			this.modelByState = wrapped;
		}
	}
}
