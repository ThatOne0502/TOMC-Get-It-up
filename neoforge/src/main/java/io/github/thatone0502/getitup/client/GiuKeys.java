package io.github.thatone0502.getitup.client;

import io.github.thatone0502.getitup.GetItUp;

import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.KeyMapping;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import org.lwjgl.glfw.GLFW;

/**
 * 「竖直放置」键。
 *
 * <p>默认 {@code LEFT_SHIFT}，手感等于原版潜行放置，但判定完全独立 —— 不用原版潜行是因为很多模组
 * 已经拿「潜行 + 右键」做自己的特殊放置模式，复用同一个键只会打架。
 *
 * <p>与「是否注入属性」无关：键按着只是要求<b>这一次放置</b>走竖直分支，松开立刻恢复原版。
 */
public final class GiuKeys {
	private static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(GetItUp.id("getitup"));

	public static final KeyMapping MODE = new KeyMapping(
		"key.getitup.mode",
		InputConstants.Type.KEYSYM,
		GLFW.GLFW_KEY_LEFT_SHIFT,
		CATEGORY
	);

	public static void register(RegisterKeyMappingsEvent event) {
		event.register(MODE);
	}

	private GiuKeys() {
	}
}
