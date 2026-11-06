package io.github.thatone0502.getitup.client.model;

import com.mojang.math.OctahedralGroup;
import io.github.thatone0502.getitup.GetItUp;
import io.github.thatone0502.getitup.Giu;
import io.github.thatone0502.getitup.GiuBase;
import io.github.thatone0502.getitup.client.GiuRenderConfig;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * 模型侧的总入口：烘焙完成后把所有 {@code giu_base != none} 的状态换成转过的那份。
 * <p>
 * 不碰 blockstate JSON、不注册自定义 loader —— 资源包与其它模组的模型照样能被转，
 * 这里只是在最后一步给「状态 → 模型」这张表的值包一层。
 */
public final class GiuModels {
	private static final AtomicBoolean LOGGED_WRAP = new AtomicBoolean();
	private static final AtomicBoolean CHECKED_ONCE = new AtomicBoolean();
	/** 顶点是浮点算出来的，容差只需要盖住 float 误差。 */
	private static final double TOLERANCE = 1.0E-3;

	private GiuModels() {
	}

	/**
	 * 包一层。没有需要转的状态时原样返回同一个 map（不复制）。
	 */
	public static Map<BlockState, BlockStateModel> wrap(Map<BlockState, BlockStateModel> original) {
		Map<BlockState, BlockStateModel> wrapped = null;
		int count = 0;

		for (Map.Entry<BlockState, BlockStateModel> entry : original.entrySet()) {
			BlockStateModel model = entry.getValue();

			if (model instanceof GiuBlockStateModel || !Giu.hasProperty(entry.getKey())) {
				continue;
			}

			GiuBase base = entry.getKey().getValue(Giu.GIU_BASE);

			if (base.isNone()) {
				continue;
			}

			if (wrapped == null) {
				wrapped = new HashMap<>(original);
			}

			wrapped.put(entry.getKey(), new GiuBlockStateModel(model, entry.getKey(), Giu.group(base),
				GiuRenderConfig.planFor()));
			count++;
		}

		if (LOGGED_WRAP.compareAndSet(false, true)) {
			GetItUp.LOGGER.info("[giu] rotated block models for {} states", count);
		}

		return wrapped == null ? original : wrapped;
	}

	/**
	 * 把 {@code [start, parts.size())} 这一段刚塞进来的模型片就地换成转过的那份。
	 * <p>
	 * 渲染主路径走这里：{@code BlockStateModelWrapper#update} 让原模型塞完模型片，紧接着原位替换。
	 * 已经是 {@link GiuModelPart} 的不再包一层，两条路径同时改到同一个列表也不会转两次。
	 */
	public static void rotateInPlace(BlockState state, List<BlockStateModelPart> parts, int start) {
		if (!Giu.hasProperty(state)) {
			return;
		}

		GiuBase base = state.getValue(Giu.GIU_BASE);

		if (base.isNone()) {
			return;
		}

		OctahedralGroup rotation = Giu.group(base);
		GiuTexturePlan plan = GiuRenderConfig.planFor();

		if (plan != null) {
			plan = plan.forState(state);
		}

		for (int i = start; i < parts.size(); i++) {
			BlockStateModelPart part = parts.get(i);

			if (!(part instanceof GiuModelPart)) {
				parts.set(i, new GiuModelPart(part, rotation, plan));
			}
		}

		checkOnce(state, parts);
	}

	/**
	 * 一次性自检：把「转过之后的模型顶点包围盒」和「转过之后的碰撞形状」对一下。
	 * 两者走的是同一条旋转定义，对不上就说明顶点旋转的方向/绕点错了 —— 这类错误看截图不一定看得出来，
	 * 所以让它自己说出来。
	 */
	static void checkOnce(BlockState state, List<BlockStateModelPart> parts) {
		if (!CHECKED_ONCE.compareAndSet(false, true)) {
			return;
		}

		AABB model = modelBounds(parts);

		if (model == null) {
			return;
		}

		AABB shape = state.getShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO).bounds();
		boolean same = Math.abs(model.minX - shape.minX) <= TOLERANCE
			&& Math.abs(model.minY - shape.minY) <= TOLERANCE
			&& Math.abs(model.minZ - shape.minZ) <= TOLERANCE
			&& Math.abs(model.maxX - shape.maxX) <= TOLERANCE
			&& Math.abs(model.maxY - shape.maxY) <= TOLERANCE
			&& Math.abs(model.maxZ - shape.maxZ) <= TOLERANCE;

		if (same) {
			GetItUp.LOGGER.info("[giu] model matches shape on first check, state = {}", state);
		} else {
			GetItUp.LOGGER.warn("[giu] MODEL/SHAPE MISMATCH, state = {} model = {} shape = {}", state, model, shape);
		}
	}

	private static AABB modelBounds(List<BlockStateModelPart> parts) {
		double[] box = null;

		for (BlockStateModelPart part : parts) {
			box = accumulate(box, part.getQuads(null));

			for (Direction direction : Direction.values()) {
				box = accumulate(box, part.getQuads(direction));
			}
		}

		return box == null ? null : new AABB(box[0], box[1], box[2], box[3], box[4], box[5]);
	}

	private static double[] accumulate(double[] box, List<net.minecraft.client.resources.model.geometry.BakedQuad> quads) {
		for (net.minecraft.client.resources.model.geometry.BakedQuad quad : quads) {
			for (int vertex = 0; vertex < 4; vertex++) {
				var position = quad.position(vertex);

				if (box == null) {
					box = new double[] {position.x(), position.y(), position.z(), position.x(), position.y(), position.z()};
					continue;
				}

				box[0] = Math.min(box[0], position.x());
				box[1] = Math.min(box[1], position.y());
				box[2] = Math.min(box[2], position.z());
				box[3] = Math.max(box[3], position.x());
				box[4] = Math.max(box[4], position.y());
				box[5] = Math.max(box[5], position.z());
			}
		}

		return box;
	}
}
