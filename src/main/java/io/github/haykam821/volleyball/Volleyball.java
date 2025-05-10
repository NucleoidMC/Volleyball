package io.github.haykam821.volleyball;

import io.github.haykam821.volleyball.entity.VolleyballEntityTypes;
import io.github.haykam821.volleyball.game.VolleyballConfig;
import io.github.haykam821.volleyball.game.phase.VolleyballWaitingPhase;
import net.fabricmc.api.ModInitializer;
import net.minecraft.util.Identifier;
import xyz.nucleoid.plasmid.api.game.GameType;

public class Volleyball implements ModInitializer {
	private static final String MOD_ID = "volleyball";

	private static final Identifier VOLLEYBALL_ID = Volleyball.identifier("volleyball");
	public static final GameType<VolleyballConfig> VOLLEYBALL_TYPE = GameType.register(VOLLEYBALL_ID, VolleyballConfig.CODEC, VolleyballWaitingPhase::open);

	@Override
	public void onInitialize() {
		VolleyballEntityTypes.register();
	}

	public static Identifier identifier(String path) {
		return Identifier.of(MOD_ID, path);
	}
}
