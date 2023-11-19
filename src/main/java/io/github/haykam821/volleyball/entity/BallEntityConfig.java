package io.github.haykam821.volleyball.entity;

import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.registry.Registries;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.floatprovider.ConstantFloatProvider;
import net.minecraft.util.math.floatprovider.FloatProvider;
import net.minecraft.util.math.random.Random;

public record BallEntityConfig(
	Optional<EntityType<?>> displayType,
	FloatProvider size,
	boolean shootsSkulls
) {
	public static final BallEntityConfig DEFAULT = new BallEntityConfig(Optional.empty(), ConstantFloatProvider.create(1), false);

	public static final Codec<BallEntityConfig> CODEC = RecordCodecBuilder.create(instance -> {
		return instance.group(
			Registries.ENTITY_TYPE.getCodec().optionalFieldOf("display_type").forGetter(BallEntityConfig::displayType),
			FloatProvider.createValidatedCodec(Float.MIN_VALUE, Float.MAX_VALUE).optionalFieldOf("size", DEFAULT.size).forGetter(BallEntityConfig::size),
			Codec.BOOL.optionalFieldOf("shoots_skulls", DEFAULT.shootsSkulls).forGetter(BallEntityConfig::shootsSkulls)
		).apply(instance, BallEntityConfig::new);
	});

	public Entity createEntity(ServerWorld world, Random random) {
		return new BallEntity(VolleyballEntityTypes.BALL, world, this);
	}
}
