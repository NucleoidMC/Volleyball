package io.github.haykam821.volleyball;

import io.github.haykam821.volleyball.game.VolleyballConfig;
import io.github.haykam821.volleyball.game.phase.VolleyballWaitingPhase;
import net.fabricmc.api.ModInitializer;
import net.minecraft.util.Identifier;
import xyz.nucleoid.plasmid.game.GameType;

public class Volleyball implements ModInitializer {
	public static final String MOD_ID = "volleyball";

	private static final Identifier VOLLEYBALL_ID = new Identifier(MOD_ID, "volleyball");
	public static final GameType<VolleyballConfig> VOLLEYBALL_TYPE = GameType.register(VOLLEYBALL_ID, VolleyballWaitingPhase::open, VolleyballConfig.CODEC);

	@Override
	public void onInitialize() {
		return;
	}
}
