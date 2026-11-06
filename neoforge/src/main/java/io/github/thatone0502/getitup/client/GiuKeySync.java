package io.github.thatone0502.getitup.client;

import io.github.thatone0502.getitup.network.GiuModePayload;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * 把按键状态同步到服务端：只在变化时发，不做心跳。
 *
 * <p>每个客户端 tick 比一次，另外在右键方块事件里补发一次 —— 同一 tick 里先按键再点方块时，
 * tick 轮询还没跑到，服务端会以为没按键。补发同一个值，幂等。
 */
public final class GiuKeySync {
	private static boolean lastSent;

	public static void onClientTick(ClientTickEvent.Post event) {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.player == null) {
			return;
		}
		push(minecraft.player);
	}

	public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
		push(event.getEntity());
	}

	private static void push(Player player) {
		boolean down = GiuKeys.MODE.isDown();
		if (down == lastSent) {
			return;
		}
		lastSent = down;
		ClientPacketDistributor.sendToServer(new GiuModePayload(down));
	}

	public static void reset() {
		lastSent = false;
	}

	private GiuKeySync() {
	}
}
