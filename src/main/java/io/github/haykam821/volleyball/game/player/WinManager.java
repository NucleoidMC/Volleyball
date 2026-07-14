package io.github.haykam821.volleyball.game.player;

import io.github.haykam821.volleyball.game.phase.VolleyballActivePhase;
import io.github.haykam821.volleyball.game.player.team.TeamEntry;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;

public class WinManager {
	private final VolleyballActivePhase phase;

	public WinManager(VolleyballActivePhase phase) {
		this.phase = phase;
	}

	private Component getNoWinnersText() {
		return Component.translatable("text.volleyball.no_winners").withStyle(ChatFormatting.GOLD);
	}

	public boolean checkForWinner() {
		for (TeamEntry team : this.phase.getTeams()) {
			if (team.hasRequiredScore()) {
				this.phase.getGameSpace().getPlayers().sendMessage(team.getWinText());
				return true;
			}
		}

		if (this.phase.getPlayers().isEmpty()) {
			this.phase.getGameSpace().getPlayers().sendMessage(this.getNoWinnersText());
			return true;
		}

		return false;
	}
}
