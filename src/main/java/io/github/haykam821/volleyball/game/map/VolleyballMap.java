package io.github.haykam821.volleyball.game.map;

import java.util.Set;

import net.minecraft.world.entity.Entity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.chunk.ChunkGenerator;
import xyz.nucleoid.map_templates.MapTemplate;
import xyz.nucleoid.map_templates.TemplateRegion;
import xyz.nucleoid.plasmid.api.game.player.JoinAcceptor;
import xyz.nucleoid.plasmid.api.game.player.JoinAcceptorResult;
import xyz.nucleoid.plasmid.api.game.level.generator.TemplateChunkGenerator;

public class VolleyballMap {
	public static final String FACING_KEY = "Facing";

	private final MapTemplate template;
	private final TemplateRegion waitingSpawn;
	private final TemplateRegion ballSpawn;

	public VolleyballMap(MapTemplate template) {
		this.template = template;
		this.waitingSpawn = this.getSpawnRegion("waiting", false);
		this.ballSpawn = this.getSpawnRegion("ball");
	}

	public MapTemplate getTemplate() {
		return this.template;
	}

	public Vec3 getWaitingSpawnPos() {
		return this.waitingSpawn.getBounds().centerBottom();
	}

	public float getWaitingSpawnYaw() {
		return this.waitingSpawn.getData().getFloatOr(FACING_KEY, 0);
	}

	public void spawnAtWaiting(ServerLevel world, Entity entity) {
		this.spawn(world, entity, this.getWaitingSpawnPos());
	}

	public JoinAcceptorResult acceptJoins(JoinAcceptor acceptor, ServerLevel world, GameType gameMode) {
		return acceptor.teleport(world, this.getWaitingSpawnPos()).thenRunForEach(player -> {
			player.setYRot(this.getWaitingSpawnYaw());
			player.setGameMode(gameMode);
		});
	}

	public AABB getBallSpawnBox() {
		return this.ballSpawn.getBounds().asBox();
	}

	public void spawnAtBall(ServerLevel level, Entity entity) {
		this.spawn(level, entity, this.ballSpawn.getBounds().center());
	}

	private void spawn(ServerLevel level, Entity entity, Vec3 pos) {
		float yaw = this.getWaitingSpawnYaw();
		entity.teleportTo(level, pos.x(), pos.y(), pos.z(), Set.of(), yaw, 0, true);
	}

	private TemplateRegion getSpawnRegion(String type) {
		return this.getSpawnRegion(type, true);
	}

	private TemplateRegion getSpawnRegion(String type, boolean fallback) {
		TemplateRegion spawnRegion = this.template.getMetadata().getFirstRegion(type + "_spawn");
		if (spawnRegion == null) {
			return fallback ? this.getSpawnRegion("waiting", false) : null;
		}

		return spawnRegion;
	}

	public ChunkGenerator createGenerator(MinecraftServer server) {
		return new TemplateChunkGenerator(server, this.template);
	}
}