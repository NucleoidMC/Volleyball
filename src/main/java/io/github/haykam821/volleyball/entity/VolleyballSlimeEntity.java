package io.github.haykam821.volleyball.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.SlimeEntity;
import net.minecraft.entity.projectile.WitherSkullEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import net.minecraft.world.WorldEvents;

public class VolleyballSlimeEntity extends SlimeEntity {
	private static final double XZ_STRENGTH = 0.7;
	private static final double Y_STRENGTH = 0.9;

	private final BallEntityConfig config;

	public VolleyballSlimeEntity(World world, BallEntityConfig config) {
		super(EntityType.SLIME, world);

		this.config = config;
	}

	@Override
	protected void initGoals() {
		return;
	}

	@Override
	public boolean damage(DamageSource source, float amount) {
		if (source.getAttacker() != null && source.getAttacker() != this) {
			this.setVelocity(VolleyballSlimeEntity.getHitVelocity(source.getAttacker()));

			if (this.config.shootsSkulls()) {
				this.shootSkullAt(source.getAttacker());
			}
		}

		return true;
	}

	private void shootSkullAt(Entity target) {
		Vec3d skullPos = this.getSkullPos();
		Vec3d direction = target.getPos().subtract(skullPos);
		
		WitherSkullEntity skull = new WitherSkullEntity(this.world, this, direction.getX(), direction.getY(), direction.getZ());
		skull.setPos(skullPos.getX(), skullPos.getY(), skullPos.getZ());

		if (this.world.getRandom().nextInt(1000) == 0) {
			skull.setCharged(true);
		}

		this.world.spawnEntity(skull);
		this.world.syncWorldEvent(null, WorldEvents.WITHER_SHOOTS, new BlockPos(skullPos), 0);
	}

	protected Vec3d getSkullPos() {
		return this.getEyePos();
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
			ball = new PolymerVolleyballSlimeEntity(world, config);
		} else {
			ball = new VolleyballSlimeEntity(world, config);
		}

		ball.setSize(config.size().get(random), true);

		ball.setPersistent();
		ball.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOW_FALLING, Integer.MAX_VALUE, 1, true, false));

		return ball;
	}
}
