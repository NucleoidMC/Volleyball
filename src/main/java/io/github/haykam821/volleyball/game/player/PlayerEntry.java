package io.github.haykam821.volleyball.game.player;

import java.util.Set;

import io.github.haykam821.volleyball.game.phase.VolleyballActivePhase;
import io.github.haykam821.volleyball.game.player.team.TeamEntry;
import net.minecraft.world.entity.Relative;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import xyz.nucleoid.map_templates.TemplateRegion;
import xyz.nucleoid.plasmid.api.game.event.GameActivityEvents;

public class PlayerEntry implements GameActivityEvents.Tick {
	private static final double OUT_OF_BOUNDS_CLAMP_INSET = 0.03;

	private final VolleyballActivePhase phase;
	private final ServerPlayer player;
	private final TeamEntry team;

	public PlayerEntry(VolleyballActivePhase phase, ServerPlayer player, TeamEntry team) {
		this.phase = phase;
		this.player = player;
		this.team = team;
	}

	// Listeners
	@Override
	public void onTick() {
		TemplateRegion area = this.team.getArea();
		if (area != null && !area.getBounds().contains(this.player.blockPosition())) {
			Vec3 pos = this.player.position();

			BlockPos min = area.getBounds().min();
			BlockPos max = area.getBounds().max();

			double x = clampOutOfBounds(pos.x(), min.getX(), max.getX() + 1);
			double y = clampOutOfBounds(pos.y(), min.getY(), max.getY() + 1);
			double z = clampOutOfBounds(pos.z(), min.getZ(), max.getZ() + 1);

			player.teleportTo(this.player.level(), x, y, z, Set.of(Relative.X_ROT, Relative.Y_ROT), 0, 0, true);
		}
	}

	// Getters
	public VolleyballActivePhase getPhase() {
		return this.phase;
	}

	public ServerPlayer getPlayer() {
		return this.player;
	}

	public TeamEntry getTeam() {
		return this.team;
	}

	// Utilities
	public void spawn() {
		this.team.spawn(this.phase.getLevel(), this.player);
	}

	public void clearInventory() {
		this.player.getInventory().clearContent();

		this.player.containerMenu.broadcastChanges();
		this.player.inventoryMenu.slotsChanged(this.player.getInventory());
	}

	private static double clampOutOfBounds(double value, double min, double max) {
		if (value < min || value > max) {
			return Mth.clamp(value, min + OUT_OF_BOUNDS_CLAMP_INSET, max - OUT_OF_BOUNDS_CLAMP_INSET);
		}

		return value;
	}
}
