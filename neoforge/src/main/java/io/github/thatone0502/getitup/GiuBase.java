package io.github.thatone0502.getitup;

import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import org.jspecify.annotations.Nullable;

/**
 * {@code giu_base} 的取值：原方块「底面 / 底座」朝外的那个方向。
 *
 * <p>{@link #NONE} 是原版形态，必须是第一个值（属性默认值取首项）；四个水平值表示底座贴着那一面。
 * 只有水平方向：点天花板/地板时底面也要挪到某个侧面上，否则模组就没有意义。
 */
public enum GiuBase implements StringRepresentable {
	NONE("none", null),
	NORTH("north", Direction.NORTH),
	SOUTH("south", Direction.SOUTH),
	EAST("east", Direction.EAST),
	WEST("west", Direction.WEST);

	private final String name;
	private final @Nullable Direction direction;

	GiuBase(String name, @Nullable Direction direction) {
		this.name = name;
		this.direction = direction;
	}

	@Override
	public String getSerializedName() {
		return this.name;
	}

	/** 原版形态（没有注入旋转）。 */
	public boolean isNone() {
		return this.direction == null;
	}

	/** 被转过 90° 了：底座贴在某个侧面上，而不是地面。 */
	public boolean isRotated() {
		return this.direction != null;
	}

	public @Nullable Direction direction() {
		return this.direction;
	}

	/** 只对 isRotated() 的值有意义。 */
	public Direction directionOrThrow() {
		if (this.direction == null) {
			throw new IllegalStateException("giu_base=none has no direction");
		}

		return this.direction;
	}

	public static @Nullable GiuBase byDirection(Direction direction) {
		for (GiuBase base : values()) {
			if (base.direction == direction) {
				return base;
			}
		}

		return null;
	}
}
