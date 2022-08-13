package io.github.haykam821.volleyball.entity;

import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.mob.SlimeEntity;
import net.minecraft.util.math.intprovider.ConstantIntProvider;
import net.minecraft.util.math.intprovider.IntProvider;
import net.minecraft.util.registry.Registry;

public record BallEntityConfig(
	Optional<EntityType<?>> displayType,
	IntProvider size,
	boolean shootsSkulls
) {
	public static final BallEntityConfig DEFAULT = new BallEntityConfig(Optional.empty(), ConstantIntProvider.create(1), false);

	public static final Codec<BallEntityConfig> CODEC = RecordCodecBuilder.create(instance -> {
		return instance.group(
			Registry.ENTITY_TYPE.getCodec().optionalFieldOf("display_type").forGetter(BallEntityConfig::displayType),
			IntProvider.createValidatingCodec(SlimeEntity.MIN_SIZE, SlimeEntity.MAX_SIZE).optionalFieldOf("size", DEFAULT.size).forGetter(BallEntityConfig::size),
			Codec.BOOL.optionalFieldOf("shoots_skulls", DEFAULT.shootsSkulls).forGetter(BallEntityConfig::shootsSkulls)
		).apply(instance, BallEntityConfig::new);
	});
}
