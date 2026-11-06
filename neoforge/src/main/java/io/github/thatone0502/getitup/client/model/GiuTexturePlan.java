package io.github.thatone0502.getitup.client.model;

import io.github.thatone0502.getitup.client.GiuRenderConfig;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;

import org.jspecify.annotations.Nullable;

import java.util.Map;

/**
 * 「这一轮重贴图要怎么做」——传到 {@link GiuQuadRotation} 里的一点点决定；拿不到就等于不做重贴图。
 *
 * @param substitute 面板型贴图要不要换成整张图（见 {@link #resolve}）；双层台阶必须关掉，
 *                   它的原版外观本来就是「两张半砖面叠起来 + 一道接缝」。
 */
public record GiuTexturePlan(GiuRenderConfig.Mode mode, boolean substitute) {
	public GiuTexturePlan(GiuRenderConfig.Mode mode) {
		this(mode, true);
	}

	/**
	 * 按具体状态把决定再修一遍。
	 *
	 * <p>{@code type=double} 的台阶几何上就是一整块方块，原版给它的模型是「两块半砖叠起来」
	 * （{@code smooth_stone_slab} 甚至专门有个 {@code smooth_stone_slab_double}），
	 * 那道接缝是它该有的样子，所以这里关掉面板替换，让它照原样裁切。
	 */
	public GiuTexturePlan forState(BlockState state) {
		if (!this.substitute) {
			return this;
		}

		boolean doubled = state.hasProperty(SlabBlock.TYPE) && state.getValue(SlabBlock.TYPE) == SlabType.DOUBLE;

		return doubled ? new GiuTexturePlan(this.mode, false) : this;
	}

	/**
	 * 挑出真正要贴在这张面上的原版面：多数情况就是法线相同的那张面，只有<b>专门画的半高面板</b>
	 * （{@code *_slab_side}）要换成该方块的整张图 —— 那张图上下两半是两种东西，拿来裁必然露接缝。
	 */
	public GiuFaceUv resolve(GiuFaceUv own, Map<Direction, GiuFaceUv> all) {
		if (!this.substitute || this.mode != GiuRenderConfig.Mode.AUTO || !own.panel()) {
			return own;
		}

		GiuFaceUv full = fullImage(all);

		return full == null ? own : full;
	}

	/**
	 * 这张面要不要「缩放铺满」而不是「按世界坐标裁切」。
	 *
	 * <p>原版是裁切（{@code slab.json} 侧面 UV {@code [0,8,16,16]} 正是侧面下半张），图案不变形；
	 * 只有退到「只剩面板图」时才缩放 —— 会变形，但至少不切出接缝。
	 */
	public boolean refitFor(GiuFaceUv face) {
		return switch (this.mode) {
			case REFIT -> true;
			case PROJECT -> false;
			case AUTO -> face.panel();
		};
	}

	/** 这个方块身上那张「完整的一张图」——优先上下两面，因为它们引用的是完整方块的原贴图。 */
	private static @Nullable GiuFaceUv fullImage(Map<Direction, GiuFaceUv> all) {
		for (Direction direction : new Direction[] {Direction.UP, Direction.DOWN}) {
			GiuFaceUv face = all.get(direction);

			if (face != null && face.full() && !face.panel()) {
				return face;
			}
		}

		for (GiuFaceUv face : all.values()) {
			if (face.full() && !face.panel()) {
				return face;
			}
		}

		return null;
	}
}
