package io.github.thatone0502.getitup.network;

import io.github.thatone0502.getitup.GetItUp;
import io.github.thatone0502.getitup.config.GiuConfig;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * 配置的线上形态。
 *
 * <p>同一个类型两个方向都用：服务端上线时把它推给玩家（玩家看到的配置页内容），客户端上线时也自述一份
 * （只为了让服务端能在日志里指出谁的文件不一致，见 {@link GiuNetwork}）。
 *
 * <p>字段一一对应 {@link GiuConfig.Values}，不再另设 id：整套值本来就只有四个。
 */
public record GiuConfigPayload(
	boolean injectSlabs,
	boolean injectStairs,
	boolean dangerousInjectEveryBlock,
	int dangerousMaxStatesPerBlock
) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<GiuConfigPayload> TYPE =
		new CustomPacketPayload.Type<>(GetItUp.id("config"));

	public static final StreamCodec<FriendlyByteBuf, GiuConfigPayload> STREAM_CODEC =
		StreamCodec.of(GiuConfigPayload::write, GiuConfigPayload::read);

	public static GiuConfigPayload of(GiuConfig.Values values) {
		return new GiuConfigPayload(
			values.injectSlabs(),
			values.injectStairs(),
			values.dangerousInjectEveryBlock(),
			values.dangerousMaxStatesPerBlock()
		);
	}

	public GiuConfig.Values toValues() {
		return new GiuConfig.Values(injectSlabs, injectStairs, dangerousInjectEveryBlock, dangerousMaxStatesPerBlock);
	}

	private static void write(FriendlyByteBuf buf, GiuConfigPayload payload) {
		buf.writeBoolean(payload.injectSlabs);
		buf.writeBoolean(payload.injectStairs);
		buf.writeBoolean(payload.dangerousInjectEveryBlock);
		buf.writeVarInt(payload.dangerousMaxStatesPerBlock);
	}

	private static GiuConfigPayload read(FriendlyByteBuf buf) {
		return new GiuConfigPayload(buf.readBoolean(), buf.readBoolean(), buf.readBoolean(), buf.readVarInt());
	}

	@Override
	public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
