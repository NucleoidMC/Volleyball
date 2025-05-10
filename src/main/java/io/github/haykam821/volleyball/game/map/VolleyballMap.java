package io.github.haykam821.volleyball.game.map;

import java.util.Set;

import net.minecraft.entity.Entity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import xyz.nucleoid.map_templates.MapTemplate;
import xyz.nucleoid.map_templates.TemplateRegion;
import xyz.nucleoid.plasmid.api.game.player.JoinAcceptor;
import xyz.nucleoid.plasmid.api.game.player.JoinAcceptorResult;
import xyz.nucleoid.plasmid.api.game.world.generator.TemplateChunkGenerator;

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

	public Vec3d getWaitingSpawnPos() {
		return this.waitingSpawn.getBounds().centerBottom();
	}

	public float getWaitingSpawnYaw() {
		return this.waitingSpawn.getData().getFloat(FACING_KEY);
	}

	public void spawnAtWaiting(ServerWorld world, Entity entity) {
		this.spawn(world, entity, this.getWaitingSpawnPos());
	}

	public JoinAcceptorResult acceptJoins(JoinAcceptor acceptor, ServerWorld world, GameMode gameMode) {
		return acceptor.teleport(world, this.getWaitingSpawnPos()).thenRunForEach(player -> {
			player.setYaw(this.getWaitingSpawnYaw());
			player.changeGameMode(gameMode);
		});
	}

	public Box getBallSpawnBox() {
		return this.ballSpawn.getBounds().asBox();
	}

	public void spawnAtBall(ServerWorld world, Entity entity) {
		this.spawn(world, entity, this.ballSpawn.getBounds().center());
	}

	private void spawn(ServerWorld world, Entity entity, Vec3d pos) {
		float yaw = this.getWaitingSpawnYaw();
		entity.teleport(world, pos.getX(), pos.getY(), pos.getZ(), Set.of(), yaw, 0, true);
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