package io.github.haykam821.volleyball.entity;

import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.util.valueproviders.FloatProviders;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.valueproviders.ConstantFloat;
import net.minecraft.util.valueproviders.FloatProvider;
import net.minecraft.util.RandomSource;

public record BallEntityConfig(
        Optional<EntityType<?>> displayType,
        FloatProvider size,
        boolean shootsSkulls
) {
	public static final BallEntityConfig DEFAULT = new BallEntityConfig(Optional.empty(), ConstantFloat.of(1), false);

	public static final Codec<BallEntityConfig> CODEC = RecordCodecBuilder.create(instance -> {
		return instance.group(
			BuiltInRegistries.ENTITY_TYPE.byNameCodec().optionalFieldOf("display_type").forGetter(BallEntityConfig::displayType),
			FloatProviders.codec(Float.MIN_VALUE, Float.MAX_VALUE).optionalFieldOf("size", DEFAULT.size).forGetter(BallEntityConfig::size),
			Codec.BOOL.optionalFieldOf("shoots_skulls", DEFAULT.shootsSkulls).forGetter(BallEntityConfig::shootsSkulls)
		).apply(instance, BallEntityConfig::new);
	});

	public Entity createEntity(ServerLevel world, RandomSource random) {
		return new BallEntity(VolleyballEntityTypes.BALL, world, this);
	}
}
