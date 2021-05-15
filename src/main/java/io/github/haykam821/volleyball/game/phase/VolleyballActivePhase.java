package io.github.haykam821.volleyball.game.phase;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import io.github.haykam821.volleyball.entity.VolleyballSlimeEntity;
import io.github.haykam821.volleyball.game.VolleyballConfig;
import io.github.haykam821.volleyball.game.map.VolleyballMap;
import io.github.haykam821.volleyball.game.player.PlayerEntry;
import io.github.haykam821.volleyball.game.player.WinManager;
import io.github.haykam821.volleyball.game.player.team.TeamEntry;
import net.minecraft.entity.Entity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.SlimeEntity;
import net.minecraft.scoreboard.ServerScoreboard;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.world.GameMode;
import xyz.nucleoid.plasmid.game.GameCloseReason;
import xyz.nucleoid.plasmid.game.GameLogic;
import xyz.nucleoid.plasmid.game.GameSpace;
import xyz.nucleoid.plasmid.game.TeamSelectionLobby;
import xyz.nucleoid.plasmid.game.event.AttackEntityListener;
import xyz.nucleoid.plasmid.game.event.GameCloseListener;
import xyz.nucleoid.plasmid.game.event.GameOpenListener;
import xyz.nucleoid.plasmid.game.event.GameTickListener;
import xyz.nucleoid.plasmid.game.event.PlayerAddListener;
import xyz.nucleoid.plasmid.game.event.PlayerDeathListener;
import xyz.nucleoid.plasmid.game.event.PlayerRemoveListener;
import xyz.nucleoid.plasmid.game.player.GameTeam;
import xyz.nucleoid.plasmid.game.rule.GameRule;
import xyz.nucleoid.plasmid.game.rule.RuleResult;

public class VolleyballActivePhase implements AttackEntityListener, GameCloseListener, GameOpenListener, GameTickListener, PlayerAddListener, PlayerDeathListener, PlayerRemoveListener {
	private final ServerWorld world;
	private final GameSpace gameSpace;
	private final VolleyballMap map;
	private final VolleyballConfig config;
	private final Set<PlayerEntry> players;
	private final Set<TeamEntry> teams;
	private final WinManager winManager = new WinManager(this);
	private boolean opened;
	private SlimeEntity ball;
	private int ballTicks = 0;

	public VolleyballActivePhase(GameSpace gameSpace, VolleyballMap map, TeamSelectionLobby teamSelection, VolleyballConfig config) {
		this.world = gameSpace.getWorld();
		this.gameSpace = gameSpace;
		this.map = map;
		this.config = config;

		this.players = new HashSet<>(this.gameSpace.getPlayerCount());
		this.teams = new HashSet<>(this.config.getTeams().size());
		Map<GameTeam, TeamEntry> gameTeamsToEntries = new HashMap<>(this.config.getTeams().size());

		MinecraftServer server = this.world.getServer();
		ServerScoreboard scoreboard = server.getScoreboard();

		teamSelection.allocate((gameTeam, player) -> {
			// Get or create team
			TeamEntry team = gameTeamsToEntries.get(gameTeam);
			if (team == null) {
				team = new TeamEntry(this, gameTeam, server, this.map.getTemplate());
				this.teams.add(team);
				gameTeamsToEntries.put(gameTeam, team);
			}

			this.players.add(new PlayerEntry(this, player, team));
			scoreboard.addPlayerToTeam(player.getEntityName(), team.getScoreboardTeam());
		});
	}

	public static void setRules(GameLogic game) {
		game.setRule(GameRule.BLOCK_DROPS, RuleResult.DENY);
		game.setRule(GameRule.BREAK_BLOCKS, RuleResult.DENY);
		game.setRule(GameRule.CRAFTING, RuleResult.DENY);
		game.setRule(GameRule.FALL_DAMAGE, RuleResult.DENY);
		game.setRule(GameRule.HUNGER, RuleResult.DENY);
		game.setRule(GameRule.INTERACTION, RuleResult.DENY);
		game.setRule(GameRule.MODIFY_ARMOR, RuleResult.DENY);
		game.setRule(GameRule.MODIFY_INVENTORY, RuleResult.DENY);
		game.setRule(GameRule.PLACE_BLOCKS, RuleResult.DENY);
		game.setRule(GameRule.PORTALS, RuleResult.DENY);
		game.setRule(GameRule.PVP, RuleResult.ALLOW);
		game.setRule(GameRule.TEAM_CHAT, RuleResult.ALLOW);
		game.setRule(GameRule.THROW_ITEMS, RuleResult.DENY);
	}

