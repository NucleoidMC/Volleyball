package io.github.haykam821.volleyball.game.ball;

import java.util.Objects;

import org.joml.Matrix4f;

import eu.pb4.polymer.virtualentity.api.ElementHolder;
import eu.pb4.polymer.virtualentity.api.attachment.ChunkAttachment;
import eu.pb4.polymer.virtualentity.api.attachment.HolderAttachment;
import eu.pb4.polymer.virtualentity.api.elements.TextDisplayElement;
import io.github.haykam821.volleyball.game.phase.VolleyballActivePhase;
import io.github.haykam821.volleyball.game.player.PlayerEntry;
import io.github.haykam821.volleyball.game.player.team.TeamEntry;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Display.BillboardConstraints;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.util.Mth;

public abstract class EntityBallState extends BallState {
	private static final Component MARKER_TEXT = Component.literal("❌").withStyle(ChatFormatting.BOLD);

	private static final Matrix4f MARKER_TRANSFORMATION = new Matrix4f()
		.translate(-1 / 32f, 0, 21 / 64f)
		.rotateX(Mth.PI / -2)
		.scale(2);

	protected final Entity ball;

	public EntityBallState(VolleyballActivePhase phase, Entity ball) {
		super(phase);

		this.ball = Objects.requireNonNull(ball);
	}

	/**
	 * @return whether the ball tick was handled by shared logic
	 */
	protected final boolean onEntityTick() {
		if (!this.ball.isAlive()) {
			this.phase.resetBall(null);
			return true;
		}

		for (TeamEntry team : this.phase.getTeams()) {
			if (team.isBallOnCourt(this.ball)) {
				team.incrementOtherTeamScore();
				return true;
			}
		}

		return false;
	}

	@Override
	public final boolean onAttackEntity(PlayerEntry entry, Entity entity) {
		if (entity == this.ball) {
			this.onHitBall(entry);
			return true;
		}

		return false;
	}

	protected abstract void onHitBall(PlayerEntry entry);

	@Override
	public final void destroy(BallState newState) {
		if (!(newState instanceof EntityBallState)) {
			this.ball.discard();
		}
	}

	@Override
	public HolderAttachment createMarker(TeamEntry team, ServerLevel level) {
		Component text = MARKER_TEXT.copy().withStyle(team.getFormatting());
		TextDisplayElement element = new TextDisplayElement(text);

		element.setShadow(true);
		element.setBackground(0);

		element.setInvisible(true);

		element.setTransformation(MARKER_TRANSFORMATION);
		element.setBillboardMode(BillboardConstraints.VERTICAL);

		ElementHolder holder = new ElementHolder();
		holder.addElement(element);

		return ChunkAttachment.of(holder, level, this.ball.position());
	}
}
