package io.github.thatone0502.getitup.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import io.github.thatone0502.getitup.Giu;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * 形状旋转点 —— 只挂这一个方法就够。
 *
 * <p>碰撞箱、遮挡、描边、支撑面在 26.1.2 里全从这里的 {@code getShape} 派生（含逐状态的碰撞缓存），
 * 所以不要再给 {@code getCollisionShape} 加一层旋转，那会转两次。
 */
@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class BlockStateBaseMixin {
	@ModifyReturnValue(
		method = "getShape(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/phys/shapes/CollisionContext;)Lnet/minecraft/world/phys/shapes/VoxelShape;",
		at = @At("RETURN")
	)
	private VoxelShape getitup$rotateShape(VoxelShape original, BlockGetter level, BlockPos pos, CollisionContext context) {
		return Giu.apply((BlockBehaviour.BlockStateBase) (Object) this, original);
	}
}
