package io.github.haykam821.volleyball.game.phase;

import io.github.haykam821.volleyball.game.VolleyballConfig;
import io.github.haykam821.volleyball.game.map.VolleyballMap;
import io.github.haykam821.volleyball.game.map.VolleyballMapBuilder;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.world.GameMode;
import xyz.nucleoid.fantasy.BubbleWorldConfig;
import xyz.nucleoid.plasmid.game.GameLogic;
import xyz.nucleoid.plasmid.game.GameOpenContext;
import xyz.nucleoid.plasmid.game.GameOpenProcedure;
import xyz.nucleoid.plasmid.game.GameSpace;
import xyz.nucleoid.plasmid.game.GameWaitingLobby;
import xyz.nucleoid.plasmid.game.StartResult;
import xyz.nucleoid.plasmid.game.TeamSelectionLobby;
import xyz.nucleoid.plasmid.game.config.PlayerConfig;
import xyz.nucleoid.plasmid.game.event.OfferPlayerListener;
import xyz.nucleoid.plasmid.game.event.PlayerAddListener;
import xyz.nucleoid.plasmid.game.event.PlayerDeathListener;
import xyz.nucleoid.plasmid.game.event.RequestStartListener;
import xyz.nucleoid.plasmid.game.player.JoinResult;
import xyz.nucleoid.plasmid.game.rule.GameRule;
import xyz.nucleoid.plasmid.game.rule.RuleResult;

public class VolleyballWaitingPhase implements PlayerAddListener, PlayerDeathListener, OfferPlayerListener, RequestStartListener {
	private final GameSpace gameSpace;
	private final VolleyballMap map;
	private final TeamSelectionLobby teamSelection;
	private final VolleyballConfig config;

	public VolleyballWaitingPhase(GameSpace gameSpace, VolleyballMap map, TeamSelectionLobby teamSelection, VolleyballConfig config) {
		this.gameSpace = gameSpace;
		this.map = map;
		this.teamSelection = teamSelection;
		this.config = config;
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
		game.setRule(GameRule.PVP, RuleResult.DENY);
		game.setRule(GameRule.TEAM_CHAT, RuleResult.DENY);
		game.setRule(GameRule.THROW_ITEMS, RuleResult.DENY);
	}

	public static GameOpenProcedure open(GameOpenContext<VolleyballConfig> context) {
		VolleyballConfig config = context.getConfig();

		VolleyballMapBuilder mapBuilder = new VolleyballMapBuilder(config);
		VolleyballMap map = mapBuilder.create();

		BubbleWorldConfig worldConfig = new BubbleWorldConfig()
			.setGenerator(map.createGenerator(context.getServer()))
			.setDefaultGameMode(GameMode.ADVENTURE);

		return context.createOpenProcedure(worldConfig, game -> {
			TeamSelectionLobby teamSelection = TeamSelectionLobby.applyTo(game, config.getTeams());
			VolleyballWaitingPhase phase = new VolleyballWaitingPhase(game.getSpace(), map, teamSelection, config);
			GameWaitingLobby.applyTo(game, config.getPlayerConfig());

			VolleyballWaitingPhase.setRules(game);

			// Listeners
			game.on(PlayerAddListener.EVENT, phase);
			game.on(PlayerDeathListener.EVENT, phase);
			game.on(OfferPlayerListener.EVENT, phase);
			game.on(RequestStartListener.EVENT, phase);
		});
	}

	// Listeners
	@Override
	public void onAddPlayer(ServerPlayerEntity player) {
		this.map.spawnAtWaiting(this.gameSpace.getWorld(), player);
	}

	@Override
	public ActionResult onDeath(ServerPlayerEntity player, DamageSource source) {
		this.map.spawnAtWaiting(this.gameSpace.getWorld(), player);
		return ActionResult.FAIL;
	}

	@Override
	public JoinResult offerPlayer(ServerPlayerEntity player) {
		return this.isFull() ? JoinResult.gameFull() : JoinResult.ok();
	}

	@Override
	public StartResult requestStart() {
		PlayerConfig playerConfig = this.config.getPlayerConfig();
		if (this.gameSpace.getPlayerCount() < playerConfig.getMinPlayers()) {
			return StartResult.NOT_ENOUGH_PLAYERS;
		}

		VolleyballActivePhase.open(this.gameSpace, this.map, this.teamSelection, this.config);
		return StartResult.OK;
	}

	// Utilities
	private boolean isFull() {
		return this.gameSpace.getPlayerCount() >= this.config.getPlayerConfig().getMaxPlayers();
	}
}