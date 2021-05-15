package io.github.haykam821.volleyball.game.player;

import io.github.haykam821.volleyball.game.phase.VolleyballActivePhase;
import io.github.haykam821.volleyball.game.player.team.TeamEntry;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Formatting;

public class WinManager {
	private final VolleyballActivePhase phase;

	public WinManager(VolleyballActivePhase phase) {
		this.phase = phase;
	}

	private Text getNoWinnersText() {
		return new TranslatableText("text.volleyball.no_winners").formatted(Formatting.GOLD);
	}

	public boolean checkForWinner() {
		for (TeamEntry team : this.phase.getTeams()) {
			if (team.hasRequiredScore()) {
				this.phase.getGameSpace().getPlayers().sendMessage(team.getWinText());
				return true;
			}
		}

		if (this.phase.getPlayers().size() == 0) {
			this.phase.getGameSpace().getPlayers().sendMessage(this.getNoWinnersText());
			return true;
		}

		return false;
	}
}
