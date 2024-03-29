package io.github.haykam821.volleyball.game.player.team;

import io.github.haykam821.volleyball.game.phase.VolleyballActivePhase;
import net.minecraft.text.Text;
import xyz.nucleoid.plasmid.game.common.GlobalWidgets;
import xyz.nucleoid.plasmid.game.common.widget.SidebarWidget;

public class VolleyballScoreboard {
	private final SidebarWidget widget;
	private final VolleyballActivePhase phase;

	public VolleyballScoreboard(GlobalWidgets widgets, VolleyballActivePhase phase, Text shortName) {
		Text name = shortName.copy().styled(style -> {
			return style.withBold(true);
		});
		this.widget = widgets.addSidebar(name);

		this.phase = phase;
	}

	public void update() {
		this.widget.set(content -> {
			this.phase.getTeams().stream().sorted().forEach(team -> {
				content.add(team.getScoreboardEntryText());
			});
		});
	}
}
