package io.github.thatone0502.getitup.client.model;

import net.minecraft.client.model.geom.builders.UVPair;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import org.jspecify.annotations.Nullable;

/**
 * 原版某个面上的「贴图 + 它实际用到的贴图矩形」。
 *
 * <p>矩形是 sprite 内的 0..1 坐标，不是图集坐标：换算要经 {@link TextureAtlasSprite#getU(float)}，
 * 那才是烘焙时真正写进 {@code packedUV} 的东西。
 *
 * <p>之所以要区分「整张」与「用到的那一块」：部分方块把一张图当面板用（{@code smooth_stone_slab} 的侧面
 * 就是 {@code smooth_stone_slab_side} 的下半张，自带描边），重贴图时两种取法结果完全不同。
 */
public record GiuFaceUv(TextureAtlasSprite sprite, float minU, float minV, float maxU, float maxV) {
	/** 顶点坐标是烘焙时除以 16 的，容差只需要盖住 float 误差。 */
	private static final float EPSILON = 1.0E-4F;

	/** 从一张烘焙好的面上反推出它用了哪张贴图、以及用到的那块矩形。取不出来就返回 {@code null}。 */
	public static @Nullable GiuFaceUv of(BakedQuad quad) {
		TextureAtlasSprite sprite = quad.materialInfo().sprite();
		float spanU = sprite.getU1() - sprite.getU0();
		float spanV = sprite.getV1() - sprite.getV0();

		if (Math.abs(spanU) < EPSILON || Math.abs(spanV) < EPSILON) {
			return null;
		}

		float minU = Float.MAX_VALUE;
		float minV = Float.MAX_VALUE;
		float maxU = -Float.MAX_VALUE;
		float maxV = -Float.MAX_VALUE;

		for (int vertex = 0; vertex < BakedQuad.VERTEX_COUNT; vertex++) {
			long packed = quad.packedUV(vertex);
			float u = UVPair.unpackU(packed);
			float v = UVPair.unpackV(packed);
			minU = Math.min(minU, u);
			minV = Math.min(minV, v);
			maxU = Math.max(maxU, u);
			maxV = Math.max(maxV, v);
		}

		return new GiuFaceUv(
			sprite,
			(minU - sprite.getU0()) / spanU,
			(minV - sprite.getV0()) / spanV,
			(maxU - sprite.getU0()) / spanU,
			(maxV - sprite.getV0()) / spanV
		);
	}

	/** 矩形退化（只采样一个像素）时没有「一块图」可言，重贴图会把它压成一条线，不如不动。 */
	public boolean degenerate() {
		return Math.abs(maxU - minU) < EPSILON || Math.abs(maxV - minV) < EPSILON;
	}

	/**
	 * 这张图是不是「专门为半高面板画的」。
	 * <p>
	 * 判定看文件名：原版把这类图命名为 {@code *_slab_side}（1.21 里整个游戏只有 {@code smooth_stone_slab_side}
	 * 一张）。它们不是「大方块侧面的一部分」，而是自成一张半高面板，上下两半画的是两种东西，
	 * 按位置裁切会切出一道接缝，所以这类图要缩放铺满。
	 */
	public boolean panel() {
		return sprite.contents().name().getPath().endsWith("_slab_side");
	}

	/** 这块矩形是不是正好铺满整张贴图（= 这张面用的是「完整的原贴图」，而不是它的一部分）。 */
	public boolean full() {
		return minU <= EPSILON && minV <= EPSILON && maxU >= 1.0F - EPSILON && maxV >= 1.0F - EPSILON;
	}
}
