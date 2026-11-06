package io.github.thatone0502.getitup.network;

import io.github.thatone0502.getitup.GetItUp;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * 「竖直放置键此刻是按着还是松开了」。
 *
 * <p>只有一个布尔，单向（客户端 → 服务端）。按下和松开都发，服务端据此覆盖该玩家的状态；
 * 没有回包 —— 客户端不需要知道服务端收到了。
 */
public record GiuModePayload(boolean active) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<GiuModePayload> TYPE =
		new CustomPacketPayload.Type<>(GetItUp.id("mode"));

	public static final StreamCodec<FriendlyByteBuf, GiuModePayload> STREAM_CODEC =
		StreamCodec.of(GiuModePayload::write, GiuModePayload::read);

	private static void write(FriendlyByteBuf buf, GiuModePayload payload) {
		buf.writeBoolean(payload.active);
	}

	private static GiuModePayload read(FriendlyByteBuf buf) {
		return new GiuModePayload(buf.readBoolean());
	}

	@Override
	public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
