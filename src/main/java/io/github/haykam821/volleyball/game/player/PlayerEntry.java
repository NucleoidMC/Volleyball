package io.github.haykam821.volleyball.game.player;

import java.util.Set;

import io.github.haykam821.volleyball.game.phase.VolleyballActivePhase;
import io.github.haykam821.volleyball.game.player.team.TeamEntry;
import net.minecraft.network.packet.s2c.play.PositionFlag;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import xyz.nucleoid.map_templates.TemplateRegion;
import xyz.nucleoid.plasmid.api.game.event.GameActivityEvents;

public class PlayerEntry implements GameActivityEvents.Tick {
	private static final double OUT_OF_BOUNDS_CLAMP_INSET = 0.03;

	private final VolleyballActivePhase phase;
	private final ServerPlayerEntity player;
	private final TeamEntry team;

	public PlayerEntry(VolleyballActivePhase phase, ServerPlayerEntity player, TeamEntry team) {
		this.phase = phase;
		this.player = player;
		this.team = team;
	}

	// Listeners
	@Override
	public void onTick() {
		TemplateRegion area = this.team.getArea();
		if (area != null && !area.getBounds().contains(this.player.getBlockPos())) {
			Vec3d pos = this.player.getPos();

			BlockPos min = area.getBounds().min();
			BlockPos max = area.getBounds().max();

			double x = clampOutOfBounds(pos.getX(), min.getX(), max.getX() + 1);
			double y = clampOutOfBounds(pos.getY(), min.getY(), max.getY() + 1);
			double z = clampOutOfBounds(pos.getZ(), min.getZ(), max.getZ() + 1);

			player.teleport(this.player.getServerWorld(), x, y, z, Set.of(PositionFlag.X_ROT, PositionFlag.Y_ROT), 0, 0, true);
		}
	}

	// Getters
	public VolleyballActivePhase getPhase() {
		return this.phase;
	}

	public ServerPlayerEntity getPlayer() {
		return this.player;
	}

	public TeamEntry getTeam() {
		return this.team;
	}

	// Utilities
	public void spawn() {
		this.team.spawn(this.phase.getWorld(), this.player);
	}

	public void clearInventory() {
		this.player.getInventory().clear();

		this.player.currentScreenHandler.sendContentUpdates();
		this.player.playerScreenHandler.onContentChanged(this.player.getInventory());
	}

	private static double clampOutOfBounds(double value, double min, double max) {
		if (value < min || value > max) {
			return MathHelper.clamp(value, min + OUT_OF_BOUNDS_CLAMP_INSET, max - OUT_OF_BOUNDS_CLAMP_INSET);
		}

		return value;
	}
}
