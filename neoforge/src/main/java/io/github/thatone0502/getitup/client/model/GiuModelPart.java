package io.github.thatone0502.getitup.client.model;

import com.mojang.math.OctahedralGroup;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.Direction;
import org.jspecify.annotations.Nullable;

/**
 * 一个「转过」的模型片。
 *
 * <p>模型片按<b>剔除面</b>分桶：桶 {@code d} 里的面只在 {@code d} 那一侧没有实心邻居时才画。
 * 几何整体转了 {@code R}，桶也要跟着搬，所以问旋转后的桶 {@code d} 时取原模型里 {@code R⁻¹(d)} 的桶。
 * 结果按方向缓存 —— 每帧会被问很多次。
 *
 * <p>重贴图要知道「原模型里法线为某方向的那张面用的是哪张贴图、哪块矩形」，第一次用时扫一遍缓存。
 */
public final class GiuModelPart implements BlockStateModelPart {
	private final BlockStateModelPart delegate;
	private final OctahedralGroup rotation;
	private final OctahedralGroup inverse;
	private final @Nullable GiuTexturePlan plan;
	private final Map<Direction, List<BakedQuad>> culled = new EnumMap<>(Direction.class);
	private @Nullable Map<Direction, GiuFaceUv> faces;
	private @Nullable List<BakedQuad> unculled;

	public GiuModelPart(BlockStateModelPart delegate, OctahedralGroup rotation, @Nullable GiuTexturePlan plan) {
		this.delegate = delegate;
		this.rotation = rotation;
		this.inverse = rotation.inverse();
		this.plan = plan;
	}

	@Override
	public List<BakedQuad> getQuads(@Nullable Direction direction) {
		if (direction == null) {
			List<BakedQuad> cached = this.unculled;

			if (cached == null) {
				cached = this.rotate(this.delegate.getQuads(null));
				this.unculled = cached;
			}

			return cached;
		}

		return this.culled.computeIfAbsent(direction, rotated -> this.rotate(this.delegate.getQuads(this.inverse.rotate(rotated))));
	}

	private List<BakedQuad> rotate(List<BakedQuad> quads) {
		if (this.plan == null) {
			return GiuQuadRotation.rotateAll(quads, this.rotation);
		}

		return GiuQuadRotation.rotateAll(quads, this.rotation, this.faces(), this.plan);
	}

	/** 原模型里「每个世界法线用哪张贴图、哪块矩形」。只扫一次。 */
	private Map<Direction, GiuFaceUv> faces() {
		Map<Direction, GiuFaceUv> found = this.faces;

		if (found == null) {
			found = new EnumMap<>(Direction.class);
			collect(found, this.delegate.getQuads(null));

			for (Direction direction : Direction.values()) {
				collect(found, this.delegate.getQuads(direction));
			}

			this.faces = found;
		}

		return found;
	}

	private static void collect(Map<Direction, GiuFaceUv> target, List<BakedQuad> quads) {
		for (BakedQuad quad : quads) {
			Direction direction = quad.direction();

			if (direction == null || target.containsKey(direction)) {
				continue;
			}

			GiuFaceUv face = GiuFaceUv.of(quad);

			if (face != null && !face.degenerate()) {
				target.put(direction, face);
			}
		}
	}

	@Override
	public boolean useAmbientOcclusion() {
		return this.delegate.useAmbientOcclusion();
	}

	@Override
	public Material.Baked particleMaterial() {
		return this.delegate.particleMaterial();
	}

	@Override
	public int materialFlags() {
		return this.delegate.materialFlags();
	}
}