	public static void open(GameSpace gameSpace, VolleyballMap map, TeamSelectionLobby teamSelection, VolleyballConfig config) {
		VolleyballActivePhase phase = new VolleyballActivePhase(gameSpace, map, teamSelection, config);

		gameSpace.openGame(game -> {
			VolleyballActivePhase.setRules(game);

			game.on(AttackEntityListener.EVENT, phase);
			game.on(GameCloseListener.EVENT, phase);
			game.on(GameOpenListener.EVENT, phase);
			game.on(GameTickListener.EVENT, phase);
			game.on(PlayerAddListener.EVENT, phase);
			game.on(PlayerDeathListener.EVENT, phase);
			game.on(PlayerRemoveListener.EVENT, phase);
		});
	}

	// Listeners
	@Override
	public ActionResult onAttackEntity(ServerPlayerEntity attacker, Hand hand, Entity attacked, EntityHitResult hitResult) {
		return attacked == this.ball ? ActionResult.PASS : ActionResult.FAIL;
	}

	@Override
	public void onClose() {
		MinecraftServer server = this.world.getServer();
		ServerScoreboard scoreboard = server.getScoreboard();

		for (TeamEntry team : this.teams) {
			scoreboard.removeTeam(team.getScoreboardTeam());
		}
	}

	@Override
	public void onOpen() {
		this.opened = true;

		for (PlayerEntry player : this.players) {
			player.spawn();
		}
	}

	@Override
	public void onTick() {
		for (PlayerEntry entry : this.players) {
			entry.onTick();
		}

		if (this.ball == null || !this.ball.isAlive()) {
			this.ballTicks -= 1;
			if (this.ballTicks <= 0) {
				this.spawnBall();
			}
		} else {
			for (TeamEntry team : this.getTeams()) {
				if (team.getCourtBounds().contains(this.ball.getBlockPos())) {
					team.incrementScore();
				}
			}
		}

		// Attempt to determine a winner
		if (this.winManager.checkForWinner()) {
			gameSpace.close(GameCloseReason.FINISHED);
		}
	}

	@Override
	public void onAddPlayer(ServerPlayerEntity player) {
		PlayerEntry entry = this.getPlayerEntry(player);
		if (entry == null) {
			this.setSpectator(player);
		} else if (this.opened) {
			entry.spawn();
		}
	}

	@Override
	public ActionResult onDeath(ServerPlayerEntity player, DamageSource source) {
		PlayerEntry entry = this.getPlayerEntry(player);
		if (entry == null) {
			this.map.spawnAtWaiting(this.world, player);
		} else {
			entry.spawn();
		}

		return ActionResult.FAIL;
	}


	@Override
	public void onRemovePlayer(ServerPlayerEntity player) {
		PlayerEntry entry = this.getPlayerEntry(player);
		if (entry != null) {
			this.players.remove(entry);
		}
	}

	// Getters
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

	// Utilities
	public SlimeEntity spawnBall() {
		this.ball = VolleyballSlimeEntity.createBall(this.world);

		this.map.spawnAtBall(this.world, this.ball);
		this.world.spawnEntity(this.ball);

		return this.ball;
	}

	public void resetBall() {
		if (this.ball != null) {
			this.ball.kill();
			this.ball = null;
		}

		this.ballTicks = this.config.getResetBallTicks();
	}

	public void pling() {
		this.getGameSpace().getPlayers().sendSound(SoundEvents.BLOCK_NOTE_BLOCK_PLING, SoundCategory.PLAYERS, 1, 1);
	}

	public PlayerEntry getPlayerEntry(ServerPlayerEntity player) {
		for (PlayerEntry entry : this.players) {
			if (player == entry.getPlayer()) {
				return entry;
			}
		}
		return null;
	}

	private void setSpectator(ServerPlayerEntity player) {
		player.setGameMode(GameMode.SPECTATOR);
	}
}