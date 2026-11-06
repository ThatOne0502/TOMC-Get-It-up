package io.github.thatone0502.getitup.client;

import io.github.thatone0502.getitup.GetItUp;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * 仅客户端的入口：收服务端推来的配置，装 Cloth 时把配置页挂到模组列表上。
 *
 * <p>工厂只在 Cloth 真的存在时注册，所以 {@link GiuConfigScreen} 在没有 Cloth 的客户端上永远不会被加载。
 */
@Mod(value = GetItUp.MOD_ID, dist = Dist.CLIENT)
public final class GiuClient {
	public GiuClient(IEventBus modBus, ModContainer container) {
		modBus.addListener(GiuKeys::register);
		modBus.addListener(GiuClientNet::registerHandlers);
		NeoForge.EVENT_BUS.addListener(GiuClientNet::onLoggingIn);
		NeoForge.EVENT_BUS.addListener(GiuClientNet::onLoggingOut);
		// 竖直放置键：tick 轮询 + 右键前补发一次，见 GiuKeySync。
		NeoForge.EVENT_BUS.addListener(GiuKeySync::onClientTick);
		NeoForge.EVENT_BUS.addListener(GiuKeySync::onRightClickBlock);

		if (ModList.get().isLoaded("cloth_config")) {
			container.registerExtensionPoint(IConfigScreenFactory.class,
				(IConfigScreenFactory) (modContainer, parent) -> GiuConfigScreen.create(parent));
		} else {
			GetItUp.LOGGER.info("cloth-config is not installed, so no config screen is offered; "
				+ "edit config/getitup.json by hand instead");
		}
	}
}
