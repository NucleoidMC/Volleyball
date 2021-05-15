package io.github.haykam821.volleyball.entity;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.SlimeEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;

public class VolleyballSlimeEntity extends SlimeEntity {
	public VolleyballSlimeEntity(World world) {
		super(EntityType.SLIME, world);
	}

	@Override
	protected void initGoals() {
		return;
	}

	@Override
	public boolean damage(DamageSource source, float amount) {
		boolean success = super.damage(source, amount);
		this.setHealth(this.getMaxHealth());

		return success;
	}

	public static VolleyballSlimeEntity createBall(ServerWorld world) {
		VolleyballSlimeEntity ball = new VolleyballSlimeEntity(world);

		ball.setPersistent();
		ball.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOW_FALLING, Integer.MAX_VALUE, 1, true, false));

		return ball;
	}
}
