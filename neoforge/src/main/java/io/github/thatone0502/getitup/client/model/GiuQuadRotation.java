package io.github.thatone0502.getitup.client.model;

import com.mojang.math.OctahedralGroup;
import java.util.List;
import java.util.Map;
import net.minecraft.client.model.geom.builders.UVPair;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.Direction;
import net.neoforged.neoforge.client.model.quad.BakedNormals;
import org.joml.Matrix3fc;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

/**
 * 把一个 {@link BakedQuad} 绕方块中心做 90° 旋转。
 *
 * <p>旋转矩阵取自 {@link OctahedralGroup#transformation()}，与 {@code Shapes.rotate(shape, group)} 同一份，
 * 所以模型和碰撞箱不会各转各的。贴图默认跟着转（图案会躺倒），传入 {@code faces} 后改成「贴图钉在世界上」。
 */
public final class GiuQuadRotation {
	private static final float CENTER = 0.5F;
	private static final float EPSILON = 1.0E-5F;

	private GiuQuadRotation() {
	}

	/** 贴图原样跟着转（旧行为）。 */
	public static List<BakedQuad> rotateAll(List<BakedQuad> quads, OctahedralGroup rotation) {
		return rotateAll(quads, rotation, null, null);
	}

	/**
	 * @param faces 原模型里「法线为某个世界方向」的那张贴图与该面用到的矩形；{@code null} 表示不做重贴图
	 * @param plan  {@code null} 时按世界坐标裁切到新面上；否则每张面各自问它要不要改成缩放铺满
	 */
	public static List<BakedQuad> rotateAll(List<BakedQuad> quads, OctahedralGroup rotation, @Nullable Map<Direction, GiuFaceUv> faces, @Nullable GiuTexturePlan plan) {
		if (quads.isEmpty()) {
			return List.of();
		}

		BakedQuad[] rotated = new BakedQuad[quads.size()];

		for (int i = 0; i < quads.size(); i++) {
			rotated[i] = rotate(quads.get(i), rotation, faces, plan);
		}

		return List.of(rotated);
	}

	public static BakedQuad rotate(BakedQuad quad, OctahedralGroup rotation) {
		return rotate(quad, rotation, null, null);
	}

	/**
	 * 旋转一张面，并可选地把贴图改成「按世界方向重新附着」。
	 *
	 * <p>原版把 UV 钉在世界上（{@code u}/{@code v} 只由法线与顶点坐标决定），旋转 90° 后法线变了、
	 * 顶点仍是方块内 0..1，于是图案跟着躺倒。这里换用「原模型里法线等于新法线」的那张面的贴图，
	 * 再用旋转后的顶点重算一遍 UV，图案方向与明暗关系就与未旋转时一致。
	 */
	public static BakedQuad rotate(BakedQuad quad, OctahedralGroup rotation, @Nullable Map<Direction, GiuFaceUv> faces, @Nullable GiuTexturePlan plan) {
		Matrix3fc matrix = rotation.transformation();

		Vector3f[] positions = {
			rotatePosition(matrix, quad.position0()),
			rotatePosition(matrix, quad.position1()),
			rotatePosition(matrix, quad.position2()),
			rotatePosition(matrix, quad.position3()),
		};

		Direction direction = quad.direction() == null ? null : rotation.rotate(quad.direction());
		GiuFaceUv face = faces == null || direction == null ? null : faces.get(direction);

		if (face != null && plan != null) {
			face = plan.resolve(face, faces);
		}

		long[] uvs = {
			quad.packedUV0(),
			quad.packedUV1(),
			quad.packedUV2(),
			quad.packedUV3(),
		};

		BakedQuad.MaterialInfo material = quad.materialInfo();

		if (face != null) {
			boolean refit = plan != null && plan.refitFor(face);

			float minU = Float.MAX_VALUE;
			float minV = Float.MAX_VALUE;
			float maxU = -Float.MAX_VALUE;
			float maxV = -Float.MAX_VALUE;

			for (Vector3f position : positions) {
				float u = uAt(direction, position);
				float v = vAt(direction, position);
				minU = Math.min(minU, u);
				minV = Math.min(minV, v);
				maxU = Math.max(maxU, u);
				maxV = Math.max(maxV, v);
			}

			for (int vertex = 0; vertex < positions.length; vertex++) {
				float u = uAt(direction, positions[vertex]);
				float v = vAt(direction, positions[vertex]);

				if (refit) {
					// 把「这块面自己的 U/V 跨度」映射到「原版面用到的那块矩形」上 —— 整块面板被压缩铺满。
					u = lerp(face.minU(), face.maxU(), normalise(u, minU, maxU));
					v = lerp(face.minV(), face.maxV(), normalise(v, minV, maxV));
				}

				uvs[vertex] = UVPair.pack(face.sprite().getU(u), face.sprite().getV(v));
			}

			if (material.sprite() != face.sprite()) {
				material = new BakedQuad.MaterialInfo(face.sprite(), material.layer(), material.itemRenderType(),
					material.tintIndex(), material.shade(), material.lightEmission(), material.ambientOcclusion());
			}
		}

		return new BakedQuad(
			positions[0],
			positions[1],
			positions[2],
			positions[3],
			uvs[0],
			uvs[1],
			uvs[2],
			uvs[3],
			direction,
			material,
			rotateNormals(quad.bakedNormals(), matrix),
			quad.bakedColors()
		);
	}

