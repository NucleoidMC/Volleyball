package io.github.haykam821.volleyball.game.phase;

import io.github.haykam821.volleyball.game.VolleyballConfig;
import io.github.haykam821.volleyball.game.map.VolleyballMap;
import io.github.haykam821.volleyball.game.map.VolleyballMapBuilder;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.world.GameMode;
import xyz.nucleoid.fantasy.RuntimeWorldConfig;
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
	private final ServerWorld world;
	private final VolleyballMap map;
	private final TeamSelectionLobby teamSelection;
	private final VolleyballConfig config;
	private final Text shortName;

	public VolleyballWaitingPhase(GameSpace gameSpace, ServerWorld world, VolleyballMap map, TeamSelectionLobby teamSelection, VolleyballConfig config, Text shortName) {
		this.gameSpace = gameSpace;
		this.world = world;
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

		RuntimeWorldConfig worldConfig = new RuntimeWorldConfig()
			.setGenerator(map.createGenerator(server));

		return context.openWithWorld(worldConfig, (activity, world) -> {
			TeamSelectionLobby teamSelection = TeamSelectionLobby.addTo(activity, config.getTeams());
			Text shortName = GameConfig.shortName(RegistryEntry.of(context.game()));

			VolleyballWaitingPhase phase = new VolleyballWaitingPhase(activity.getGameSpace(), world, map, teamSelection, config, shortName);
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
	public void onAddPlayer(ServerPlayerEntity player) {
		this.map.spawnAtWaiting(this.world, player);
	}

	@Override
	public EventResult onDeath(ServerPlayerEntity player, DamageSource source) {
		this.map.spawnAtWaiting(this.world, player);
		return EventResult.DENY;
	}

	@Override
	public JoinAcceptorResult onAcceptPlayers(JoinAcceptor acceptor) {
		return this.map.acceptJoins(acceptor, this.world, GameMode.ADVENTURE);
	}

	@Override
	public GameResult onRequestStart() {
		VolleyballActivePhase.open(this.gameSpace, this.world, this.map, this.teamSelection, this.config, this.shortName);
		return GameResult.ok();
	}
}