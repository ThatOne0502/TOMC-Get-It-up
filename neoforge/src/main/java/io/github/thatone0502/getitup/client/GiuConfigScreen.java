package io.github.thatone0502.getitup.client;

import io.github.thatone0502.getitup.config.GiuConfig;

import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.Arrays;
import java.util.Locale;

/**
 * 模组列表里那个「配置」按钮背后的 Cloth Config 页面。
 *
 * <p>注入项的可编辑窗口只在主菜单：那些值在方块构造期定下、改完必须重启，所以在世界里改是假动作；
 * 进了世界之后页面只负责把<b>服务端</b>那一份念给玩家听，服务端权威因此天然成立。
 *
 * <p>分四类（注入对象 / 危险项 / 客户端渲染 / 权威与生效）。「客户端渲染」是本机的、不进存档、不参与同步，
 * 所以世界开着也能改（改完 {@code F3+T} 重载资源）。
 *
 * <p>从 {@link GiuClient} 通过 {@code IConfigScreenFactory} 扩展点注册；没有 Cloth 的客户端不会加载本类。
 */
public final class GiuConfigScreen {
	private GiuConfigScreen() {
	}

	/** 构建页面；{@code parent} 可以为 null（从模组列表之外打开时）。 */
	public static Screen create(Screen parent) {
		Minecraft minecraft = Minecraft.getInstance();
		// 没进世界才可编辑；进了世界（单人也是）一律只读。
		boolean editable = minecraft.level == null;

		GiuConfig.Values local = GiuConfig.reload();
		GiuConfig.Values pushed = GiuClientNet.serverValues();
		boolean showingServer = !editable && pushed != null;
		GiuConfig.Values shown = showingServer ? pushed : local;
		GiuRenderConfig.Values render = GiuRenderConfig.reload();

		// 保存回调在任何一个条目被碰之前就会来取一次，所以先落到数组里，保存时再读回去。
		boolean[] injectSlabs = {shown.injectSlabs()};
		boolean[] injectStairs = {shown.injectStairs()};
		boolean[] everyBlock = {shown.dangerousInjectEveryBlock()};
		int[] maxStates = {shown.dangerousMaxStatesPerBlock()};
		boolean[] retexture = {render.retexture()};
		GiuRenderConfig.Mode[] renderMode = {render.mode()};

		ConfigBuilder builder = ConfigBuilder.create()
			.setParentScreen(parent)
			.setTitle(Component.translatable("config.getitup.title"))
			.setSavingRunnable(() -> {
				if (editable) {
					GiuConfig.write(new GiuConfig.Values(injectSlabs[0], injectStairs[0], everyBlock[0], maxStates[0]));
				}

				GiuRenderConfig.write(new GiuRenderConfig.Values(retexture[0], renderMode[0]));
			});
		builder.setGlobalized(true);
		// 整页不再一刀切地灰：注入那几条自己决定能不能点（服务端权威），渲染那几条永远可改。
		builder.setEditable(true);

		ConfigEntryBuilder entries = builder.entryBuilder();

		// ---- 注入对象：只放真正决定「哪些方块会变」的两个开关 ----------------
		ConfigCategory targets = builder.getOrCreateCategory(
			Component.translatable("config.getitup.category.targets"));
		addReadonlyNotice(targets, entries, editable);
		targets.addEntry(serverEntry(entries.startBooleanToggle(
					label("inject_slabs"),
					shown.injectSlabs())
			.setDefaultValue(GiuConfig.Values.DEFAULT.injectSlabs())
			.setTooltip(tooltip("inject_slabs"))
			.setSaveConsumer(value -> injectSlabs[0] = value)
			.build(), editable));
		targets.addEntry(serverEntry(entries.startBooleanToggle(
					label("inject_stairs"),
					shown.injectStairs())
			.setDefaultValue(GiuConfig.Values.DEFAULT.injectStairs())
			.setTooltip(tooltip("inject_stairs"))
			.setSaveConsumer(value -> injectStairs[0] = value)
			.build(), editable));

		// ---- 危险项：单独一类，免得它跟普通开关挨着、被顺手打开 --------------
		ConfigCategory dangerous = builder.getOrCreateCategory(
			Component.translatable("config.getitup.category.dangerous"));
		addReadonlyNotice(dangerous, entries, editable);
		dangerous.addEntry(serverEntry(entries.startBooleanToggle(
					label("dangerous_inject_every_block"),
					shown.dangerousInjectEveryBlock())
			.setDefaultValue(GiuConfig.Values.DEFAULT.dangerousInjectEveryBlock())
			.setTooltip(tooltip("dangerous_inject_every_block"))
			.setSaveConsumer(value -> everyBlock[0] = value)
			.build(), editable));
		dangerous.addEntry(serverEntry(entries.startIntSlider(
					label("dangerous_max_states_per_block"),
					shown.dangerousMaxStatesPerBlock(),
					0,
					4096)
			.setDefaultValue(GiuConfig.Values.DEFAULT.dangerousMaxStatesPerBlock())
			.setTooltip(tooltip("dangerous_max_states_per_block"))
			.setSaveConsumer(value -> maxStates[0] = value)
			.build(), editable));

		// ---- 客户端渲染：本机设置，世界开着也能改，改完 F3+T 生效 -----------------
		ConfigCategory renderCategory = builder.getOrCreateCategory(
			Component.translatable("config.getitup.category.render"));
		renderCategory.addEntry(notice(entries, "config.getitup.render.local"));
		renderCategory.addEntry(notice(entries, "config.getitup.render.reload"));
		renderCategory.addEntry(entries.startBooleanToggle(
					label("retexture"),
					retexture[0])
			.setDefaultValue(GiuRenderConfig.Values.DEFAULT.retexture())
			.setTooltip(tooltip("retexture"))
			.setSaveConsumer(value -> retexture[0] = value)
			.build());
		renderCategory.addEntry(entries.startEnumSelector(
					label("texture_mode"),
					GiuRenderConfig.Mode.class,
					renderMode[0])
			.setDefaultValue(GiuRenderConfig.Values.DEFAULT.mode())
			.setEnumNameProvider(mode -> Component.translatable("config.getitup.texture_mode." + mode.name().toLowerCase(Locale.ROOT)))
			.setTooltip(tooltip("texture_mode"))
			.setSaveConsumer(value -> renderMode[0] = value)
			.build());

		// ---- 权威与生效：全是文字，回答「现在为什么是这样」 -------------------
		ConfigCategory authority = builder.getOrCreateCategory(
			Component.translatable("config.getitup.category.authority"));
		authority.addEntry(notice(entries, "config.getitup.restart"));

		if (editable) {
			authority.addEntry(notice(entries, "config.getitup.authority.local"));
		} else {
			authority.addEntry(notice(entries, "config.getitup.readonly.in_game"));
			authority.addEntry(notice(entries, showingServer
				? "config.getitup.authority.server"
				: "config.getitup.authority.pending"));

			if (showingServer && !local.sameInjection(pushed)) {
				authority.addEntry(notice(entries, "config.getitup.mismatch"));
			}
		}

		return builder.build();
	}

