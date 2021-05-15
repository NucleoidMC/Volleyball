package io.github.haykam821.volleyball.game;

import java.util.List;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.util.Identifier;
import xyz.nucleoid.plasmid.game.config.PlayerConfig;
import xyz.nucleoid.plasmid.game.player.GameTeam;

public class VolleyballConfig {
	public static final Codec<VolleyballConfig> CODEC = RecordCodecBuilder.create(instance -> {
		return instance.group(
			Identifier.CODEC.fieldOf("map").forGetter(VolleyballConfig::getMap),
			PlayerConfig.CODEC.fieldOf("players").forGetter(VolleyballConfig::getPlayerConfig),
			GameTeam.CODEC.listOf().fieldOf("teams").forGetter(VolleyballConfig::getTeams),
			Codec.INT.optionalFieldOf("required_score", 10).forGetter(VolleyballConfig::getRequiredScore),
			Codec.INT.optionalFieldOf("reset_ball_ticks", 20 * 3).forGetter(VolleyballConfig::getResetBallTicks)
		).apply(instance, VolleyballConfig::new);
	});

	private final Identifier map;
	private final PlayerConfig playerConfig;
	private final List<GameTeam> teams;
	private final int requiredScore;
	private final int resetBallTicks;

	public VolleyballConfig(Identifier map, PlayerConfig playerConfig, List<GameTeam> teams, int requiredScore, int resetBallTicks) {
		this.map = map;
		this.playerConfig = playerConfig;
		this.teams = teams;
		this.requiredScore = requiredScore;
		this.resetBallTicks = resetBallTicks;
	}

	public Identifier getMap() {
		return this.map;
	}

	public PlayerConfig getPlayerConfig() {
		return this.playerConfig;
	}

	public List<GameTeam> getTeams() {
		return this.teams;
	}

	public int getRequiredScore() {
		return this.requiredScore;
	}

	public int getResetBallTicks() {
		return this.resetBallTicks;
	}
}