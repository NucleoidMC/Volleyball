package io.github.haykam821.volleyball.game.phase;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import eu.pb4.polymer.virtualentity.api.attachment.HolderAttachment;
import io.github.haykam821.volleyball.game.VolleyballConfig;
import io.github.haykam821.volleyball.game.ball.BallState;
import io.github.haykam821.volleyball.game.ball.InactiveBallState;
import io.github.haykam821.volleyball.game.ball.ReadyBallState;
import io.github.haykam821.volleyball.game.map.VolleyballMap;
import io.github.haykam821.volleyball.game.player.PlayerEntry;
import io.github.haykam821.volleyball.game.player.WinManager;
import io.github.haykam821.volleyball.game.player.team.TeamEntry;
import io.github.haykam821.volleyball.game.player.team.VolleyballScoreboard;
import net.minecraft.entity.Entity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.scoreboard.Team;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.world.GameMode;
import xyz.nucleoid.plasmid.api.chat.ChatChannel;
import xyz.nucleoid.plasmid.api.chat.HasChatChannel;
import xyz.nucleoid.plasmid.api.game.GameActivity;
import xyz.nucleoid.plasmid.api.game.GameCloseReason;
import xyz.nucleoid.plasmid.api.game.GameSpace;
import xyz.nucleoid.plasmid.api.game.common.GlobalWidgets;
import xyz.nucleoid.plasmid.api.game.common.team.GameTeam;
import xyz.nucleoid.plasmid.api.game.common.team.GameTeamConfig;
import xyz.nucleoid.plasmid.api.game.common.team.GameTeamKey;
import xyz.nucleoid.plasmid.api.game.common.team.TeamChat;
import xyz.nucleoid.plasmid.api.game.common.team.TeamManager;
import xyz.nucleoid.plasmid.api.game.common.team.TeamSelectionLobby;
import xyz.nucleoid.plasmid.api.game.event.GameActivityEvents;
import xyz.nucleoid.plasmid.api.game.event.GamePlayerEvents;
import xyz.nucleoid.plasmid.api.game.player.JoinAcceptor;
import xyz.nucleoid.plasmid.api.game.player.JoinAcceptorResult;
import xyz.nucleoid.plasmid.api.game.player.JoinOffer;
import xyz.nucleoid.plasmid.api.game.player.PlayerSet;
import xyz.nucleoid.plasmid.api.game.rule.GameRuleType;
import xyz.nucleoid.stimuli.event.EventResult;
import xyz.nucleoid.stimuli.event.player.PlayerAttackEntityEvent;
import xyz.nucleoid.stimuli.event.player.PlayerDeathEvent;

public class VolleyballActivePhase implements PlayerAttackEntityEvent, GameActivityEvents.Enable, GameActivityEvents.Tick, GamePlayerEvents.Accept, PlayerDeathEvent, GamePlayerEvents.Remove {
	private final ServerWorld world;
	private final GameSpace gameSpace;
	private final VolleyballMap map;
	private final VolleyballConfig config;
	private final Set<PlayerEntry> players;
	private final Set<TeamEntry> teams;
	private final WinManager winManager = new WinManager(this);
	private final VolleyballScoreboard scoreboard;

	private BallState ballState;
	private int ticksUntilClose = -1;

	public VolleyballActivePhase(ServerWorld world, GameSpace gameSpace, VolleyballMap map, TeamManager teamManager, GlobalWidgets widgets, VolleyballConfig config, Text shortName) {
		this.world = world;
		this.gameSpace = gameSpace;
		this.map = map;
		this.config = config;

		PlayerSet participants = this.gameSpace.getPlayers().participants();
		this.players = new HashSet<>(participants.size());

		int teamCount = this.config.getTeams().list().size();
		this.teams = new HashSet<>(teamCount);
		Map<GameTeamKey, TeamEntry> gameTeamsToEntries = new HashMap<>(teamCount);

		this.scoreboard = new VolleyballScoreboard(widgets, this, shortName);

		for (ServerPlayerEntity player : participants) {
			GameTeamKey teamKey = teamManager.teamFor(player);

			// Get or create team
			TeamEntry team = gameTeamsToEntries.get(teamKey);
			if (team == null) {
				team = new TeamEntry(this, teamKey, teamManager.getTeamConfig(teamKey), this.map.getTemplate());
				this.teams.add(team);
				gameTeamsToEntries.put(teamKey, team);
			}

			this.players.add(new PlayerEntry(this, player, team));
		}
	}

	public static void setRules(GameActivity activity) {
		activity.deny(GameRuleType.BLOCK_DROPS);
		activity.deny(GameRuleType.BREAK_BLOCKS);
		activity.deny(GameRuleType.CRAFTING);
		activity.deny(GameRuleType.FALL_DAMAGE);
		activity.deny(GameRuleType.HUNGER);
		activity.deny(GameRuleType.INTERACTION);
		activity.deny(GameRuleType.MODIFY_ARMOR);
		activity.deny(GameRuleType.MODIFY_INVENTORY);
		activity.deny(GameRuleType.PLACE_BLOCKS);
		activity.deny(GameRuleType.PORTALS);
		activity.allow(GameRuleType.PVP);
		activity.deny(GameRuleType.THROW_ITEMS);
	}

