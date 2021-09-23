package io.github.haykam821.volleyball.game;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.util.Identifier;
import xyz.nucleoid.plasmid.game.common.config.PlayerConfig;
import xyz.nucleoid.plasmid.game.common.team.GameTeamList;

public class VolleyballConfig {
	public static final Codec<VolleyballConfig> CODEC = RecordCodecBuilder.create(instance -> {
		return instance.group(
			Identifier.CODEC.fieldOf("map").forGetter(VolleyballConfig::getMap),
			PlayerConfig.CODEC.fieldOf("players").forGetter(VolleyballConfig::getPlayerConfig),
			GameTeamList.CODEC.fieldOf("teams").forGetter(VolleyballConfig::getTeams),
			Codec.INT.optionalFieldOf("required_score", 10).forGetter(VolleyballConfig::getRequiredScore),
			Codec.INT.optionalFieldOf("reset_ball_ticks", 20 * 3).forGetter(VolleyballConfig::getResetBallTicks),
			Codec.INT.optionalFieldOf("ball_size", 1).forGetter(VolleyballConfig::getBallSize),
			Codec.INT.optionalFieldOf("inactive_ball_ticks", 20 * 15).forGetter(VolleyballConfig::getInactiveBallTicks)
		).apply(instance, VolleyballConfig::new);
	});

	private final Identifier map;
	private final PlayerConfig playerConfig;
	private final GameTeamList teams;
	private final int requiredScore;
	private final int resetBallTicks;
	private final int ballSize;
	private final int inactiveBallTicks;

	public VolleyballConfig(Identifier map, PlayerConfig playerConfig, GameTeamList teams, int requiredScore, int resetBallTicks, int ballSize, int inactiveBallTicks) {
		this.map = map;
		this.playerConfig = playerConfig;
		this.teams = teams;
		this.requiredScore = requiredScore;
		this.resetBallTicks = resetBallTicks;
		this.ballSize = ballSize;
		this.inactiveBallTicks = inactiveBallTicks;
	}

	public Identifier getMap() {
		return this.map;
	}

	public PlayerConfig getPlayerConfig() {
		return this.playerConfig;
	}

	public GameTeamList getTeams() {
		return this.teams;
	}

	public int getRequiredScore() {
		return this.requiredScore;
	}

	public int getResetBallTicks() {
		return this.resetBallTicks;
	}

	public int getBallSize() {
		return this.ballSize;
	}

	public int getInactiveBallTicks() {
		return this.inactiveBallTicks;
	}
}