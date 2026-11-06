package io.github.thatone0502.getitup;

import io.github.thatone0502.getitup.network.GiuNetwork;

import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(GetItUp.MOD_ID)
public final class GetItUp {
	public static final String MOD_ID = "getitup";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public GetItUp(IEventBus modBus) {
		// 提前触发 giu_base 属性表与旋转表的构造：映射出错就在启动时炸，而不是等玩家去挖方块。
		Giu.ready();
		LOGGER.info("Get It up loaded, giu_base = {}", Giu.GIU_BASE.getPossibleValues());

		modBus.addListener(GiuNetwork::registerPayloads);
		// 玩家上线时把服务端这一份配置推过去：这就是「服务端权威」的全部实现，线上没有反过来改配置的通道。
		NeoForge.EVENT_BUS.addListener(GiuNetwork::onPlayerLoggedIn);
		NeoForge.EVENT_BUS.addListener(GiuNetwork::onPlayerLoggedOut);
		// 启动后打一行注入指纹（几个方块、多少状态），存档兼容性靠它定位。
		NeoForge.EVENT_BUS.addListener(GiuReport::onServerStarted);
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