	/** 只读时在分类顶部插一行说明，否则玩家不知道条目为什么点不动。 */
	private static void addReadonlyNotice(ConfigCategory category, ConfigEntryBuilder entries, boolean editable) {
		if (!editable) {
			category.addEntry(notice(entries, "config.getitup.readonly.in_game"));
		}
	}

	/**
	 * 注入类条目：世界开着时不可点。
	 * <p>
	 * 注入名单是方块构造期定下的，改完必须重启；而且它决定的是<b>服务端</b>的方块状态表，
	 * 客户端改它毫无意义 —— 所以这里的灰不是界面偷懒，而是把「改了也不会生效」直接摆在脸上。
	 */
	private static <T extends AbstractConfigListEntry<?>> T serverEntry(T entry, boolean editable) {
		entry.setEditable(editable);

		return entry;
	}

	/** 条目标题：{@code config.getitup.<key>}。 */
	private static Component label(String key) {
		return Component.translatable("config.getitup." + key);
	}

	/**
	 * 条目说明：{@code config.getitup.<key>.tooltip}，其中 {@code \n} 变成真正的换行 ——
	 * Cloth 会把每个 {@code Component} 参数渲染成一行，所以先切开再逐个传是唯一可靠的换行方式。
	 */
	private static Component[] tooltip(String key) {
		return Arrays.stream(Component.translatable("config.getitup." + key + ".tooltip").getString().split("\n"))
			.map(Component::literal)
			.toArray(Component[]::new);
	}

	/** 一段纯文字说明（不可编辑）。 */
	private static AbstractConfigListEntry<?> notice(ConfigEntryBuilder entries, String key) {
		return entries.startTextDescription(Component.translatable(key)).build();
	}
}
