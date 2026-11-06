package io.github.thatone0502.getitup.mixin;

import io.github.thatone0502.getitup.Giu;
import java.util.Map;
import java.util.function.Function;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.Property;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 属性注入点。
 * <p>
 * {@code Block} 构造器里是：{@code builder = new Builder<>(this); createBlockStateDefinition(builder);
 * this.stateDefinition = builder.create(...); this.registerDefaultState(stateDefinition.any());}
 * —— 也就是说 {@code create} 之前是这个方块最后一次能长属性的机会，注入之后默认状态才会带上 {@code giu_base=none}。
 */
@Mixin(StateDefinition.Builder.class)
public abstract class StateDefinitionBuilderMixin {
	@Shadow
	@Final
	private Object owner;

	@Shadow
	@Final
	private Map<String, Property<?>> properties;

	@Inject(method = "create", at = @At("HEAD"))
	private void getitup$injectGiuBase(
		Function<?, ?> defaultState,
		StateDefinition.Factory<?, ?> factory,
		CallbackInfoReturnable<StateDefinition<?, ?>> cir
	) {
		if (!(this.owner instanceof Block block) || this.properties.containsKey(Giu.propertyName())) {
			return;
		}

		// 现有的状态数（各属性取值数之积），给危险模式的上限用：超了就跳过，别把红石线那种组合爆炸的方块也乘五。
		if (Giu.isTarget(block, stateCount())) {
			((StateDefinition.Builder<?, ?>) (Object) this).add(Giu.GIU_BASE);
			Giu.onInjected(block);
		}
	}

	private int stateCount() {
		long count = 1;

		for (Property<?> property : this.properties.values()) {
			count *= property.getPossibleValues().size();

			if (count > Integer.MAX_VALUE) {
				return Integer.MAX_VALUE;
			}
		}

		return (int) count;
	}
}