	public static void open(GameSpace gameSpace, ServerWorld world, VolleyballMap map, TeamSelectionLobby teamSelection, VolleyballConfig config, Text shortName) {
		gameSpace.setActivity(activity -> {
			TeamManager teamManager = TeamManager.addTo(activity);
			TeamChat.addTo(activity, teamManager);

			for (GameTeam team : config.getTeams()) {
				GameTeamConfig teamConfig = GameTeamConfig.builder(team.config())
					.setFriendlyFire(false)
					.setCollision(Team.CollisionRule.NEVER)
					.build();

				teamManager.addTeam(team.key(), teamConfig);
			}

			teamSelection.allocate(gameSpace.getPlayers().participants(), (teamKey, player) -> {
				teamManager.addPlayerTo(player, teamKey);
			});

			GlobalWidgets widgets = GlobalWidgets.addTo(activity);
			VolleyballActivePhase phase = new VolleyballActivePhase(world, gameSpace, map, teamManager, widgets, config, shortName);

			VolleyballActivePhase.setRules(activity);

			activity.listen(PlayerAttackEntityEvent.EVENT, phase);
			activity.listen(GameActivityEvents.ENABLE, phase);
			activity.listen(GameActivityEvents.TICK, phase);
			activity.listen(GamePlayerEvents.ACCEPT, phase);
			activity.listen(GamePlayerEvents.OFFER, JoinOffer::acceptParticipants);
			activity.listen(PlayerDeathEvent.EVENT, phase);
			activity.listen(GamePlayerEvents.REMOVE, phase);
		});
	}

	// Listeners
	@Override
	public EventResult onAttackEntity(ServerPlayerEntity attacker, Hand hand, Entity attacked, EntityHitResult hitResult) {
		PlayerEntry entry = this.getPlayerEntry(attacker);

		if (this.ballState.onAttackEntity(entry, attacked)) {
			return EventResult.PASS;
		}

		return EventResult.DENY;
	}

	@Override
	public void onEnable() {
		for (PlayerEntry player : this.players) {
			player.spawn();
			player.clearInventory();
		}

		for (ServerPlayerEntity player : this.gameSpace.getPlayers().spectators()) {
			this.map.spawnAtWaiting(this.world, player);
			player.changeGameMode(GameMode.SPECTATOR);
		}

		this.spawnBall();
	}

	@Override
	public void onTick() {
		for (PlayerEntry entry : this.players) {
			entry.onTick();
		}

		// Decrease ticks until game end to zero
		if (this.isGameEnding()) {
			if (this.ticksUntilClose == 0) {
				this.gameSpace.close(GameCloseReason.FINISHED);
			}

			this.ticksUntilClose -= 1;
			return;
		}

		this.ballState.onTick();

		// Attempt to determine a winner
		if (this.winManager.checkForWinner()) {
			this.endGame();
		}
	}

	@Override
	public JoinAcceptorResult onAcceptPlayers(JoinAcceptor acceptor) {
		return this.map.acceptJoins(acceptor, this.world, GameMode.SPECTATOR);
	}

	@Override
	public EventResult onDeath(ServerPlayerEntity player, DamageSource source) {
		PlayerEntry entry = this.getPlayerEntry(player);
		if (entry == null) {
			this.map.spawnAtWaiting(this.world, player);
		} else {
			entry.spawn();
		}

		return EventResult.DENY;
	}


	@Override
	public void onRemovePlayer(ServerPlayerEntity player) {
		PlayerEntry entry = this.getPlayerEntry(player);
		if (entry != null) {
			this.players.remove(entry);
		}
	}

	// Getters
	public ServerWorld getWorld() {
		return this.world;
	}

	public GameSpace getGameSpace() {
		return this.gameSpace;
	}

	public VolleyballMap getMap() {
		return this.map;
	}

	public VolleyballConfig getConfig() {
		return this.config;
	}

	public Set<PlayerEntry> getPlayers() {
		return this.players;
	}

	public Set<TeamEntry> getTeams() {
		return this.teams;
	}

	public VolleyballScoreboard getScoreboard() {
		return this.scoreboard;
	}

	// Utilities
	public TeamEntry getChatTeam(ServerPlayerEntity sender) {
		if (!(sender instanceof HasChatChannel)) return null;
		if (((HasChatChannel) sender).getChatChannel() != ChatChannel.TEAM) return null;

		PlayerEntry entry = this.getPlayerEntry(sender);
		if (entry == null) return null;

		return entry.getTeam();
	}

	/**
	 * Spawns a ball, transitioning it into the ready state.
	 */
	public void spawnBall() {
		Entity ball = this.config.getBallEntityConfig().createEntity(this.world, this.world.getRandom());
		this.setBallState(new ReadyBallState(this, ball));

		this.map.spawnAtBall(this.world, ball);
		this.world.spawnEntity(ball);
	}

	/**
	 * Resets the ball, transitioning it into the ready state.
	 */
	public void resetBall(TeamEntry markerTeam) {
		HolderAttachment markerAttachment = markerTeam == null ? null : this.ballState.createMarker(markerTeam, this.world);
		this.setBallState(new InactiveBallState(this, markerAttachment));
	}

	public void setBallState(BallState ballState) {
		if (this.ballState != null) {
			this.ballState.destroy(ballState);
		}

		this.ballState = ballState;
	}

	private void endGame() {
		this.ticksUntilClose = this.config.getTicksUntilClose().get(this.world.getRandom());
	}

	private boolean isGameEnding() {
		return this.ticksUntilClose >= 0;
	}

	public boolean hasBallLandedOffCourt(Entity ball) {
		return ball.isOnGround() && !this.map.getBallSpawnBox().intersects(ball.getBoundingBox());
	}

	public void pling() {
		this.getGameSpace().getPlayers().playSound(SoundEvents.BLOCK_NOTE_BLOCK_PLING.value(), SoundCategory.PLAYERS, 1, 1);
	}

	public PlayerEntry getPlayerEntry(ServerPlayerEntity player) {
		for (PlayerEntry entry : this.players) {
			if (player == entry.getPlayer()) {
				return entry;
			}
		}
		return null;
	}
}