package io.github.haykam821.volleyball.entity;

import java.util.Random;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.SlimeEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class VolleyballSlimeEntity extends SlimeEntity {
	private static final double XZ_STRENGTH = 0.7;
	private static final double Y_STRENGTH = 0.9;

	public VolleyballSlimeEntity(World world) {
		super(EntityType.SLIME, world);
	}

	@Override
	protected void initGoals() {
		return;
	}

	@Override
	public boolean damage(DamageSource source, float amount) {
		if (source.getAttacker() != null) {
			this.setVelocity(VolleyballSlimeEntity.getHitVelocity(source.getAttacker()));
		}

		return true;
	}

	@Override
	protected void drop(DamageSource source) {
		return;
	}

	private static Vec3d getHitVelocity(Entity attacker) {
		double yaw = Math.toRadians(attacker.getYaw() + 90);
		double pitch = Math.toRadians(attacker.getPitch() + 90);

		double spikeMultiplier = VolleyballSlimeEntity.isSpiking(attacker) ? 1.2 : 1;

		double x = Math.sin(pitch) * Math.cos(yaw) * XZ_STRENGTH * spikeMultiplier;
		double y = Math.cos(pitch) * Y_STRENGTH;
		double z = Math.sin(pitch) * Math.sin(yaw) * XZ_STRENGTH * spikeMultiplier;

		return new Vec3d(x, y, z);
	}

	private static boolean isSpiking(Entity entity) {
		return entity.isSprinting() && !entity.isOnGround();
	}

	public static VolleyballSlimeEntity createBall(ServerWorld world, BallEntityConfig config, Random random) {
		VolleyballSlimeEntity ball;
		
		if (config.displayType().isPresent()) {
			EntityType<?> displayType = config.displayType().get();
			ball = new PolymerVolleyballSlimeEntity(world, displayType);
		} else {
			ball = new VolleyballSlimeEntity(world);
		}

		ball.setSize(config.size().get(random), true);

		ball.setPersistent();
		ball.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOW_FALLING, Integer.MAX_VALUE, 1, true, false));

		return ball;
	}
}
