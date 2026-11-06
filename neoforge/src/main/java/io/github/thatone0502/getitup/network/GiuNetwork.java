package io.github.thatone0502.getitup.network;

import io.github.thatone0502.getitup.GetItUp;
import io.github.thatone0502.getitup.GiuMode;
import io.github.thatone0502.getitup.config.GiuConfig;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * 服务端权威的那一半。
 *
 * <p>玩家上线时把服务端那份推过去（配置页显示的就是它），并在玩家自述时比对一次 ——
 * 注入范围决定全局状态 id，不一致会让客户端把状态解释成错的方块，这种事必须能从日志里查出来。
 * 线上没有「改配置」的通道，这就是服务端权威最省事的实现。
 */
public final class GiuNetwork {
	/**
	 * 网络版本。与配置无关：配置不一致由下面那句 WARN 负责，这个版本号只用来挡「对方的 mod 缺这个 payload」。
	 */
	private static final String VERSION = "1";

	public static void registerPayloads(RegisterPayloadHandlersEvent event) {
		event.registrar(VERSION)
			.playBidirectional(GiuConfigPayload.TYPE, GiuConfigPayload.STREAM_CODEC, GiuNetwork::onClientSelfReport)
			.playToServer(GiuModePayload.TYPE, GiuModePayload.STREAM_CODEC, GiuNetwork::onMode);
	}

	/** 服务端：记下这个玩家此刻按没按住竖直放置键。 */
	private static void onMode(GiuModePayload payload, IPayloadContext context) {
		GiuMode.set(context.player().getUUID(), payload.active());
	}

	/** 服务端：人走了就把状态删掉，别让 UUID 集合无限长。 */
	public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
		GiuMode.clear(event.getEntity().getUUID());
	}

	/** 服务端：把权威的一份推给刚进来的玩家。 */
	public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
		if (event.getEntity() instanceof ServerPlayer player) {
			PacketDistributor.sendToPlayer(player, GiuConfigPayload.of(GiuConfig.values()));
		}
	}

	/** 服务端：收客户端自述，只做比对。 */
	private static void onClientSelfReport(GiuConfigPayload payload, IPayloadContext context) {
		GiuConfig.Values server = GiuConfig.values();
		GiuConfig.Values client = payload.toValues();

		if (!server.sameInjection(client)) {
			GetItUp.LOGGER.warn(
				"[giu] {} has a different injection config (server: {}, client: {})."
					+ " Block-state ids only line up when both sides agree, so this client will read some blocks"
					+ " as the wrong ones. Fix config/getitup.json on the client side.",
				context.player().getScoreboardName(),
				server.fingerprint(),
				client.fingerprint()
			);
		}
	}

	private GiuNetwork() {
	}
}
