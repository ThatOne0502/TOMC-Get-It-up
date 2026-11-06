package io.github.thatone0502.getitup.client;

import io.github.thatone0502.getitup.config.GiuConfig;
import io.github.thatone0502.getitup.network.GiuConfigPayload;

import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.Nullable;

/**
 * 客户端这一半：收服务端推来的配置，并在上线时自述一份。
 *
 * <p>收下的那份只用于「显示」—— 玩家在游戏里能看到的必须是真正生效的那一套，也就是服务端的。
 */
public final class GiuClientNet {
	/** 服务端推来的权威值；还没收到时为 null。 */
	private static volatile @Nullable GiuConfig.Values serverValues;

	public static void registerHandlers(RegisterClientPayloadHandlersEvent event) {
		event.register(GiuConfigPayload.TYPE, GiuClientNet::onServerPush);
	}

	/** 服务端推来的那一份。连接期间才有值。 */
	public static @Nullable GiuConfig.Values serverValues() {
		return serverValues;
	}

	private static void onServerPush(GiuConfigPayload payload, IPayloadContext context) {
		serverValues = payload.toValues();
	}

	/** 上线时自述本机设置，让服务端能在日志里点名不一致的客户端。 */
	public static void onLoggingIn(ClientPlayerNetworkEvent.LoggingIn event) {
		ClientPacketDistributor.sendToServer(GiuConfigPayload.of(GiuConfig.values()));
	}

	public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
		serverValues = null;
	}

	private GiuClientNet() {
	}
}
