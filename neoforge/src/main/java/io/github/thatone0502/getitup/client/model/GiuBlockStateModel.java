package io.github.thatone0502.getitup.client.model;

import com.mojang.math.OctahedralGroup;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * 一个「转过」的方块模型：包住原版那个 {@link BlockStateModel}，其它一切照旧。
 * <p>
 * 只有收集模型片这一步不同 —— 原模型报出来的每个模型片换成 {@link GiuModelPart}。
 * 模型片按<b>身份</b>缓存：同一个原模型片永远对应同一个转过的那份，每帧只是查表，不重新旋转。
 * <p>
 * 两个收集入口都要覆盖：渲染器走的是带方块上下文的 5 参版本（NeoForge 的扩展口），
 * 其它地方（例如掉落物、物品栏发光）可能走 2 参版本。两个都转发给原模型，
 * 再统一替换 —— 这样既不丢上下文，也不会漏掉某条路径。
 */
public final class GiuBlockStateModel implements BlockStateModel {
	private final BlockStateModel delegate;
	private final BlockState state;
	private final OctahedralGroup rotation;
	private final @Nullable GiuTexturePlan plan;
	private final Map<BlockStateModelPart, GiuModelPart> parts = new IdentityHashMap<>();

	public GiuBlockStateModel(BlockStateModel delegate, BlockState state, OctahedralGroup rotation, @Nullable GiuTexturePlan plan) {
		this.delegate = delegate;
		this.state = state;
		this.rotation = rotation;
		this.plan = plan == null ? null : plan.forState(state);
	}

	@Override
	public void collectParts(RandomSource random, List<BlockStateModelPart> collector) {
		int start = collector.size();
		this.delegate.collectParts(random, collector);
		this.replace(collector, start);
	}

	@Override
	public void collectParts(BlockAndTintGetter level, BlockPos pos, BlockState blockState, RandomSource random,
			List<BlockStateModelPart> collector) {
		int start = collector.size();
		this.delegate.collectParts(level, pos, blockState, random, collector);
		this.replace(collector, start);
	}

	@Override
	public Material.Baked particleMaterial() {
		return this.delegate.particleMaterial();
	}

	@Override
	public int materialFlags() {
		return this.delegate.materialFlags();
	}

	private void replace(List<BlockStateModelPart> collector, int start) {
		for (int i = start; i < collector.size(); i++) {
			if (!(collector.get(i) instanceof GiuModelPart)) {
				collector.set(i, this.parts.computeIfAbsent(collector.get(i), part -> new GiuModelPart(part, this.rotation, this.plan)));
			}
		}

		GiuModels.checkOnce(this.state, collector);
	}
}
