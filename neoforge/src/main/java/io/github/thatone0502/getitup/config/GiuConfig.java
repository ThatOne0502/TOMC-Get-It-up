package io.github.thatone0502.getitup.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import io.github.thatone0502.getitup.GetItUp;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 设置，来自 {@code config/getitup.json}。
 *
 * <p>注入哪些方块决定了每个方块的状态表有多大，而状态表的顺序就是全局
 * {@code Block.BLOCK_STATE_REGISTRY} 的 id 顺序 —— 那个 id 会进区块调色板与网络包，
 * 所以<b>客户端与服务端必须读到完全相同的文件</b>，否则同一个 id 在两边指向不同状态。
 * 改这个文件等于改存档格式：在别的设置下保存的存档会被读成错的方块。
 *
 * <p>注入发生在方块构造期，此时还拿不到 registry id，所以只能按<b>结构</b>判定：「额外方块」不是一个
 * id 列表，而是一个开关加一条状态数上限。
 */
public final class GiuConfig {
	private static final String FILE_NAME = "getitup.json";
	private static final String INJECT_SLABS = "inject_slabs";
	private static final String INJECT_STAIRS = "inject_stairs";
	private static final String DANGEROUS_INJECT_EVERY_BLOCK = "dangerous_inject_every_block";
	private static final String DANGEROUS_MAX_STATES = "dangerous_max_states_per_block";
	private static final String README = "__readme";
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

	private static final String[] README_LINES = {
		"giu_base 决定了「注入哪些方块」，而这个决定会改变全局方块状态 id 的排布。",
		"The set of injected blocks determines the global block-state id layout.",
		"客户端与服务端必须使用完全相同的本文件，否则同一个状态 id 在两边指向不同方块。",
		"Client and server must read an identical copy of this file.",
		"改动本文件的任何注入项之后必须完整重启，并且旧存档（在别的设置下保存的）会被读成错的方块。",
		"Changing any injection entry needs a full restart, and worlds saved under other settings will read as wrong blocks.",
		"dangerous_inject_every_block 默认关闭。打开它不再只是「多放几个方块」—— 它给整个注册表里的方块加状态。",
		"dangerous_inject_every_block is off by default and is not just a convenience switch. It grows every block's state table.",
	};

	private static boolean warned;
	/** 本侧读到的那一份；注入期只读一次，避免方块构造期反复碰磁盘。 */
	private static Values cached;

	/** 文件里的一整套设置。加一个字段就会自动进读写两边的逻辑。 */
	public record Values(
		boolean injectSlabs,
		boolean injectStairs,
		boolean dangerousInjectEveryBlock,
		int dangerousMaxStatesPerBlock
	) {
		public static final Values DEFAULT = new Values(true, true, false, 256);

		public Values withInjectSlabs(boolean value) {
			return new Values(value, injectStairs, dangerousInjectEveryBlock, dangerousMaxStatesPerBlock);
		}

		public Values withInjectStairs(boolean value) {
			return new Values(injectSlabs, value, dangerousInjectEveryBlock, dangerousMaxStatesPerBlock);
		}

		public Values withDangerousInjectEveryBlock(boolean value) {
			return new Values(injectSlabs, injectStairs, value, dangerousMaxStatesPerBlock);
		}

		public Values withDangerousMaxStatesPerBlock(int value) {
			return new Values(injectSlabs, injectStairs, dangerousInjectEveryBlock, value);
		}

		/**
		 * 两份设置会不会注入出不同的方块集合 —— 也就是状态 id 排布是否一致。
		 *
		 * <p>只有开着危险开关时，状态上限才影响结果（关着的时候台阶/楼梯不看它），所以关着就不比它，
		 * 免得因为一个无关字段报出「两边不一致」的假警报。
		 */
		public boolean sameInjection(Values other) {
			if (injectSlabs != other.injectSlabs || injectStairs != other.injectStairs) {
				return false;
			}

			if (dangerousInjectEveryBlock != other.dangerousInjectEveryBlock) {
				return false;
			}

			return !dangerousInjectEveryBlock || dangerousMaxStatesPerBlock == other.dangerousMaxStatesPerBlock;
		}

		/** 打日志用的一行摘要。 */
		public String fingerprint() {
			return "slabs=" + injectSlabs
				+ ",stairs=" + injectStairs
				+ ",every_block=" + dangerousInjectEveryBlock
				+ (dangerousInjectEveryBlock ? ",max_states=" + dangerousMaxStatesPerBlock : "");
		}
	}

	/** 文件位置。两侧都用同一个名字，因为两侧必须一致。 */
	public static Path file() {
		return FMLPaths.CONFIGDIR.get().resolve(FILE_NAME);
	}

	/** 本侧已缓存的那一份；第一次访问时才读盘。 */
	public static Values values() {
		if (cached == null) {
			cached = read();
		}

		return cached;
	}

