package io.github.haykam821.volleyball.game.phase;

import io.github.haykam821.volleyball.game.VolleyballConfig;
import io.github.haykam821.volleyball.game.map.VolleyballMap;
import io.github.haykam821.volleyball.game.map.VolleyballMapBuilder;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.core.Holder;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.GameType;
import xyz.nucleoid.fantasy.RuntimeLevelConfig;
import xyz.nucleoid.plasmid.api.game.GameActivity;
import xyz.nucleoid.plasmid.api.game.GameOpenContext;
import xyz.nucleoid.plasmid.api.game.GameOpenProcedure;
import xyz.nucleoid.plasmid.api.game.GameResult;
import xyz.nucleoid.plasmid.api.game.GameSpace;
import xyz.nucleoid.plasmid.api.game.common.GameWaitingLobby;
import xyz.nucleoid.plasmid.api.game.common.team.TeamSelectionLobby;
import xyz.nucleoid.plasmid.api.game.config.GameConfig;
import xyz.nucleoid.plasmid.api.game.event.GameActivityEvents;
import xyz.nucleoid.plasmid.api.game.event.GamePlayerEvents;
import xyz.nucleoid.plasmid.api.game.player.JoinAcceptor;
import xyz.nucleoid.plasmid.api.game.player.JoinAcceptorResult;
import xyz.nucleoid.plasmid.api.game.player.JoinOffer;
import xyz.nucleoid.plasmid.api.game.rule.GameRuleType;
import xyz.nucleoid.stimuli.event.EventResult;
import xyz.nucleoid.stimuli.event.player.PlayerDeathEvent;

public class VolleyballWaitingPhase implements GamePlayerEvents.Add, PlayerDeathEvent, GamePlayerEvents.Accept, GameActivityEvents.RequestStart {
	private final GameSpace gameSpace;
	private final ServerLevel level;
	private final VolleyballMap map;
	private final TeamSelectionLobby teamSelection;
	private final VolleyballConfig config;
	private final Component shortName;

	public VolleyballWaitingPhase(GameSpace gameSpace, ServerLevel level, VolleyballMap map, TeamSelectionLobby teamSelection, VolleyballConfig config, Component shortName) {
		this.gameSpace = gameSpace;
		this.level = level;
		this.map = map;
		this.teamSelection = teamSelection;
		this.config = config;
		this.shortName = shortName;
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
		activity.deny(GameRuleType.PVP);
		activity.deny(GameRuleType.THROW_ITEMS);
	}

	public static GameOpenProcedure open(GameOpenContext<VolleyballConfig> context) {
		VolleyballConfig config = context.game().config();
		MinecraftServer server = context.server();

		VolleyballMapBuilder mapBuilder = new VolleyballMapBuilder(config);
		VolleyballMap map = mapBuilder.create(server);

		RuntimeLevelConfig levelConfig = new RuntimeLevelConfig()
			.setGenerator(map.createGenerator(server));

		return context.openWithLevel(levelConfig, (activity, level) -> {
			TeamSelectionLobby teamSelection = TeamSelectionLobby.addTo(activity, config.getTeams());
			Component shortName = GameConfig.shortName(Holder.direct(context.game()));

			VolleyballWaitingPhase phase = new VolleyballWaitingPhase(activity.getGameSpace(), level, map, teamSelection, config, shortName);
			GameWaitingLobby.addTo(activity, config.getPlayerConfig());

			VolleyballWaitingPhase.setRules(activity);

			// Listeners
			activity.listen(GamePlayerEvents.ADD, phase);
			activity.listen(PlayerDeathEvent.EVENT, phase);
			activity.listen(GamePlayerEvents.ACCEPT, phase);
			activity.listen(GamePlayerEvents.OFFER, JoinOffer::accept);
			activity.listen(GameActivityEvents.REQUEST_START, phase);
		});
	}

	// Listeners
	@Override
	public void onAddPlayer(ServerPlayer player) {
		this.map.spawnAtWaiting(this.level, player);
	}

	@Override
	public EventResult onDeath(ServerPlayer player, DamageSource source) {
		this.map.spawnAtWaiting(this.level, player);
		return EventResult.DENY;
	}

	@Override
	public JoinAcceptorResult onAcceptPlayers(JoinAcceptor acceptor) {
		return this.map.acceptJoins(acceptor, this.level, GameType.ADVENTURE);
	}

	@Override
	public GameResult onRequestStart() {
		VolleyballActivePhase.open(this.gameSpace, this.level, this.map, this.teamSelection, this.config, this.shortName);
		return GameResult.ok();
	}
}