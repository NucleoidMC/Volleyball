package io.github.haykam821.volleyball.entity;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.function.Consumer;

import eu.pb4.polymer.virtualentity.api.data.EntityData;
import net.fabricmc.fabric.api.networking.v1.context.PacketContext;
import net.minecraft.network.protocol.game.ServerboundAttackPacket;
import net.minecraft.world.entity.EntityTypes;
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
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.network.protocol.game.ClientboundSetPassengersPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.Level;

public class BallEntity extends HittableEntity implements PolymerEntity {
	private static final String BALL_TEXTURE = encodeTexture("9efb0547d31598b9359a66dc24d5d9d9771c37cd1492a90c9d6eaa2bac36b0e9");
	private static final ItemStack BALL_STACK = PolymerUtils.createPlayerHead(BALL_TEXTURE);

	private final ElementHolder holder;
	private final EntityAttachment attachment;

	private final GenericEntityElement visualElement;
	private final InteractionElement interaction;
	private final MobAnchorElement rideAnchor = new MobAnchorElement();

	public BallEntity(EntityType<? extends BallEntity> type, Level level, BallEntityConfig config) {
		super(type, level, config);

		this.holder = new ElementHolder() {
			@Override
			protected void notifyElementsOfPositionUpdate(Vec3 newPos, Vec3 delta) {
				BallEntity.this.rideAnchor.notifyMove(this.currentPos, newPos, delta);
			}

			@Override
			public Vec3 getPos() {
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

			this.interaction.setWidth(this.getBbWidth());
			this.interaction.setHeight(this.getBbHeight());

			this.holder.addElement(this.interaction);
			this.interaction.ignorePositionUpdates();

			VirtualEntityUtils.addVirtualPassenger(this, this.interaction.getEntityId());
		} else {
			this.interaction = null;
		}

		this.attachment = EntityAttachment.ofTicking(this.holder, this);
	}

	public BallEntity(EntityType<? extends BallEntity> type, Level level) {
		this(type, level, BallEntityConfig.DEFAULT);
	}

	private GenericEntityElement createVisualElement(BallEntityConfig config) {
		if (config.displayType().isPresent()) {
			return new GenericEntityElement() {
				private final InteractionHandler interactionHandler = new InteractionHandler() {
					@Override
					public void interact(ServerPlayer player, InteractionHand hand, Vec3 pos, boolean usingSecondaryAction) {
						player.connection.handleInteract(new ServerboundInteractPacket(BallEntity.this.getId(), hand, pos, usingSecondaryAction));
					}

					@Override
					public void attack(ServerPlayer player) {
						player.connection.handleAttack(new ServerboundAttackPacket(BallEntity.this.getId()));
					}
				};

				@Override
				protected EntityType<? extends Entity> getEntityType() {
					return config.displayType().get();
				}

				@Override
				public InteractionHandler getInteractionHandler(ServerPlayer player) {
					return this.interactionHandler;
				}
			};
		}

		ItemDisplayElement element = new ItemDisplayElement(BALL_STACK);

		element.setTransformation(this.getTransformation());
		element.setShadowRadius(0.25f);

		element.setInterpolationDuration(1);
		element.setDisplaySize(this.getBbWidth(), this.getBbHeight());

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
	public EntityType<?> getPolymerEntityType(PacketContext context) {
		return EntityTypes.ARMOR_STAND;
	}

	@Override
	public void onEntityPacketSent(Consumer<Packet<?>> consumer, Packet<?> packet) {
		if (packet instanceof ClientboundSetPassengersPacket passengersSetPacket) {
			IntList passengers = IntList.of(passengersSetPacket.getPassengers());
			packet = VirtualEntityUtils.createClientboundSetPassengersPacket(this.rideAnchor.getEntityId(), passengers);
		}

		consumer.accept(packet);
	}

	@Override
	public void modifyRawTrackedData(List<SynchedEntityData.DataValue<?>> data, ServerPlayer player, boolean initial) {
		data.add(SynchedEntityData.DataValue.create(EntityData.FLAGS, (byte) (1 << EntityData.INVISIBLE_FLAG_INDEX)));
		data.add(SynchedEntityData.DataValue.create(ArmorStand.DATA_CLIENT_FLAGS, (byte) (ArmorStand.CLIENT_FLAG_SMALL | ArmorStand.CLIENT_FLAG_MARKER)));
	}

	public static String encodeTexture(String hash) {
		String json = "{\"textures\":{\"SKIN\":{\"url\":\"https://textures.minecraft.net/texture/" + hash + "\"}}}";
		byte[] bytes = Base64.getEncoder().encode(json.getBytes(StandardCharsets.UTF_8));

		return new String(bytes, StandardCharsets.UTF_8);
	}
}
