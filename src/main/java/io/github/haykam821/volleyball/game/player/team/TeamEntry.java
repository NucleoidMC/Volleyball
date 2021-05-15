package io.github.haykam821.volleyball.game.player.team;

import org.apache.commons.lang3.RandomStringUtils;

import io.github.haykam821.volleyball.game.phase.VolleyballActivePhase;
import net.minecraft.entity.mob.SlimeEntity;
import net.minecraft.scoreboard.ServerScoreboard;
import net.minecraft.scoreboard.Team;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Vec3d;
import xyz.nucleoid.plasmid.game.player.GameTeam;
import xyz.nucleoid.plasmid.map.template.MapTemplate;
import xyz.nucleoid.plasmid.map.template.TemplateRegion;
import xyz.nucleoid.plasmid.util.BlockBounds;

public class TeamEntry {
	private final VolleyballActivePhase phase;
	private final GameTeam gameTeam;
	private final Team scoreboardTeam;
	private final TemplateRegion area;
	private final TemplateRegion spawn;
	private final BlockBounds courtBounds;
	private int score;

	public TeamEntry(VolleyballActivePhase phase, GameTeam gameTeam, MinecraftServer server, MapTemplate template) {
		this.phase = phase;
		this.gameTeam = gameTeam;

		ServerScoreboard scoreboard = server.getScoreboard();
		String key = RandomStringUtils.randomAlphanumeric(16);
		this.scoreboardTeam = TeamEntry.getOrCreateScoreboardTeam(key, scoreboard);
		this.initializeTeam();

		this.area = this.getRegion(template, "area");
		this.spawn = this.getRegion(template, "spawn");
		this.courtBounds = this.getBoundsOrDefault(template, "court");
	}

	// Getters
	public GameTeam getGameTeam() {
		return this.gameTeam;
	}

	public Team getScoreboardTeam() {
		return this.scoreboardTeam;
	}

	public TemplateRegion getArea() {
		return this.area;
	}

	public BlockBounds getCourtBounds() {
		return this.courtBounds;
	}

	// Utilities
	public void spawn(ServerWorld world, ServerPlayerEntity player) {
		Vec3d spawnPos = this.spawn.getBounds().getCenterBottom();
		float yaw = this.spawn.getData().getFloat("Facing");
	
		player.teleport(world, spawnPos.getX(), spawnPos.getY(), spawnPos.getZ(), yaw, 0);
	}

	public boolean isBallOnCourt(SlimeEntity ball) {
		return ball.isOnGround() && this.courtBounds.contains(ball.getBlockPos());
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

	public Text getName() {
		return new LiteralText(this.gameTeam.getDisplay()).formatted(this.gameTeam.getFormatting());
	}

	private Text getUncoloredName() {
		return new LiteralText(this.gameTeam.getDisplay());
	}

	private void initializeTeam() {
		// Display
		this.scoreboardTeam.setDisplayName(this.getUncoloredName());
		this.scoreboardTeam.setColor(this.gameTeam.getFormatting());

		// Rules
		this.scoreboardTeam.setFriendlyFireAllowed(false);
		this.scoreboardTeam.setShowFriendlyInvisibles(true);
		this.scoreboardTeam.setCollisionRule(Team.CollisionRule.NEVER);
	}

	private BlockBounds getBoundsOrDefault(MapTemplate template, String key) {
		TemplateRegion region = this.getRegion(template, key);
		return region == null ? BlockBounds.EMPTY : region.getBounds();
	}

	private TemplateRegion getRegion(MapTemplate template, String key) {
		return template.getMetadata().getFirstRegion(this.gameTeam.getKey() + "_" + key);
	}

	private static Team getOrCreateScoreboardTeam(String key, ServerScoreboard scoreboard) {
		Team scoreboardTeam = scoreboard.getTeam(key);
		if (scoreboardTeam == null) {
			return scoreboard.addTeam(key);
		}
		return scoreboardTeam;
	}
}
