package io.github.haykam821.volleyball.entity;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.function.Consumer;

import org.joml.Matrix4f;

import eu.pb4.polymer.core.api.entity.PolymerEntity;
import eu.pb4.polymer.core.api.utils.PolymerUtils;
import eu.pb4.polymer.virtualentity.api.ElementHolder;
import eu.pb4.polymer.virtualentity.api.VirtualEntityUtils;
import eu.pb4.polymer.virtualentity.api.attachment.EntityAttachment;
import eu.pb4.polymer.virtualentity.api.elements.DisplayElement;
import eu.pb4.polymer.virtualentity.api.elements.GenericEntityElement;
import eu.pb4.polymer.virtualentity.api.elements.InteractionElement;
import eu.pb4.polymer.virtualentity.api.elements.ItemDisplayElement;
import eu.pb4.polymer.virtualentity.api.elements.MobAnchorElement;
import eu.pb4.polymer.virtualentity.api.tracker.EntityTrackedData;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.s2c.play.EntityPassengersSetS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class BallEntity extends HittableEntity implements PolymerEntity {
	private static final String BALL_TEXTURE = encodeTexture("9efb0547d31598b9359a66dc24d5d9d9771c37cd1492a90c9d6eaa2bac36b0e9");
	private static final ItemStack BALL_STACK = PolymerUtils.createPlayerHead(BALL_TEXTURE);

	private final ElementHolder holder;
	private final EntityAttachment attachment;

	private final GenericEntityElement visualElement;
	private final InteractionElement interaction;
	private final MobAnchorElement rideAnchor = new MobAnchorElement();

	public BallEntity(EntityType<? extends BallEntity> type, World world, BallEntityConfig config) {
		super(type, world, config);

		this.holder = new ElementHolder() {
			@Override
			protected void notifyElementsOfPositionUpdate(Vec3d newPos, Vec3d delta) {
				BallEntity.this.rideAnchor.notifyMove(this.currentPos, newPos, delta);
			}

			@Override
			public Vec3d getPos() {
				return this.getAttachment().getPos();
			}
		};

		this.visualElement = this.createVisualElement(config);
		this.visualElement.ignorePositionUpdates();

		this.holder.addElement(this.visualElement);
		this.holder.addElement(this.rideAnchor);

		for (int id : this.visualElement.getEntityIds()) {
			VirtualEntityUtils.addVirtualPassenger(this, id);
		}

		if (this.visualElement instanceof DisplayElement) {
			this.interaction = InteractionElement.redirect(this);

			this.interaction.setWidth(this.getWidth());
			this.interaction.setHeight(this.getHeight());

			this.holder.addElement(this.interaction);

			VirtualEntityUtils.addVirtualPassenger(this, this.interaction.getEntityId());
		} else {
			this.interaction = null;
		}

		this.attachment = EntityAttachment.ofTicking(this.holder, this);
	}

	public BallEntity(EntityType<? extends BallEntity> type, World world) {
		this(type, world, BallEntityConfig.DEFAULT);
	}

	private GenericEntityElement createVisualElement(BallEntityConfig config) {
		if (config.displayType().isPresent()) {
			return new GenericEntityElement() {
				private final InteractionHandler interactionHandler = new InteractionHandler() {
					@Override
					public void interact(ServerPlayerEntity player, Hand hand) {
						player.networkHandler.onPlayerInteractEntity(PlayerInteractEntityC2SPacket.interact(BallEntity.this, player.isSneaking(), hand));
					}

					@Override
					public void interactAt(ServerPlayerEntity player, Hand hand, Vec3d pos) {
						player.networkHandler.onPlayerInteractEntity(PlayerInteractEntityC2SPacket.interactAt(BallEntity.this, player.isSneaking(), hand, pos));
					}

					@Override
					public void attack(ServerPlayerEntity player) {
						player.networkHandler.onPlayerInteractEntity(PlayerInteractEntityC2SPacket.attack(BallEntity.this, player.isSneaking()));
					}
				};

				@Override
				protected EntityType<? extends Entity> getEntityType() {
					return config.displayType().get();
				}

				@Override
				public InteractionHandler getInteractionHandler(ServerPlayerEntity player) {
					return this.interactionHandler;
				}
			};
		}

		ItemDisplayElement element = new ItemDisplayElement(BALL_STACK);

		element.setTransformation(this.getTransformation());
		element.setShadowRadius(0.25f);

		element.setInterpolationDuration(1);
		element.setDisplaySize(this.getWidth(), this.getHeight());

		return element;
	}

	private Matrix4f getTransformation() {
		Matrix4f transformation = new Matrix4f();

		transformation.scale(this.getSize());
		transformation.translate(0, 0.5f, 0);

		transformation.translate(0, -0.25f, 0);
		transformation.rotateXYZ((float) this.rotationX, (float) this.rotationY, (float) this.rotationZ);
		transformation.translate(0, 0.25f, 0);

		return transformation;
	}

	@Override
	public void tick() {
		super.tick();

		if (this.visualElement instanceof DisplayElement displayElement) {
			displayElement.setTransformation(this.getTransformation());

			if (displayElement.isDirty()) {
				displayElement.startInterpolation();
			}
		}
	}

	@Override
	public void remove(RemovalReason reason) {
		super.remove(reason);

		this.holder.destroy();
		this.attachment.destroy();
	}

	@Override
	public EntityType<?> getPolymerEntityType(ServerPlayerEntity player) {
		return EntityType.ARMOR_STAND;
	}

	@Override
	public void onEntityPacketSent(Consumer<Packet<?>> consumer, Packet<?> packet) {
		if (packet instanceof EntityPassengersSetS2CPacket passengersSetPacket) {
			IntList passengers = IntList.of(passengersSetPacket.getPassengerIds());
			packet = VirtualEntityUtils.createRidePacket(this.rideAnchor.getEntityId(), passengers);
		}

		consumer.accept(packet);
	}

	@Override
	public void modifyRawTrackedData(List<DataTracker.SerializedEntry<?>> data, ServerPlayerEntity player, boolean initial) {
		data.add(DataTracker.SerializedEntry.of(EntityTrackedData.FLAGS, (byte) (1 << EntityTrackedData.INVISIBLE_FLAG_INDEX)));
		data.add(DataTracker.SerializedEntry.of(ArmorStandEntity.ARMOR_STAND_FLAGS, (byte) (ArmorStandEntity.SMALL_FLAG | ArmorStandEntity.MARKER_FLAG)));
	}

	public static String encodeTexture(String hash) {
		String json = "{\"textures\":{\"SKIN\":{\"url\":\"https://textures.minecraft.net/texture/" + hash + "\"}}}";
		byte[] bytes = Base64.getEncoder().encode(json.getBytes(StandardCharsets.UTF_8));

		return new String(bytes, StandardCharsets.UTF_8);
	}
}
