package io.github.haykam821.volleyball.entity;

import eu.pb4.polymer.core.api.entity.PolymerEntityUtils;
import io.github.haykam821.volleyball.Volleyball;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;

public final class VolleyballEntityTypes {
	private static final Identifier BALL_ID = Volleyball.identifier("ball");
	private static final RegistryKey<EntityType<?>> BALL_KEY = RegistryKey.of(RegistryKeys.ENTITY_TYPE, BALL_ID);

	public static final EntityType<BallEntity> BALL = EntityType.Builder.<BallEntity>create(BallEntity::new, SpawnGroup.MISC)
		.dimensions(8 / 16f, 8 / 16f)
		.build(BALL_KEY);

	private VolleyballEntityTypes() {
		return;
	}

	public static void register() {
		Registry.register(Registries.ENTITY_TYPE, BALL_KEY, BALL);

		PolymerEntityUtils.registerType(BALL);
		FabricDefaultAttributeRegistry.register(BALL, LivingEntity.createLivingAttributes());
	}
}
