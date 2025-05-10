package io.github.haykam821.volleyball.game;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.github.haykam821.volleyball.entity.BallEntityConfig;
import net.minecraft.SharedConstants;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.intprovider.ConstantIntProvider;
import net.minecraft.util.math.intprovider.IntProvider;
import xyz.nucleoid.plasmid.api.game.common.config.WaitingLobbyConfig;
import xyz.nucleoid.plasmid.api.game.common.team.GameTeamList;

public class VolleyballConfig {
	public static final MapCodec<VolleyballConfig> CODEC = RecordCodecBuilder.mapCodec(instance -> {
		return instance.group(
			Identifier.CODEC.fieldOf("map").forGetter(VolleyballConfig::getMap),
			WaitingLobbyConfig.CODEC.fieldOf("players").forGetter(VolleyballConfig::getPlayerConfig),
			GameTeamList.CODEC.fieldOf("teams").forGetter(VolleyballConfig::getTeams),
			BallEntityConfig.CODEC.optionalFieldOf("ball_entity", BallEntityConfig.DEFAULT).forGetter(VolleyballConfig::getBallEntityConfig),
			IntProvider.NON_NEGATIVE_CODEC.optionalFieldOf("ticks_until_close", ConstantIntProvider.create(SharedConstants.TICKS_PER_SECOND * 5)).forGetter(VolleyballConfig::getTicksUntilClose),
			Codec.INT.optionalFieldOf("required_score", 10).forGetter(VolleyballConfig::getRequiredScore),
			Codec.INT.optionalFieldOf("reset_ball_ticks", 20 * 3).forGetter(VolleyballConfig::getResetBallTicks),
			Codec.INT.optionalFieldOf("inactive_ball_ticks", 20 * 15).forGetter(VolleyballConfig::getInactiveBallTicks)
		).apply(instance, VolleyballConfig::new);
	});

	private final Identifier map;
	private final WaitingLobbyConfig playerConfig;
	private final GameTeamList teams;
	private final BallEntityConfig ballEntityConfig;
	private final int requiredScore;
	private final int resetBallTicks;
	private final int inactiveBallTicks;
	private final IntProvider ticksUntilClose;

	public VolleyballConfig(Identifier map, WaitingLobbyConfig playerConfig, GameTeamList teams, BallEntityConfig ballEntityConfig, IntProvider ticksUntilClose, int requiredScore, int resetBallTicks, int inactiveBallTicks) {
		this.map = map;
		this.playerConfig = playerConfig;
		this.teams = teams;
		this.ballEntityConfig = ballEntityConfig;
		this.ticksUntilClose = ticksUntilClose;
		this.requiredScore = requiredScore;
		this.resetBallTicks = resetBallTicks;
		this.inactiveBallTicks = inactiveBallTicks;
	}

	public Identifier getMap() {
		return this.map;
	}

	public WaitingLobbyConfig getPlayerConfig() {
		return this.playerConfig;
	}

	public GameTeamList getTeams() {
		return this.teams;
	}

	public BallEntityConfig getBallEntityConfig() {
		return this.ballEntityConfig;
	}

	public IntProvider getTicksUntilClose() {
		return this.ticksUntilClose;
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