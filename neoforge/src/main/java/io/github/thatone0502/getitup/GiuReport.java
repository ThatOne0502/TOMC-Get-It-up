package io.github.thatone0502.getitup;

import io.github.thatone0502.getitup.config.GiuConfig;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

/**
 * 启动报告：把「这次实际注入了哪些方块、状态表被撑到多大」打进日志。
 *
 * <p>这不是调试噪声，而是存档兼容性的指纹：注入集合决定全局状态 id 的排布，所以这一行就是
 * 「这个存档能不能被另一份配置打开」的判据。出事时第一眼看的就是它。
 */
public final class GiuReport {
	public static void onServerStarted(ServerStartedEvent event) {
		int slabs = 0;
		int stairs = 0;
		int others = 0;
		int totalStates = 0;
		int injectedStates = 0;

		for (Block block : BuiltInRegistries.BLOCK) {
			int states = block.getStateDefinition().getPossibleStates().size();
			totalStates += states;

			if (!block.getStateDefinition().getProperties().contains(Giu.GIU_BASE)) {
				continue;
			}

			injectedStates += states;

			if (block instanceof SlabBlock) {
				slabs++;
			} else if (block instanceof StairBlock) {
				stairs++;
			} else {
				others++;
			}
		}

		// 注入后每个目标方块的状态数是原来的 5 倍，所以原始数就是当前数除以 5。
		int originalInjectedStates = injectedStates / Giu.GIU_BASE.getPossibleValues().size();
		int addedStates = injectedStates - originalInjectedStates;

		GetItUp.LOGGER.info(
			"[giu] giu_base on {} blocks (slabs={}, stairs={}, extra={}); block states {} -> {} (+{})",
			slabs + stairs + others,
			slabs,
			stairs,
			others,
			totalStates - addedStates,
			totalStates,
			addedStates
		);

		if (others > 0) {
			GetItUp.LOGGER.warn(
				"[giu] dangerous_inject_every_block is active: {} non-slab/non-stair blocks carry giu_base."
					+ " This layout only matches worlds and clients configured identically.",
				others
			);
		}

		GiuConfig.values();
	}

	private GiuReport() {
	}
}
