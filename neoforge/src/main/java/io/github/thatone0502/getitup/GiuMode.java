package io.github.thatone0502.getitup;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.world.entity.player.Player;

/**
 * 谁按住了「竖直放置」键 —— 服务端侧的那一份。
 *
 * <p>键在客户端、放置判定在服务端，所以状态由 {@code GiuModePayload} 同步过来；单人模式那条 memory
 * connection 一样会序列化，没有「本地直接读按键」的捷径。只存按下/松开，松开即恢复原版行为。
 */
public final class GiuMode {
	private static final Set<UUID> ACTIVE = ConcurrentHashMap.newKeySet();

	public static void set(UUID player, boolean active) {
		if (active) {
			ACTIVE.add(player);
		} else {
			ACTIVE.remove(player);
		}
	}

	public static boolean isActive(Player player) {
		return player != null && ACTIVE.contains(player.getUUID());
	}

	public static void clear(UUID player) {
		ACTIVE.remove(player);
	}

	public static void clearAll() {
		ACTIVE.clear();
	}

	private GiuMode() {
	}
}
