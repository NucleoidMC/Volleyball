package io.github.haykam821.volleyball.entity;

import eu.pb4.polymer.core.api.entity.PolymerEntityUtils;
import io.github.haykam821.volleyball.Volleyball;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;

public final class VolleyballEntityTypes {
	private static final Identifier BALL_ID = Volleyball.identifier("ball");
	private static final ResourceKey<EntityType<?>> BALL_KEY = ResourceKey.create(Registries.ENTITY_TYPE, BALL_ID);

	public static final EntityType<BallEntity> BALL = EntityType.Builder.<BallEntity>of(BallEntity::new, MobCategory.MISC)
		.sized(8 / 16f, 8 / 16f)
		.build(BALL_KEY);

	private VolleyballEntityTypes() {
		return;
	}

	public static void register() {
		Registry.register(BuiltInRegistries.ENTITY_TYPE, BALL_KEY, BALL);

		PolymerEntityUtils.registerType(BALL);
		FabricDefaultAttributeRegistry.register(BALL, LivingEntity.createLivingAttributes());
	}
}