	/** 位置：先平移到以方块中心为原点，转完再移回来。 */
	private static Vector3f rotatePosition(Matrix3fc matrix, Vector3fc position) {
		Vector3f local = new Vector3f(position).sub(CENTER, CENTER, CENTER);
		matrix.transform(local, local);

		return local.add(CENTER, CENTER, CENTER);
	}

	/** 法线只转向、不平移；未指定的法线原样保留。 */
	private static BakedNormals rotateNormals(BakedNormals normals, Matrix3fc matrix) {
		if (normals == null) {
			return null;
		}

		int[] rotated = new int[BakedQuad.VERTEX_COUNT];
		Vector3f scratch = new Vector3f();

		for (int vertex = 0; vertex < rotated.length; vertex++) {
			int packed = normals.normal(vertex);

			if (BakedNormals.isUnspecified(packed)) {
				rotated[vertex] = packed;
				continue;
			}

			BakedNormals.unpack(packed, scratch);
			matrix.transform(scratch, scratch);
			rotated[vertex] = BakedNormals.pack(scratch);
		}

		return BakedNormals.of(rotated[0], rotated[1], rotated[2], rotated[3]);
	}

	/**
	 * 顶点在「世界方向 {@code normal} 的那张面」上的 U 参数（0..1），逐字对应原版
	 * {@code FaceBakery.defaultFaceUV} 的 {@code 16x / 16-16x / 16z / 16-16z}。
	 */
	private static float uAt(Direction normal, Vector3fc position) {
		return switch (normal) {
			case UP, DOWN, SOUTH -> clamp01(position.x());
			case NORTH -> clamp01(1.0F - position.x());
			case WEST -> clamp01(position.z());
			case EAST -> clamp01(1.0F - position.z());
		};
	}

	/** V 参数（0..1），同样对应 {@code 16z / 16-16z / 16-16y}。 */
	private static float vAt(Direction normal, Vector3fc position) {
		return switch (normal) {
			case UP -> clamp01(position.z());
			case DOWN -> clamp01(1.0F - position.z());
			default -> clamp01(1.0F - position.y());
		};
	}

	private static float normalise(float value, float min, float max) {
		float span = max - min;

		return Math.abs(span) < EPSILON ? 0.0F : (value - min) / span;
	}

	private static float lerp(float from, float to, float t) {
		return from + (to - from) * t;
	}

	private static float clamp01(float value) {
		return Math.max(0.0F, Math.min(1.0F, value));
	}
}
