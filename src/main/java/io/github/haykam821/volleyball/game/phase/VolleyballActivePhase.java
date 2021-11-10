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
import io.github.haykam821.volleyball.game.player.team.VolleyballScoreboard;
import net.minecraft.entity.Entity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.SlimeEntity;
import net.minecraft.scoreboard.ServerScoreboard;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.world.GameMode;
import xyz.nucleoid.plasmid.chat.ChatChannel;
import xyz.nucleoid.plasmid.chat.HasChatChannel;
import xyz.nucleoid.plasmid.game.GameActivity;
import xyz.nucleoid.plasmid.game.GameCloseReason;
import xyz.nucleoid.plasmid.game.GameSpace;
import xyz.nucleoid.plasmid.game.common.GlobalWidgets;
import xyz.nucleoid.plasmid.game.common.team.GameTeam;
import xyz.nucleoid.plasmid.game.common.team.TeamSelectionLobby;
import xyz.nucleoid.plasmid.game.event.GameActivityEvents;
import xyz.nucleoid.plasmid.game.event.GamePlayerEvents;
import xyz.nucleoid.plasmid.game.player.PlayerOffer;
import xyz.nucleoid.plasmid.game.player.PlayerOfferResult;
import xyz.nucleoid.plasmid.game.rule.GameRuleType;
import xyz.nucleoid.stimuli.event.player.PlayerAttackEntityEvent;
import xyz.nucleoid.stimuli.event.player.PlayerChatEvent;
import xyz.nucleoid.stimuli.event.player.PlayerDeathEvent;

public class VolleyballActivePhase implements PlayerAttackEntityEvent, GameActivityEvents.Disable, GameActivityEvents.Enable, GameActivityEvents.Tick, GamePlayerEvents.Offer, PlayerDeathEvent, GamePlayerEvents.Remove, PlayerChatEvent {
	private final ServerWorld world;
	private final GameSpace gameSpace;
	private final VolleyballMap map;
	private final VolleyballConfig config;
	private final Set<PlayerEntry> players;
	private final Set<TeamEntry> teams;
	private final WinManager winManager = new WinManager(this);
	private final VolleyballScoreboard scoreboard;
	private SlimeEntity ball;
	private int ballTicks = 0;
	/**
	 * The number of ticks since the ball was last hit.
	 */
	private int inactiveBallTicks = 0;

	public VolleyballActivePhase(ServerWorld world, GameSpace gameSpace, VolleyballMap map, TeamSelectionLobby teamSelection, GlobalWidgets widgets, VolleyballConfig config, Text shortName) {
		this.world = world;
		this.gameSpace = gameSpace;
		this.map = map;
		this.config = config;

		this.players = new HashSet<>(this.gameSpace.getPlayers().size());

		int teamCount = this.config.getTeams().list().size();
		this.teams = new HashSet<>(teamCount);
		Map<GameTeam, TeamEntry> gameTeamsToEntries = new HashMap<>(teamCount);

		this.scoreboard = new VolleyballScoreboard(widgets, this, shortName);

		MinecraftServer server = this.world.getServer();
		ServerScoreboard scoreboard = server.getScoreboard();

		teamSelection.allocate(this.gameSpace.getPlayers(), (key, player) -> {
			GameTeam gameTeam = this.config.getTeams().byKey(key);

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
			GlobalWidgets widgets = GlobalWidgets.addTo(activity);
			VolleyballActivePhase phase = new VolleyballActivePhase(world, gameSpace, map, teamSelection, widgets, config, shortName);

			VolleyballActivePhase.setRules(activity);

			activity.listen(PlayerAttackEntityEvent.EVENT, phase);
			activity.listen(GameActivityEvents.DISABLE, phase);
			activity.listen(GameActivityEvents.ENABLE, phase);
			activity.listen(GameActivityEvents.TICK, phase);
			activity.listen(GamePlayerEvents.OFFER, phase);
			activity.listen(PlayerDeathEvent.EVENT, phase);
			activity.listen(GamePlayerEvents.REMOVE, phase);
			activity.listen(PlayerChatEvent.EVENT, phase);
		});
	}

	// Listeners
	@Override
	public ActionResult onAttackEntity(ServerPlayerEntity attacker, Hand hand, Entity attacked, EntityHitResult hitResult) {
		if (attacked == this.ball) {
			this.inactiveBallTicks = 0;
			return ActionResult.PASS;
		}
		return ActionResult.FAIL;
	}

	@Override
	public void onDisable() {
		MinecraftServer server = this.world.getServer();
		ServerScoreboard scoreboard = server.getScoreboard();

		for (TeamEntry team : this.teams) {
			scoreboard.removeTeam(team.getScoreboardTeam());
		}
	}

	@Override
	public void onEnable() {
		for (PlayerEntry player : this.players) {
			player.spawn();
			player.clearInventory();
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
		} else if (this.inactiveBallTicks >= this.config.getInactiveBallTicks()) {
			this.resetBall();
			this.gameSpace.getPlayers().sendMessage(this.getInactiveBallResetText());
		} else {
			this.inactiveBallTicks += 1;
			for (TeamEntry team : this.getTeams()) {
				if (team.isBallOnCourt(this.ball)) {
					team.getOtherTeam().incrementScore();
					this.scoreboard.update();
					break;
				}
			}
		}

		// Attempt to determine a winner
		if (this.winManager.checkForWinner()) {
			gameSpace.close(GameCloseReason.FINISHED);
		}
	}

	@Override
	public PlayerOfferResult onOfferPlayer(PlayerOffer offer) {
		return offer.accept(this.world, this.map.getWaitingSpawnPos()).and(() -> {
			this.setSpectator(offer.player());
		});
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

	@Override
	public ActionResult onSendChatMessage(ServerPlayerEntity sender, Text message) {
		TeamEntry team = this.getChatTeam(sender);
		if (team != null) {
			Text teamMessage = new TranslatableText("text.plasmid.chat.team", message);
			for (PlayerEntry player : this.players) {
				if (player.getTeam() == team) {
					player.getPlayer().sendSystemMessage(teamMessage, sender.getUuid());
				}
			}
		}

		return ActionResult.PASS;
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

	// Utilities
	public TeamEntry getChatTeam(ServerPlayerEntity sender) {
		if (!(sender instanceof HasChatChannel)) return null;
		if (((HasChatChannel) sender).getChatChannel() != ChatChannel.TEAM) return null;

		PlayerEntry entry = this.getPlayerEntry(sender);
		if (entry == null) return null;

		return entry.getTeam();
	}

	public SlimeEntity spawnBall() {
		this.ball = VolleyballSlimeEntity.createBall(this.world, this.config.getBallSize());
		this.inactiveBallTicks = 0;

		this.map.spawnAtBall(this.world, this.ball);
		this.world.spawnEntity(this.ball);

		return this.ball;
	}

	public void resetBall() {
		if (this.ball != null) {
			this.ball.discard();
			this.ball = null;
		}

		this.ballTicks = this.config.getResetBallTicks();
	}

	public Text getInactiveBallResetText() {
		return new TranslatableText("text.volleyball.inactive_ball_reset").formatted(Formatting.RED);
	}

	public void pling() {
		this.getGameSpace().getPlayers().playSound(SoundEvents.BLOCK_NOTE_BLOCK_PLING, SoundCategory.PLAYERS, 1, 1);
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
		player.changeGameMode(GameMode.SPECTATOR);
	}
}