	/** 强制重新读盘。配置界面用；方块构造期不用，因为那时只读一次就够了。 */
	public static Values reload() {
		cached = read();
		return cached;
	}

	/** 判断某个方块要不要注入 {@code giu_base}。{@code currentStates} 是它现有的状态数。 */
	public static boolean shouldInject(Block block, int currentStates) {
		Values values = values();

		if (block instanceof SlabBlock) {
			return values.injectSlabs();
		}

		if (block instanceof StairBlock) {
			return values.injectStairs();
		}

		if (!values.dangerousInjectEveryBlock()) {
			return false;
		}

		int max = values.dangerousMaxStatesPerBlock();
		return max <= 0 || currentStates <= max;
	}

	/** 读盘；文件不存在就写一份默认的。 */
	public static Values read() {
		Path file = file();
		if (!Files.isRegularFile(file)) {
			writeDefault(file);
			return Values.DEFAULT;
		}

		try {
			JsonObject json = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8)).getAsJsonObject();
			Values defaults = Values.DEFAULT;
			Values values = new Values(
				readBoolean(json, INJECT_SLABS, defaults.injectSlabs()),
				readBoolean(json, INJECT_STAIRS, defaults.injectStairs()),
				readBoolean(json, DANGEROUS_INJECT_EVERY_BLOCK, defaults.dangerousInjectEveryBlock()),
				readInt(json, DANGEROUS_MAX_STATES, defaults.dangerousMaxStatesPerBlock())
			);

			if (values.dangerousInjectEveryBlock()) {
				warnDangerous(values);
			}

			return values;
		} catch (IOException | RuntimeException e) {
			// 坏掉的配置文件不该带崩启动，也不该顺手改掉游戏行为：退回默认值，并且只说一次。
			if (!warned) {
				warned = true;
				GetItUp.LOGGER.warn("[giu] cannot read {}, using defaults: {}", file, e.toString());
			}

			return Values.DEFAULT;
		}
	}

	/** 写整套设置。返回是否真的落盘。 */
	public static boolean write(Values values) {
		Path file = file();

		try {
			Files.createDirectories(file.toAbsolutePath().getParent());
			Files.writeString(file, GSON.toJson(toJson(values)) + System.lineSeparator(), StandardCharsets.UTF_8);
			cached = values;
			GetItUp.LOGGER.info(
				"[giu] wrote config {} (slabs={}, stairs={}, every_block={}, max_states={})",
				file,
				values.injectSlabs(),
				values.injectStairs(),
				values.dangerousInjectEveryBlock(),
				values.dangerousMaxStatesPerBlock()
			);

			if (values.dangerousInjectEveryBlock()) {
				warnDangerous(values);
			}

			return true;
		} catch (IOException e) {
			GetItUp.LOGGER.warn("[giu] cannot write {}: {}", file, e.toString());
			return false;
		}
	}

	private static void warnDangerous(Values values) {
		GetItUp.LOGGER.warn(
			"[giu] dangerous_inject_every_block is ON (max states per block = {})."
				+ " The injected set no longer matches a default install: worlds saved under other settings,"
				+ " and clients whose config differs, will read the wrong blocks.",
			values.dangerousMaxStatesPerBlock() <= 0 ? "unlimited" : values.dangerousMaxStatesPerBlock()
		);
	}

	private static JsonObject toJson(Values values) {
		JsonObject json = new JsonObject();

		JsonArray readme = new JsonArray();
		for (String line : README_LINES) {
			readme.add(line);
		}

		json.add(README, readme);
		json.addProperty(INJECT_SLABS, values.injectSlabs());
		json.addProperty(INJECT_STAIRS, values.injectStairs());
		json.addProperty(DANGEROUS_INJECT_EVERY_BLOCK, values.dangerousInjectEveryBlock());
		json.addProperty(DANGEROUS_MAX_STATES, values.dangerousMaxStatesPerBlock());
		return json;
	}

	private static boolean readBoolean(JsonObject json, String key, boolean fallback) {
		return json.has(key) ? json.get(key).getAsBoolean() : fallback;
	}

	private static int readInt(JsonObject json, String key, int fallback) {
		return json.has(key) ? json.get(key).getAsInt() : fallback;
	}

	private static void writeDefault(Path file) {
		try {
			Files.createDirectories(file.toAbsolutePath().getParent());
			Files.writeString(file, GSON.toJson(toJson(Values.DEFAULT)) + System.lineSeparator(), StandardCharsets.UTF_8);
			GetItUp.LOGGER.info("[giu] wrote default config {}", file);
		} catch (IOException e) {
			GetItUp.LOGGER.warn("[giu] cannot write {}: {}", file, e.toString());
		}
	}

	private GiuConfig() {
	}
}
