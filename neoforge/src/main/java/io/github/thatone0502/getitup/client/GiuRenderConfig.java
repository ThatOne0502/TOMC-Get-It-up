package io.github.thatone0502.getitup.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import io.github.thatone0502.getitup.GetItUp;
import io.github.thatone0502.getitup.client.model.GiuTexturePlan;

import net.neoforged.fml.loading.FMLPaths;

import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 客户端本地渲染设置，来自 {@code config/getitup-client.json}。
 *
 * <p>与 {@code getitup.json} 相反：那边影响方块状态 id、必须双端一致；这边只管「自己的显卡把哪张图的
 * 哪一块画到哪个面上」，不碰状态、不碰存档、不参与同步，所以可以各调各的。
 *
 * <p>{@code auto} 的取样跟原版一致 —— 按世界坐标裁切，图案绝不拉伸；例外是专门画的半高面板贴图
 * （{@code *_slab_side}），它上下两半是两种东西，切开会露接缝，所以改成缩放铺满。
 */
public final class GiuRenderConfig {
	private static final String FILE_NAME = "getitup-client.json";
	private static final String RETEXTURE = "retexture";
	private static final String TEXTURE_MODE = "texture_mode";
	private static final String README = "__readme";
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

	private static final String[] README_LINES = {
		"客户端本地渲染设置。只影响你自己看到的贴图，不影响存档，也不需要与服务端一致。",
		"Client-side rendering only. It never touches block states or world data, and needs no server agreement.",
		"retexture：竖直方块的贴图是否仍然「正着」附着到对应的世界方向。关掉就恢复成贴图跟着几何一起转。",
		"retexture: whether upright blocks keep their textures attached to the world direction instead of rotating along.",
		"texture_mode：auto / project / refit。auto = 按世界坐标裁切，只有专门画的面板贴图（文件名以 _slab_side 结尾）才缩放；",
		"texture_mode: auto / project / refit. auto cuts by world position, except dedicated panel textures (sprite names ending in _slab_side), which are stretched.",
		"project = 一律按世界坐标裁切（图案不变形，与邻块连续）；refit = 一律把原版面用到的那块矩形拉伸铺满（面板图不会出现接缝，但会被拉长压扁）。",
		"project cuts the whole texture by world position; refit stretches the source rectangle to fill the new face.",
		"改动后按 F3+T 重载资源即可生效，不需要重启游戏。",
		"Press F3+T to reload resources after editing; no game restart needed.",
	};

	private static Values cached;
	private static boolean warned;

	/** 取样方式。 */
	public enum Mode {
		/** 逐张贴图判断：普通贴图裁切，专门画出来的半高面板贴图（{@code *_slab_side}）缩放。 */
		AUTO,
		PROJECT,
		REFIT;

		static Mode byName(String name, Mode fallback) {
			if (name == null) {
				return fallback;
			}

			for (Mode mode : values()) {
				if (mode.name().equalsIgnoreCase(name)) {
					return mode;
				}
			}

			return fallback;
		}
	}

	public record Values(boolean retexture, Mode mode) {
		public static final Values DEFAULT = new Values(true, Mode.AUTO);

		public Values withRetexture(boolean value) {
			return new Values(value, mode);
		}

		public Values withMode(Mode value) {
			return new Values(retexture, value);
		}
	}

	/** 这一轮重贴图的总策略；{@code null} 表示不重贴图（贴图跟着几何一起转）。 */
	public static @Nullable GiuTexturePlan planFor() {
		Values values = values();

		if (!values.retexture()) {
			return null;
		}

		return new GiuTexturePlan(values.mode());
	}

	public static Path file() {
		return FMLPaths.CONFIGDIR.get().resolve(FILE_NAME);
	}

	public static Values values() {
		if (cached == null) {
			cached = read();
		}

		return cached;
	}

	public static Values reload() {
		cached = read();

		return cached;
	}

	public static void write(Values values) {
		Path path = file();

		try {
			Files.createDirectories(path.getParent());
			Files.writeString(path, GSON.toJson(toJson(values)), StandardCharsets.UTF_8);
		} catch (IOException exception) {
			warnOnce("failed to write " + path, exception);
		}

		cached = values;
	}

	private static Values read() {
		Path path = file();

		if (!Files.isRegularFile(path)) {
			writeDefault(path);

			return Values.DEFAULT;
		}

		try {
			String text = Files.readString(path, StandardCharsets.UTF_8);
			JsonObject root = JsonParser.parseString(text).getAsJsonObject();

			return new Values(
				readBoolean(root, RETEXTURE, Values.DEFAULT.retexture()),
				Mode.byName(readString(root, TEXTURE_MODE), Values.DEFAULT.mode())
			);
		} catch (IOException | RuntimeException exception) {
			warnOnce("failed to read " + path + ", using defaults", exception);

			return Values.DEFAULT;
		}
	}

	private static void writeDefault(Path path) {
		try {
			Files.createDirectories(path.getParent());
			Files.writeString(path, GSON.toJson(toJson(Values.DEFAULT)), StandardCharsets.UTF_8);
			GetItUp.LOGGER.info("[giu] wrote default render config {}", path);
		} catch (IOException exception) {
			warnOnce("failed to write default " + path, exception);
		}
	}

	private static JsonObject toJson(Values values) {
		JsonObject root = new JsonObject();
		JsonArray readme = new JsonArray();

		for (String line : README_LINES) {
			readme.add(line);
		}

		root.add(README, readme);
		root.addProperty(RETEXTURE, values.retexture());
		root.addProperty(TEXTURE_MODE, values.mode().name().toLowerCase());
		root.addProperty("__note_mode", "auto = cut by world position; panel textures (*_slab_side) fall back to the block's full texture");

		return root;
	}

	private static boolean readBoolean(JsonObject root, String key, boolean fallback) {
		try {
			return root.has(key) ? root.get(key).getAsBoolean() : fallback;
		} catch (RuntimeException exception) {
			return fallback;
		}
	}

	private static @Nullable String readString(JsonObject root, String key) {
		try {
			return root.has(key) ? root.get(key).getAsString() : null;
		} catch (RuntimeException exception) {
			return null;
		}
	}

	private static void warnOnce(String message, Exception exception) {
		if (!warned) {
			warned = true;
			GetItUp.LOGGER.warn("[giu] {}", message, exception);
		}
	}
}
