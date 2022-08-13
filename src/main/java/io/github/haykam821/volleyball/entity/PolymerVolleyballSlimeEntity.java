package io.github.haykam821.volleyball.entity;

import eu.pb4.polymer.api.entity.PolymerEntity;
import net.minecraft.entity.EntityType;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class PolymerVolleyballSlimeEntity extends VolleyballSlimeEntity implements PolymerEntity {
	private final EntityType<?> displayType;
	private final float eyeHeight;

	public PolymerVolleyballSlimeEntity(World world, BallEntityConfig config) {
		super(world, config);
		
		this.displayType = config.displayType().orElseThrow();
		
		float eyeHeight;
		try {
			eyeHeight = this.displayType.create(null).getStandingEyeHeight();
		} catch (Exception exception) {
			eyeHeight = this.getStandingEyeHeight();
		}

		this.eyeHeight = eyeHeight;
	}

	@Override
	public EntityType<?> getPolymerEntityType() {
		return this.displayType;
	}

	@Override
	protected Vec3d getSkullPos() {
		return new Vec3d(this.getX(), this.getY() + this.eyeHeight, this.getZ());
	}
}
