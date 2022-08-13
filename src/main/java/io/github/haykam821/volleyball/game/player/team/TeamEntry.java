package io.github.haykam821.volleyball.game.player.team;

import io.github.haykam821.volleyball.game.phase.VolleyballActivePhase;
import net.minecraft.entity.mob.SlimeEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import xyz.nucleoid.map_templates.BlockBounds;
import xyz.nucleoid.map_templates.MapTemplate;
import xyz.nucleoid.map_templates.TemplateRegion;
import xyz.nucleoid.plasmid.game.common.team.GameTeamConfig;
import xyz.nucleoid.plasmid.game.common.team.GameTeamKey;

public class TeamEntry implements Comparable<TeamEntry> {
	private static final BlockBounds DEFAULT_BOUNDS = BlockBounds.ofBlock(BlockPos.ORIGIN);

	private final VolleyballActivePhase phase;

	private final GameTeamKey key;
	private final GameTeamConfig config;

	private final TemplateRegion area;
	private final TemplateRegion spawn;
	private final Box courtBox;

	private int score;

	public TeamEntry(VolleyballActivePhase phase, GameTeamKey key, GameTeamConfig config, MapTemplate template) {
		this.phase = phase;

		this.key = key;
		this.config = config;

		this.area = this.getRegion(template, "area");
		this.spawn = this.getRegion(template, "spawn");
		this.courtBox = this.getBoundsOrDefault(template, "court").asBox();
	}

	// Getters
	public TemplateRegion getArea() {
		return this.area;
	}

	// Utilities
	public void spawn(ServerWorld world, ServerPlayerEntity player) {
		Vec3d spawnPos = this.spawn.getBounds().centerBottom();
		float yaw = this.spawn.getData().getFloat("Facing");
	
		player.teleport(world, spawnPos.getX(), spawnPos.getY(), spawnPos.getZ(), yaw, 0);
	}

	public boolean isBallOnCourt(SlimeEntity ball) {
		if (!this.courtBox.intersects(ball.getBoundingBox())) {
			return false;
		}

		return ball.isOnGround() || ball.getY() < this.courtBox.minY;
	}

	public int incrementScore() {
		this.phase.getGameSpace().getPlayers().sendMessage(this.getScoreText());
		this.phase.pling();
		this.phase.resetBall();

		return this.score += 1;
	}

	public boolean hasRequiredScore() {
		return this.score >= this.phase.getConfig().getRequiredScore();
	}

	public Text getScoreText() {
		return new TranslatableText("text.volleyball.score", this.getName()).formatted(Formatting.GOLD);
	}

	public Text getWinText() {
		return new TranslatableText("text.volleyball.win", this.getName()).formatted(Formatting.GOLD);
	}

	public Text getScoreboardEntryText() {
		return new TranslatableText("text.volleyball.scoreboard.entry", this.getName(), this.score);
	}

	public Text getName() {
		return this.config.name();
	}

	private BlockBounds getBoundsOrDefault(MapTemplate template, String key) {
		TemplateRegion region = this.getRegion(template, key);
		return region == null ? DEFAULT_BOUNDS : region.getBounds();
	}

	private TemplateRegion getRegion(MapTemplate template, String key) {
		return template.getMetadata().getFirstRegion(this.key.id() + "_" + key);
	}

	public TeamEntry getOtherTeam() {
		for (TeamEntry team : this.phase.getTeams()) {
			if (this != team) return team;
		}
		return this;
	}

	@Override
	public int compareTo(TeamEntry other) {
		return this.score - other.score;
	}
}
