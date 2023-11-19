package io.github.haykam821.volleyball.entity;

import java.util.Collections;
import java.util.Comparator;
import java.util.stream.Stream;

import org.joml.Quaternionf;
import org.joml.Vector3f;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.projectile.WitherSkullEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.util.Arm;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.WorldEvents;

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

	public HittableEntity(EntityType<? extends HittableEntity> type, World world, BallEntityConfig config) {
		super(type, world);

		this.config = config;
		this.size = config.size().get(this.random);

		this.calculateDimensions();

		if (this.config.displayType().isPresent()) {
			float eyeHeight;
			try {
				eyeHeight = this.config.displayType().get().create(world).getStandingEyeHeight();
			} catch (Exception exception) {
				eyeHeight = this.getStandingEyeHeight();
			}

			this.eyeHeight = eyeHeight;
		} else {
			this.eyeHeight = this.getStandingEyeHeight();
		}
	}

	public float getSize() {
		return this.size;
	}

	@Override
	public EntityDimensions getDimensions(EntityPose pose) {
		return super.getDimensions(pose).scaled(this.getSize());
	}

	@Override
	protected float getActiveEyeHeight(EntityPose pose, EntityDimensions dimensions) {
		return super.getActiveEyeHeight(pose, dimensions) * this.getSize();
	}

	@Override
	public boolean hasStatusEffect(StatusEffect effect) {
		return effect == StatusEffects.SLOW_FALLING || super.hasStatusEffect(effect);
	}

	@Override
	public void tick() {
		super.tick();

		if (!this.isOnGround()) {
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
	public boolean damage(DamageSource source, float amount) {
		if (source.isIn(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
			return super.damage(source, amount);
		}

		if (source.getAttacker() != null && source.getAttacker() != this) {
			Vec3d hitVelocity = HittableEntity.getHitVelocity(source.getAttacker());
			this.setVelocity(hitVelocity);

			this.rotationVelocityX = hitVelocity.getY() * ROTATION_STRENGTH;
			this.rotationVelocityY = hitVelocity.getZ() * ROTATION_STRENGTH;
			this.rotationVelocityZ = hitVelocity.getX() * ROTATION_STRENGTH;

			if (this.config.shootsSkulls()) {
				this.shootSkullAt(source.getAttacker());
			}
		}

		return true;
	}

	private void shootSkullAt(Entity target) {
		Vec3d skullPos = this.getSkullPos();
		Vec3d direction = target.getPos().subtract(skullPos);
		
		WitherSkullEntity skull = new WitherSkullEntity(this.getWorld(), this, direction.getX(), direction.getY(), direction.getZ());
		skull.setPos(skullPos.getX(), skullPos.getY(), skullPos.getZ());

		if (this.getWorld().getRandom().nextInt(1000) == 0) {
			skull.setCharged(true);
		}

		this.getWorld().spawnEntity(skull);
		this.getWorld().syncWorldEvent(null, WorldEvents.WITHER_SHOOTS, BlockPos.ofFloored(skullPos), 0);
	}

	protected Vec3d getSkullPos() {
		return new Vec3d(this.getX(), this.getY() + this.eyeHeight * this.size, this.getZ());
	}

	@Override
	protected void drop(DamageSource source) {
		return;
	}

	@Override
	public void equipStack(EquipmentSlot slot, ItemStack stack) {
		return;
	}

	@Override
	public Iterable<ItemStack> getArmorItems() {
		return Collections.emptySet();
	}

	@Override
	public ItemStack getEquippedStack(EquipmentSlot slot) {
		return ItemStack.EMPTY;
	}

	@Override
	public Arm getMainArm() {
		return Arm.RIGHT;
	}

	private static Vec3d getHitVelocity(Entity attacker) {
		double yaw = Math.toRadians(attacker.getYaw() + 90);
		double pitch = Math.toRadians(attacker.getPitch() + 90);

		double spikeMultiplier = HittableEntity.isSpiking(attacker) ? 1.2 : 1;

		double x = Math.sin(pitch) * Math.cos(yaw) * XZ_STRENGTH * spikeMultiplier;
		double y = Math.cos(pitch) * Y_STRENGTH;
		double z = Math.sin(pitch) * Math.sin(yaw) * XZ_STRENGTH * spikeMultiplier;

		return new Vec3d(x, y, z);
	}

	private static boolean isSpiking(Entity entity) {
		return entity.isSprinting() && !entity.isOnGround();
	}
}
