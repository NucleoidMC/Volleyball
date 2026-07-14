package io.github.haykam821.volleyball.entity;

import java.util.Comparator;
import java.util.stream.Stream;

import org.joml.Quaternionf;
import org.joml.Vector3f;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.projectile.hurtingprojectile.WitherSkull;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.Holder;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LevelEvent;

public abstract class HittableEntity extends LivingEntity {
	private static final Vector3f[] localVertices = {
		new Vector3f(-1, -1, -1),
		new Vector3f(1, -1, -1),
		new Vector3f(1, 1, -1),
		new Vector3f(-1, 1, -1),
		new Vector3f(-1, -1, 1),
		new Vector3f(1, -1, 1),
		new Vector3f(1, 1, 1),
		new Vector3f(-1, 1, 1)
	};

	private static final Vector3f UP = new Vector3f(0, 1, 0);

	private static final double XZ_STRENGTH = 0.7;
	private static final double Y_STRENGTH = 0.9;

	private static final double ROTATIONAL_FRICTION = 1;
	private static final double ROTATION_STRENGTH = 0.2;

	private final BallEntityConfig config;

	private final float size;
	private final float eyeHeight;

	protected double rotationX;
	protected double rotationY;
	protected double rotationZ;

	private double rotationVelocityX;
	private double rotationVelocityY;
	private double rotationVelocityZ;

	private boolean wasOnGround;

	public HittableEntity(EntityType<? extends HittableEntity> type, Level level, BallEntityConfig config) {
		super(type, level);

		this.config = config;
		this.size = config.size().sample(this.random);

		this.refreshDimensions();

		if (this.config.displayType().isPresent()) {
			float eyeHeight;
			try {
				eyeHeight = this.config.displayType().get().create(level, EntitySpawnReason.SPAWNER).getEyeHeight();
			} catch (Exception exception) {
				eyeHeight = this.getEyeHeight();
			}

			this.eyeHeight = eyeHeight;
		} else {
			this.eyeHeight = this.getEyeHeight();
		}
	}

	public float getSize() {
		return this.size;
	}

	@Override
	public EntityDimensions getDefaultDimensions(Pose pose) {
		return super.getDefaultDimensions(pose).scale(this.getSize());
	}

	@Override
	public boolean hasEffect(Holder<MobEffect> effect) {
		return effect == MobEffects.SLOW_FALLING || super.hasEffect(effect);
	}

	@Override
	public void tick() {
		super.tick();

		if (!this.onGround()) {
			this.rotationX += this.rotationVelocityX;
			this.rotationY += this.rotationVelocityY;
			this.rotationZ += this.rotationVelocityZ;

			this.rotationVelocityX *= ROTATIONAL_FRICTION;
			this.rotationVelocityY *= ROTATIONAL_FRICTION;
			this.rotationVelocityZ *= ROTATIONAL_FRICTION;

			this.wasOnGround = false;
		} else if (!this.wasOnGround) {
			this.alignToGround();
			this.wasOnGround = true;
		}
	}

	private void alignToGround() {
		Quaternionf rotation = new Quaternionf().rotateXYZ((float) this.rotationX, (float) this.rotationY, (float) this.rotationZ);

		Vector3f[] vertices = Stream.of(localVertices)
			.map(vertex -> vertex.rotate(rotation, new Vector3f()))
			.sorted(Comparator.comparing(Vector3f::y).thenComparing(Vector3f::x).thenComparing(Vector3f::z).reversed())
			.limit(3)
			.toArray(Vector3f[]::new);

		Vector3f corner1 = vertices[1].sub(vertices[0], new Vector3f());
		Vector3f corner2 = vertices[2].sub(vertices[0], new Vector3f());

		Vector3f normal = corner1
			.cross(corner2, new Vector3f())
			.normalize();

		Vector3f alignedRotation = new Quaternionf()
			.rotationTo(normal, UP)
			.mul(rotation)
			.normalize()
			.getEulerAnglesXYZ(new Vector3f());

		this.rotationX = alignedRotation.x();
		this.rotationY = alignedRotation.y();
		this.rotationZ = alignedRotation.z();

		this.rotationVelocityX = 0;
		this.rotationVelocityY = 0;
		this.rotationVelocityZ = 0;
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
			return super.hurtServer(level, source, amount);
		}

		if (source.getEntity() != null && source.getEntity() != this) {
			Vec3 hitVelocity = HittableEntity.getHitVelocity(source.getEntity());
			this.setDeltaMovement(hitVelocity);

			this.rotationVelocityX = hitVelocity.y() * ROTATION_STRENGTH;
			this.rotationVelocityY = hitVelocity.z() * ROTATION_STRENGTH;
			this.rotationVelocityZ = hitVelocity.x() * ROTATION_STRENGTH;

			if (this.config.shootsSkulls()) {
				this.shootSkullAt(source.getEntity());
			}
		}

		return true;
	}

	private void shootSkullAt(Entity target) {
		Vec3 skullPos = this.getSkullPos();
		Vec3 direction = target.position().subtract(skullPos).normalize();
		
		WitherSkull skull = new WitherSkull(this.level(), this, direction);
		skull.setPosRaw(skullPos.x(), skullPos.y(), skullPos.z());

		if (this.level().getRandom().nextInt(1000) == 0) {
			skull.setDangerous(true);
		}

		this.level().addFreshEntity(skull);
		this.level().levelEvent(null, LevelEvent.SOUND_WITHER_BOSS_SHOOT, BlockPos.containing(skullPos), 0);
	}

	protected Vec3 getSkullPos() {
		return new Vec3(this.getX(), this.getY() + this.eyeHeight * this.size, this.getZ());
	}

	@Override
	protected void dropAllDeathLoot(ServerLevel level, DamageSource source) {
		return;
	}

	@Override
	public void setItemSlot(EquipmentSlot slot, ItemStack stack) {
		return;
	}

	@Override
	public ItemStack getItemBySlot(EquipmentSlot slot) {
		return ItemStack.EMPTY;
	}

	@Override
	public HumanoidArm getMainArm() {
		return HumanoidArm.RIGHT;
	}

	private static Vec3 getHitVelocity(Entity attacker) {
		double yaw = Math.toRadians(attacker.getYRot() + 90);
		double pitch = Math.toRadians(attacker.getXRot() + 90);

		double spikeMultiplier = HittableEntity.isSpiking(attacker) ? 1.2 : 1;

		double x = Math.sin(pitch) * Math.cos(yaw) * XZ_STRENGTH * spikeMultiplier;
		double y = Math.cos(pitch) * Y_STRENGTH;
		double z = Math.sin(pitch) * Math.sin(yaw) * XZ_STRENGTH * spikeMultiplier;

		return new Vec3(x, y, z);
	}

	private static boolean isSpiking(Entity entity) {
		return entity.isSprinting() && !entity.onGround();
	}
}
