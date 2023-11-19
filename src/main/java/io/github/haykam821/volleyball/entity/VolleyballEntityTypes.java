package io.github.haykam821.volleyball.entity;

import eu.pb4.polymer.core.api.entity.PolymerEntityUtils;
import io.github.haykam821.volleyball.Volleyball;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public final class VolleyballEntityTypes {
	private static final Identifier BALL_ID = new Identifier(Volleyball.MOD_ID, "ball");
	public static final EntityType<BallEntity> BALL = FabricEntityTypeBuilder.<BallEntity>create(SpawnGroup.MISC, BallEntity::new)
		.dimensions(EntityDimensions.changing(8 / 16f, 8 / 16f))
		.build();

	private VolleyballEntityTypes() {
		return;
	}

	public static void register() {
		Registry.register(Registries.ENTITY_TYPE, BALL_ID, BALL);

		PolymerEntityUtils.registerType(BALL);
		FabricDefaultAttributeRegistry.register(BALL, LivingEntity.createLivingAttributes());
	}
}
