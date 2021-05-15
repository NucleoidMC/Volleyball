package io.github.haykam821.volleyball.game.map;

import java.io.IOException;

import io.github.haykam821.volleyball.game.VolleyballConfig;
import net.minecraft.text.TranslatableText;
import xyz.nucleoid.plasmid.game.GameOpenException;
import xyz.nucleoid.plasmid.map.template.MapTemplate;
import xyz.nucleoid.plasmid.map.template.MapTemplateSerializer;

public class VolleyballMapBuilder {
	private final VolleyballConfig config;

	public VolleyballMapBuilder(VolleyballConfig config) {
		this.config = config;
	}

	public VolleyballMap create() {
		try {
			MapTemplate template = MapTemplateSerializer.INSTANCE.loadFromResource(this.config.getMap());
			return new VolleyballMap(template);
		} catch (IOException exception) {
			throw new GameOpenException(new TranslatableText("text.volleyball.template_load_failed"), exception);
		}
	}
}