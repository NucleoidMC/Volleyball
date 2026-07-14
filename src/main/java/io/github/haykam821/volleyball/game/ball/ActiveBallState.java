package io.github.haykam821.volleyball.game.ball;

import io.github.haykam821.volleyball.game.phase.VolleyballActivePhase;
import io.github.haykam821.volleyball.game.player.PlayerEntry;
import io.github.haykam821.volleyball.game.player.team.TeamEntry;
import net.minecraft.world.entity.Entity;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;

public class ActiveBallState extends EntityBallState {
	private static final Component INACTIVE_BALL_RESET_MESSAGE = Component.translatable("text.volleyball.inactive_ball_reset").withStyle(ChatFormatting.RED);

	/**
	 * The number of ticks since the ball was last hit.
	 */
	private int ticksSinceHit = 0;

	/**
	 * The team that last hit the ball.
	 */
	private TeamEntry possessionTeam;

	public ActiveBallState(VolleyballActivePhase phase, Entity ball, TeamEntry possessionTeam) {
		super(phase, ball);

		this.possessionTeam = possessionTeam;
	}

	@Override
	public void onTick() {
		if (this.onEntityTick()) return;

		if (this.ticksSinceHit >= this.phase.getConfig().getInactiveBallTicks()) {
			this.phase.resetBall(null);
			this.phase.getGameSpace().getPlayers().sendMessage(INACTIVE_BALL_RESET_MESSAGE);
		} else {
			this.ticksSinceHit += 1;

			if (this.possessionTeam != null && this.phase.hasBallLandedOffCourt(this.ball)) {
				this.possessionTeam.incrementOtherTeamScore();
			}
		}
	}

	public void onHitBall(PlayerEntry entry) {
		this.ticksSinceHit = 0;

		if (entry != null) {
			this.possessionTeam = entry.getTeam();
		}
	}
}
