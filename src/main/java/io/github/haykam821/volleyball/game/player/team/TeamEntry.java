package io.github.haykam821.volleyball.game.player.team;

import java.util.Set;

import io.github.haykam821.volleyball.game.map.VolleyballMap;
import io.github.haykam821.volleyball.game.phase.VolleyballActivePhase;
import net.minecraft.world.entity.Entity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import xyz.nucleoid.map_templates.BlockBounds;
import xyz.nucleoid.map_templates.MapTemplate;
import xyz.nucleoid.map_templates.TemplateRegion;
import xyz.nucleoid.plasmid.api.game.common.team.GameTeamConfig;
import xyz.nucleoid.plasmid.api.game.common.team.GameTeamKey;

public class TeamEntry implements Comparable<TeamEntry> {
	private static final BlockBounds DEFAULT_BOUNDS = BlockBounds.ofBlock(BlockPos.ZERO);

	private final VolleyballActivePhase phase;

	private final GameTeamKey key;
	private final GameTeamConfig config;

	private final TemplateRegion area;
	private final TemplateRegion spawn;
	private final AABB courtBox;

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
	public void spawn(ServerLevel level, ServerPlayer player) {
		Vec3 spawnPos = this.spawn.getBounds().centerBottom();
		float yaw = this.spawn.getData().getFloatOr(VolleyballMap.FACING_KEY, 0);
	
		player.teleportTo(level, spawnPos.x(), spawnPos.y(), spawnPos.z(), Set.of(), yaw, 0, true);
	}

	public boolean isBallOnCourt(Entity ball) {
		if (!this.courtBox.intersects(ball.getBoundingBox())) {
			return false;
		}

		return ball.onGround() || ball.getY() < this.courtBox.minY;
	}

	public void incrementOtherTeamScore() {
		this.getOtherTeam().incrementScore(this);
	}

	private void incrementScore(TeamEntry causer) {
		this.phase.getGameSpace().getPlayers().sendMessage(this.getScoreText());
		this.phase.pling();

		this.phase.resetBall(causer);

		this.score += 1;
		this.phase.getScoreboard().update();
	}

	public boolean hasRequiredScore() {
		return this.score >= this.phase.getConfig().getRequiredScore();
	}

	public Component getScoreText() {
		return Component.translatable("text.volleyball.score", this.getName()).withStyle(ChatFormatting.GOLD);
	}

	public Component getWinText() {
		return Component.translatable("text.volleyball.win", this.getName()).withStyle(ChatFormatting.GOLD);
	}

	public Component getScoreboardEntryText() {
		return Component.translatable("text.volleyball.scoreboard.entry", this.getName(), this.score);
	}

	public ChatFormatting getFormatting() {
		return this.config.chatFormatting();
	}

	public Component getName() {
		return this.config.name();
	}

	private BlockBounds getBoundsOrDefault(MapTemplate template, String key) {
		TemplateRegion region = this.getRegion(template, key);
		return region == null ? DEFAULT_BOUNDS : region.getBounds();
	}

	private TemplateRegion getRegion(MapTemplate template, String key) {
		return template.getMetadata().getFirstRegion(this.key.id() + "_" + key);
	}

	private TeamEntry getOtherTeam() {
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
