package io.github.haykam821.volleyball.game.map;

import java.io.IOException;

import io.github.haykam821.volleyball.game.VolleyballConfig;
import net.minecraft.server.MinecraftServer;
import net.minecraft.text.Text;
import xyz.nucleoid.map_templates.MapTemplate;
import xyz.nucleoid.map_templates.MapTemplateSerializer;
import xyz.nucleoid.plasmid.game.GameOpenException;

public class VolleyballMapBuilder {
	private final VolleyballConfig config;

	public VolleyballMapBuilder(VolleyballConfig config) {
		this.config = config;
	}

	public VolleyballMap create(MinecraftServer server) {
		try {
			MapTemplate template = MapTemplateSerializer.loadFromResource(server, this.config.getMap());
			return new VolleyballMap(template);
		} catch (IOException exception) {
			throw new GameOpenException(Text.translatable("text.volleyball.template_load_failed"), exception);
		}
	}
}