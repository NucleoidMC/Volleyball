package io.github.haykam821.volleyball.game.ball;

import java.util.Objects;

import eu.pb4.polymer.virtualentity.api.attachment.HolderAttachment;
import io.github.haykam821.volleyball.game.phase.VolleyballActivePhase;
import io.github.haykam821.volleyball.game.player.PlayerEntry;
import io.github.haykam821.volleyball.game.player.team.TeamEntry;
import net.minecraft.world.entity.Entity;
import net.minecraft.server.level.ServerLevel;

public abstract class BallState {
	protected final VolleyballActivePhase phase;

	public BallState(VolleyballActivePhase phase) {
		this.phase = Objects.requireNonNull(phase);
	}

	public abstract void onTick();

	public boolean onAttackEntity(PlayerEntry entry, Entity entity) {
		return false;
	}

	public void destroy(BallState newState) {
		return;
	}

	public HolderAttachment createMarker(TeamEntry team, ServerLevel world) {
		return null;
	}
}
