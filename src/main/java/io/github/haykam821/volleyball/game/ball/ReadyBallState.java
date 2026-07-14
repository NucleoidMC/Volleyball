package io.github.haykam821.volleyball.game.ball;

import io.github.haykam821.volleyball.game.phase.VolleyballActivePhase;
import io.github.haykam821.volleyball.game.player.PlayerEntry;
import io.github.haykam821.volleyball.game.player.team.TeamEntry;
import net.minecraft.world.entity.Entity;

public class ReadyBallState extends EntityBallState {
	public ReadyBallState(VolleyballActivePhase phase, Entity ball) {
		super(phase, ball);
	}

	@Override
	public void onTick() {
		this.onEntityTick();
	}

	@Override
	public void onHitBall(PlayerEntry entry) {
		TeamEntry possessionTeam = entry == null ? null : entry.getTeam();
		this.phase.setBallState(new ActiveBallState(this.phase, this.ball, possessionTeam));
	}
}
