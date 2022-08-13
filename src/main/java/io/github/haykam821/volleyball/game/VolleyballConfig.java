package io.github.haykam821.volleyball.game;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.github.haykam821.volleyball.entity.BallEntityConfig;
import net.minecraft.util.Identifier;
import xyz.nucleoid.plasmid.game.common.config.PlayerConfig;
import xyz.nucleoid.plasmid.game.common.team.GameTeamList;

public class VolleyballConfig {
	public static final Codec<VolleyballConfig> CODEC = RecordCodecBuilder.create(instance -> {
		return instance.group(
			Identifier.CODEC.fieldOf("map").forGetter(VolleyballConfig::getMap),
			PlayerConfig.CODEC.fieldOf("players").forGetter(VolleyballConfig::getPlayerConfig),
			GameTeamList.CODEC.fieldOf("teams").forGetter(VolleyballConfig::getTeams),
			BallEntityConfig.CODEC.optionalFieldOf("ball_entity", BallEntityConfig.DEFAULT).forGetter(VolleyballConfig::getBallEntityConfig),
			Codec.INT.optionalFieldOf("required_score", 10).forGetter(VolleyballConfig::getRequiredScore),
			Codec.INT.optionalFieldOf("reset_ball_ticks", 20 * 3).forGetter(VolleyballConfig::getResetBallTicks),
			Codec.INT.optionalFieldOf("inactive_ball_ticks", 20 * 15).forGetter(VolleyballConfig::getInactiveBallTicks)
		).apply(instance, VolleyballConfig::new);
	});

	private final Identifier map;
	private final PlayerConfig playerConfig;
	private final GameTeamList teams;
	private final BallEntityConfig ballEntityConfig;
	private final int requiredScore;
	private final int resetBallTicks;
	private final int inactiveBallTicks;

	public VolleyballConfig(Identifier map, PlayerConfig playerConfig, GameTeamList teams, BallEntityConfig ballEntityConfig, int requiredScore, int resetBallTicks, int inactiveBallTicks) {
		this.map = map;
		this.playerConfig = playerConfig;
		this.teams = teams;
		this.ballEntityConfig = ballEntityConfig;
		this.requiredScore = requiredScore;
		this.resetBallTicks = resetBallTicks;
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

	public BallEntityConfig getBallEntityConfig() {
		return this.ballEntityConfig;
	}

	public int getRequiredScore() {
		return this.requiredScore;
	}

	public int getResetBallTicks() {
		return this.resetBallTicks;
	}

	public int getInactiveBallTicks() {
		return this.inactiveBallTicks;
	}
}