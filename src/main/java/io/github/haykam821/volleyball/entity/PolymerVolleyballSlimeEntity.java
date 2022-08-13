package io.github.haykam821.volleyball.entity;

import eu.pb4.polymer.api.entity.PolymerEntity;
import net.minecraft.entity.EntityType;
import net.minecraft.world.World;

public class PolymerVolleyballSlimeEntity extends VolleyballSlimeEntity implements PolymerEntity {
	private final EntityType<?> displayType;

	public PolymerVolleyballSlimeEntity(World world, EntityType<?> displayType) {
		super(world);
		
		this.displayType = displayType;
	}

	@Override
	public EntityType<?> getPolymerEntityType() {
		return this.displayType;
	}
}
